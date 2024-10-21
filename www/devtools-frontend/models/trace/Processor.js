import * as Handlers from './handlers/handlers.js';
export class TraceParseProgressEvent extends Event {
    data;
    static eventName = 'traceparseprogress';
    constructor(data, init = { bubbles: true }) {
        super(TraceParseProgressEvent.eventName, init);
        this.data = data;
    }
}
export class TraceProcessor extends EventTarget {
    // We force the Meta handler to be enabled, so the TraceHandlers type here is
    // the model handlers the user passes in and the Meta handler.
    // eslint-disable-next-line @typescript-eslint/naming-convention
    #traceHandlers;
    #pauseDuration;
    #eventsPerChunk;
    #status = "IDLE" /* Status.IDLE */;
    static createWithAllHandlers() {
        return new TraceProcessor(Handlers.ModelHandlers);
    }
    constructor(traceHandlers, { pauseDuration = 1, eventsPerChunk = 15_000 } = {}) {
        super();
        this.#verifyHandlers(traceHandlers);
        this.#traceHandlers = {
            Meta: Handlers.ModelHandlers.Meta,
            ...traceHandlers,
        };
        this.#pauseDuration = pauseDuration;
        this.#eventsPerChunk = eventsPerChunk;
    }
    /**
     * When the user passes in a set of handlers, we want to ensure that we have all
     * the required handlers. Handlers can depend on other handlers, so if the user
     * passes in FooHandler which depends on BarHandler, they must also pass in
     * BarHandler too. This method verifies that all dependencies are met, and
     * throws if not.
     **/
    #verifyHandlers(providedHandlers) {
        // Tiny optimisation: if the amount of provided handlers matches the amount
        // of handlers in the Handlers.ModelHandlers object, that means that the
        // user has passed in every handler we have. So therefore they cannot have
        // missed any, and there is no need to iterate through the handlers and
        // check the dependencies.
        if (Object.keys(providedHandlers).length === Object.keys(Handlers.ModelHandlers).length) {
            return;
        }
        const requiredHandlerKeys = new Set();
        for (const [handlerName, handler] of Object.entries(providedHandlers)) {
            requiredHandlerKeys.add(handlerName);
            for (const depName of (handler.deps?.() || [])) {
                requiredHandlerKeys.add(depName);
            }
        }
        const providedHandlerKeys = new Set(Object.keys(providedHandlers));
        // We always force the Meta handler to be enabled when creating the
        // Processor, so if it is missing from the set the user gave us that is OK,
        // as we will have enabled it anyway.
        requiredHandlerKeys.delete('Meta');
        for (const requiredKey of requiredHandlerKeys) {
            if (!providedHandlerKeys.has(requiredKey)) {
                throw new Error(`Required handler ${requiredKey} not provided.`);
            }
        }
    }
    reset() {
        if (this.#status === "PARSING" /* Status.PARSING */) {
            throw new Error('Trace processor can\'t reset while parsing.');
        }
        const handlers = Object.values(this.#traceHandlers);
        for (const handler of handlers) {
            handler.reset();
        }
        this.#status = "IDLE" /* Status.IDLE */;
    }
    async parse(traceEvents, freshRecording = false) {
        if (this.#status !== "IDLE" /* Status.IDLE */) {
            throw new Error(`Trace processor can't start parsing when not idle. Current state: ${this.#status}`);
        }
        try {
            this.#status = "PARSING" /* Status.PARSING */;
            await this.#parse(traceEvents, freshRecording);
            this.#status = "FINISHED_PARSING" /* Status.FINISHED_PARSING */;
        }
        catch (e) {
            this.#status = "ERRORED_WHILE_PARSING" /* Status.ERRORED_WHILE_PARSING */;
            throw e;
        }
    }
    async #parse(traceEvents, freshRecording) {
        // This iterator steps through all events, periodically yielding back to the
        // main thread to avoid blocking execution. It uses `dispatchEvent` to
        // provide status update events, and other various bits of config like the
        // pause duration and frequency.
        const traceEventIterator = new TraceEventIterator(traceEvents, this.#pauseDuration, this.#eventsPerChunk);
        // Convert to array so that we are able to iterate all handlers multiple times.
        const sortedHandlers = [...sortHandlers(this.#traceHandlers).values()];
        // Reset.
        for (const handler of sortedHandlers) {
            handler.reset();
        }
        // Initialize.
        for (const handler of sortedHandlers) {
            handler.initialize?.(freshRecording);
        }
        // Handle each event.
        for await (const item of traceEventIterator) {
            if (item.kind === 2 /* IteratorItemType.STATUS_UPDATE */) {
                this.dispatchEvent(new TraceParseProgressEvent(item.data));
                continue;
            }
            for (const handler of sortedHandlers) {
                handler.handleEvent(item.data);
            }
        }
        // Finalize.
        for (const handler of sortedHandlers) {
            await handler.finalize?.();
        }
    }
    get data() {
        if (this.#status !== "FINISHED_PARSING" /* Status.FINISHED_PARSING */) {
            return null;
        }
        const data = {};
        for (const [name, handler] of Object.entries(this.#traceHandlers)) {
            Object.assign(data, { [name]: handler.data() });
        }
        return data;
    }
}
/**
 * Some Handlers need data provided by others. Dependencies of a handler handler are
 * declared in the `deps` field.
 * @returns A map from trace event handler name to trace event hander whose entries
 * iterate in such a way that each handler is visited after its dependencies.
 */
export function sortHandlers(traceHandlers) {
    const sortedMap = new Map();
    const visited = new Set();
    const visitHandler = (handlerName) => {
        if (sortedMap.has(handlerName)) {
            return;
        }
        if (visited.has(handlerName)) {
            let stackPath = '';
            for (const handler of visited) {
                if (stackPath || handler === handlerName) {
                    stackPath += `${handler}->`;
                }
            }
            stackPath += handlerName;
            throw new Error(`Found dependency cycle in trace event handlers: ${stackPath}`);
        }
        visited.add(handlerName);
        const handler = traceHandlers[handlerName];
        if (!handler) {
            return;
        }
        const deps = handler.deps?.();
        if (deps) {
            deps.forEach(visitHandler);
        }
        sortedMap.set(handlerName, handler);
    };
    for (const handlerName of Object.keys(traceHandlers)) {
        visitHandler(handlerName);
    }
    return sortedMap;
}
class TraceEventIterator {
    traceEvents;
    pauseDuration;
    eventsPerChunk;
    #eventCount;
    constructor(traceEvents, pauseDuration, eventsPerChunk) {
        this.traceEvents = traceEvents;
        this.pauseDuration = pauseDuration;
        this.eventsPerChunk = eventsPerChunk;
        this.#eventCount = 0;
    }
    async *[Symbol.asyncIterator]() {
        for (let i = 0, length = this.traceEvents.length; i < length; i++) {
            // Every so often we take a break just to render.
            if (++this.#eventCount % this.eventsPerChunk === 0) {
                // Take the opportunity to provide status update events.
                yield { kind: 2 /* IteratorItemType.STATUS_UPDATE */, data: { index: i, total: length } };
                // Wait for rendering before resuming.
                await new Promise(resolve => setTimeout(resolve, this.pauseDuration));
            }
            yield { kind: 1 /* IteratorItemType.TRACE_EVENT */, data: this.traceEvents[i] };
        }
    }
}
//# sourceMappingURL=Processor.js.map