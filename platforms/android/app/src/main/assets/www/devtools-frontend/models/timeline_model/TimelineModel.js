/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration)
/* eslint-disable @typescript-eslint/no-explicit-any */
import * as Common from '../../core/common/common.js';
import * as i18n from '../../core/i18n/i18n.js';
import * as Platform from '../../core/platform/platform.js';
import * as Root from '../../core/root/root.js';
import * as SDK from '../../core/sdk/sdk.js';
import * as TraceEngine from '../trace/trace.js';
import { TimelineJSProfileProcessor } from './TimelineJSProfile.js';
const UIStrings = {
    /**
     *@description Text for the name of a thread of the page
     *@example {1} PH1
     */
    threadS: 'Thread {PH1}',
    /**
     *@description Title of a worker in the timeline flame chart of the Performance panel
     *@example {https://google.com} PH1
     */
    workerS: '`Worker` — {PH1}',
    /**
     *@description Title of a worker in the timeline flame chart of the Performance panel
     */
    dedicatedWorker: 'Dedicated `Worker`',
    /**
     *@description Title of a worker in the timeline flame chart of the Performance panel
     *@example {FormatterWorker} PH1
     *@example {https://google.com} PH2
     */
    workerSS: '`Worker`: {PH1} — {PH2}',
    /**
     *@description Title of a bidder auction worklet with known URL in the timeline flame chart of the Performance panel
     *@example {https://google.com} PH1
     */
    bidderWorkletS: 'Bidder Worklet — {PH1}',
    /**
     *@description Title of a seller auction worklet with known URL in the timeline flame chart of the Performance panel
     *@example {https://google.com} PH1
     */
    sellerWorkletS: 'Seller Worklet — {PH1}',
    /**
     *@description Title of an auction worklet with known URL in the timeline flame chart of the Performance panel
     *@example {https://google.com} PH1
     */
    unknownWorkletS: 'Auction Worklet — {PH1}',
    /**
     *@description Title of a bidder auction worklet in the timeline flame chart of the Performance panel
     */
    bidderWorklet: 'Bidder Worklet',
    /**
     *@description Title of a seller auction worklet in the timeline flame chart of the Performance panel
     */
    sellerWorklet: 'Seller Worklet',
    /**
     *@description Title of an auction worklet in the timeline flame chart of the Performance panel
     */
    unknownWorklet: 'Auction Worklet',
    /**
     *@description Title of control thread of a service process for an auction worklet in the timeline flame chart of the Performance panel
     */
    workletService: 'Auction Worklet Service',
    /**
     *@description Title of control thread of a service process for an auction worklet with known URL in the timeline flame chart of the Performance panel
     * @example {https://google.com} PH1
     */
    workletServiceS: 'Auction Worklet Service — {PH1}',
};
const str_ = i18n.i18n.registerUIStrings('models/timeline_model/TimelineModel.ts', UIStrings);
const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
export class TimelineModelImpl {
    isGenericTraceInternal;
    tracksInternal;
    namedTracks;
    inspectedTargetEventsInternal;
    timeMarkerEventsInternal;
    sessionId;
    mainFrameNodeId;
    pageFrames;
    auctionWorklets;
    cpuProfilesInternal;
    workerIdByThread;
    requestsFromBrowser;
    mainFrame;
    minimumRecordTimeInternal;
    maximumRecordTimeInternal;
    totalBlockingTimeInternal;
    estimatedTotalBlockingTime;
    asyncEventTracker;
    invalidationTracker;
    layoutInvalidate;
    lastScheduleStyleRecalculation;
    paintImageEventByPixelRefId;
    lastPaintForLayer;
    lastRecalculateStylesEvent;
    currentScriptEvent;
    eventStack;
    browserFrameTracking;
    persistentIds;
    legacyCurrentPage;
    currentTaskLayoutAndRecalcEvents;
    tracingModelInternal;
    mainFrameLayerTreeId;
    #isFreshRecording = false;
    constructor() {
        this.minimumRecordTimeInternal = 0;
        this.maximumRecordTimeInternal = 0;
        this.totalBlockingTimeInternal = 0;
        this.estimatedTotalBlockingTime = 0;
        this.reset();
        this.resetProcessingState();
        this.currentTaskLayoutAndRecalcEvents = [];
        this.tracingModelInternal = null;
    }
    /**
     * Iterates events in a tree hierarchically, from top to bottom,
     * calling back on every event's start and end in the order
     * dictated by the corresponding timestamp.
     *
     * Events are assumed to be in ascendent order by timestamp.
     *
     * For example, given this tree, the following callbacks
     * are expected to be made in the following order
     * |---------------A---------------|
     *  |------B------||-------D------|
     *    |---C---|
     *
     * 1. Start A
     * 3. Start B
     * 4. Start C
     * 5. End C
     * 6. End B
     * 7. Start D
     * 8. End D
     * 9. End A
     *
     * By default, async events are filtered. This behaviour can be
     * overriden making use of the filterAsyncEvents parameter.
     */
    static forEachEvent(events, onStartEvent, onEndEvent, onInstantEvent, startTime, endTime, filter, ignoreAsyncEvents = true) {
        startTime = startTime || 0;
        endTime = endTime || Infinity;
        const stack = [];
        const startEvent = TimelineModelImpl.topLevelEventEndingAfter(events, startTime);
        for (let i = startEvent; i < events.length; ++i) {
            const e = events[i];
            const { endTime: eventEndTime, startTime: eventStartTime, duration: eventDuration } = SDK.TracingModel.timesForEventInMilliseconds(e);
            const eventPhase = SDK.TracingModel.phaseForEvent(e);
            if ((eventEndTime || eventStartTime) < startTime) {
                continue;
            }
            if (eventStartTime >= endTime) {
                break;
            }
            const canIgnoreAsyncEvent = ignoreAsyncEvents && TraceEngine.Types.TraceEvents.isAsyncPhase(eventPhase);
            if (canIgnoreAsyncEvent || TraceEngine.Types.TraceEvents.isFlowPhase(eventPhase)) {
                continue;
            }
            let last = stack[stack.length - 1];
            let lastEventEndTime = last && SDK.TracingModel.timesForEventInMilliseconds(last).endTime;
            while (last && lastEventEndTime !== undefined && lastEventEndTime <= eventStartTime) {
                stack.pop();
                onEndEvent(last);
                last = stack[stack.length - 1];
                lastEventEndTime = last && SDK.TracingModel.timesForEventInMilliseconds(last).endTime;
            }
            if (filter && !filter(e)) {
                continue;
            }
            if (eventDuration) {
                onStartEvent(e);
                stack.push(e);
            }
            else {
                onInstantEvent && onInstantEvent(e, stack[stack.length - 1] || null);
            }
        }
        while (stack.length) {
            const last = stack.pop();
            if (last) {
                onEndEvent(last);
            }
        }
    }
    static topLevelEventEndingAfter(events, time) {
        let index = Platform.ArrayUtilities.upperBound(events, time, (time, event) => time - SDK.TracingModel.timesForEventInMilliseconds(event).startTime) -
            1;
        while (index > 0 && !SDK.TracingModel.TracingModel.isTopLevelEvent(events[index])) {
            index--;
        }
        return Math.max(index, 0);
    }
    mainFrameID() {
        return this.mainFrame.frameId;
    }
    /**
     * Determines if an event is potentially a marker event. A marker event here
     * is a single moment in time that we want to highlight on the timeline, such as
     * the LCP point. This method does not filter out events: for example, it treats
     * every LCP Candidate event as a potential marker event. The logic to pick the
     * right candidate to use is implemeneted in the TimelineFlameChartDataProvider.
     **/
    isMarkerEvent(event) {
        switch (event.name) {
            case RecordType.TimeStamp:
                return true;
            case RecordType.MarkFirstPaint:
            case RecordType.MarkFCP:
                return Boolean(this.mainFrame) && event.args.frame === this.mainFrame.frameId && Boolean(event.args.data);
            case RecordType.MarkDOMContent:
            case RecordType.MarkLoad:
            case RecordType.MarkLCPCandidate:
            case RecordType.MarkLCPInvalidate:
                return Boolean(event.args['data']['isOutermostMainFrame'] ?? event.args['data']['isMainFrame']);
            default:
                return false;
        }
    }
    isInteractiveTimeEvent(event) {
        return event.name === RecordType.InteractiveTime;
    }
    isLayoutShiftEvent(event) {
        return event.name === RecordType.LayoutShift;
    }
    isParseHTMLEvent(event) {
        return event.name === RecordType.ParseHTML;
    }
    static isJsFrameEvent(event) {
        return event.name === RecordType.JSFrame || event.name === RecordType.JSIdleFrame ||
            event.name === RecordType.JSSystemFrame;
    }
    static globalEventId(event, field) {
        const data = event.args['data'] || event.args['beginData'];
        const id = data && data[field];
        if (!id) {
            return '';
        }
        return `${event.thread.process().id()}.${id}`;
    }
    static eventFrameId(event) {
        const data = event.args['data'] || event.args['beginData'];
        return data && data['frame'] || null;
    }
    cpuProfiles() {
        return this.cpuProfilesInternal;
    }
    totalBlockingTime() {
        if (this.totalBlockingTimeInternal === -1) {
            return { time: this.estimatedTotalBlockingTime, estimated: true };
        }
        return { time: this.totalBlockingTimeInternal, estimated: false };
    }
    targetByEvent(event) {
        let thread;
        if (event instanceof SDK.TracingModel.Event) {
            thread = event.thread;
        }
        else {
            const process = this.tracingModelInternal?.getProcessById(event.pid);
            thread = process?.threadById(event.tid);
        }
        if (!thread) {
            return null;
        }
        // FIXME: Consider returning null for loaded traces.
        const workerId = this.workerIdByThread.get(thread);
        const primaryPageTarget = SDK.TargetManager.TargetManager.instance().primaryPageTarget();
        return workerId ? SDK.TargetManager.TargetManager.instance().targetById(workerId) : primaryPageTarget;
    }
    navStartTimes() {
        if (!this.tracingModelInternal) {
            return new Map();
        }
        return this.tracingModelInternal.navStartTimes();
    }
    isFreshRecording() {
        return this.#isFreshRecording;
    }
    setEvents(tracingModel, isFreshRecording = false) {
        this.#isFreshRecording = isFreshRecording;
        this.reset();
        this.resetProcessingState();
        this.tracingModelInternal = tracingModel;
        this.minimumRecordTimeInternal = tracingModel.minimumRecordTime();
        this.maximumRecordTimeInternal = tracingModel.maximumRecordTime();
        // Remove LayoutShift events from the main thread list of events because they are
        // represented in the experience track. This is done prior to the main thread being processed for its own events.
        const layoutShiftEvents = [];
        for (const process of tracingModel.sortedProcesses()) {
            if (process.name() !== 'Renderer') {
                continue;
            }
            for (const thread of process.sortedThreads()) {
                const shifts = thread.removeEventsByName(RecordType.LayoutShift);
                layoutShiftEvents.push(...shifts);
            }
        }
        this.processSyncBrowserEvents(tracingModel);
        if (this.browserFrameTracking) {
            this.processThreadsForBrowserFrames(tracingModel);
        }
        else {
            // The next line is for loading legacy traces recorded before M67.
            // TODO(alph): Drop the support at some point.
            const metadataEvents = this.processMetadataEvents(tracingModel);
            this.isGenericTraceInternal = !metadataEvents;
            if (metadataEvents) {
                this.processMetadataAndThreads(tracingModel, metadataEvents);
            }
            else {
                this.processGenericTrace(tracingModel);
            }
        }
        this.inspectedTargetEventsInternal.sort(SDK.TracingModel.Event.compareStartTime);
        this.processAsyncBrowserEvents(tracingModel);
        this.resetProcessingState();
    }
    processGenericTrace(tracingModel) {
        let browserMainThread = SDK.TracingModel.TracingModel.browserMainThread(tracingModel);
        if (!browserMainThread && tracingModel.sortedProcesses().length) {
            browserMainThread = tracingModel.sortedProcesses()[0].sortedThreads()[0];
        }
        for (const process of tracingModel.sortedProcesses()) {
            for (const thread of process.sortedThreads()) {
                this.processThreadEvents(tracingModel, thread, thread === browserMainThread, false, true, 0 /* WorkletType.NotWorklet */, null);
            }
        }
    }
    processMetadataAndThreads(tracingModel, metadataEvents) {
        let startTime = 0;
        for (let i = 0, length = metadataEvents.page.length; i < length; i++) {
            const metaEvent = metadataEvents.page[i];
            const process = metaEvent.thread.process();
            const endTime = i + 1 < length ? metadataEvents.page[i + 1].startTime : Infinity;
            if (startTime === endTime) {
                continue;
            }
            this.legacyCurrentPage = metaEvent.args['data'] && metaEvent.args['data']['page'];
            for (const thread of process.sortedThreads()) {
                let workerUrl = null;
                if (thread.name() === TimelineModelImpl.WorkerThreadName ||
                    thread.name() === TimelineModelImpl.WorkerThreadNameLegacy) {
                    const workerMetaEvent = metadataEvents.workers.find(e => {
                        if (e.args['data']['workerThreadId'] !== thread.id()) {
                            return false;
                        }
                        // This is to support old traces.
                        if (e.args['data']['sessionId'] === this.sessionId) {
                            return true;
                        }
                        const frameId = TimelineModelImpl.eventFrameId(e);
                        return frameId ? Boolean(this.pageFrames.get(frameId)) : false;
                    });
                    if (!workerMetaEvent) {
                        continue;
                    }
                    const workerId = workerMetaEvent.args['data']['workerId'];
                    if (workerId) {
                        this.workerIdByThread.set(thread, workerId);
                    }
                    workerUrl = workerMetaEvent.args['data']['url'] || Platform.DevToolsPath.EmptyUrlString;
                }
                this.processThreadEvents(tracingModel, thread, thread === metaEvent.thread, Boolean(workerUrl), true, 0 /* WorkletType.NotWorklet */, workerUrl);
            }
            startTime = endTime;
        }
    }
    processThreadsForBrowserFrames(tracingModel) {
        const processDataByPid = new Map();
        for (const frame of this.pageFrames.values()) {
            for (let i = 0; i < frame.processes.length; i++) {
                const pid = frame.processes[i].processId;
                let data = processDataByPid.get(pid);
                if (!data) {
                    data = [];
                    processDataByPid.set(pid, data);
                }
                const to = i === frame.processes.length - 1 ? (frame.deletedTime || Infinity) : frame.processes[i + 1].time;
                data.push({
                    from: frame.processes[i].time,
                    to: to,
                    main: !frame.parent,
                    url: frame.processes[i].url,
                    workletType: 0 /* WorkletType.NotWorklet */,
                });
            }
        }
        for (const auctionWorklet of this.auctionWorklets.values()) {
            const pid = auctionWorklet.processId;
            let data = processDataByPid.get(pid);
            if (!data) {
                data = [];
                processDataByPid.set(pid, data);
            }
            data.push({
                from: auctionWorklet.startTime,
                to: auctionWorklet.endTime,
                main: false,
                workletType: auctionWorklet.workletType,
                url: (auctionWorklet.host ? 'https://' + auctionWorklet.host :
                    Platform.DevToolsPath.EmptyUrlString),
            });
        }
        const allMetadataEvents = tracingModel.devToolsMetadataEvents();
        for (const process of tracingModel.sortedProcesses()) {
            const processData = processDataByPid.get(process.id());
            if (!processData) {
                continue;
            }
            // Sort ascending by range starts, followed by range ends
            processData.sort((a, b) => a.from - b.from || a.to - b.to);
            let lastUrl = null;
            let lastMainUrl = null;
            let hasMain = false;
            let allWorklet = true;
            // false: not set, true: inconsistent.
            let workletUrl = false;
            // NotWorklet used for not set.
            let workletType = 0 /* WorkletType.NotWorklet */;
            for (const item of processData) {
                if (item.main) {
                    hasMain = true;
                }
                if (item.url) {
                    if (item.main) {
                        lastMainUrl = item.url;
                    }
                    lastUrl = item.url;
                }
                // Worklet identification
                if (item.workletType === 0 /* WorkletType.NotWorklet */) {
                    allWorklet = false;
                }
                else {
                    // Update combined workletUrl, checking for inconsistencies.
                    if (workletUrl === false) {
                        workletUrl = item.url;
                    }
                    else if (workletUrl !== item.url) {
                        workletUrl = true; // Process used for different things.
                    }
                    if (workletType === 0 /* WorkletType.NotWorklet */) {
                        workletType = item.workletType;
                    }
                    else if (workletType !== item.workletType) {
                        workletType = 3 /* WorkletType.UnknownWorklet */;
                    }
                }
            }
            for (const thread of process.sortedThreads()) {
                if (thread.name() === TimelineModelImpl.RendererMainThreadName) {
                    this.processThreadEvents(tracingModel, thread, true /* isMainThread */, false /* isWorker */, hasMain, 0 /* WorkletType.NotWorklet */, hasMain ? lastMainUrl : lastUrl);
                }
                else if (thread.name() === TimelineModelImpl.WorkerThreadName ||
                    thread.name() === TimelineModelImpl.WorkerThreadNameLegacy) {
                    const workerMetaEvent = allMetadataEvents.find(e => {
                        if (e.name !== TimelineModelImpl.DevToolsMetadataEvent.TracingSessionIdForWorker) {
                            return false;
                        }
                        if (e.thread.process() !== process) {
                            return false;
                        }
                        if (e.args['data']['workerThreadId'] !== thread.id()) {
                            return false;
                        }
                        const frameId = TimelineModelImpl.eventFrameId(e);
                        return frameId ? Boolean(this.pageFrames.get(frameId)) : false;
                    });
                    if (!workerMetaEvent) {
                        continue;
                    }
                    this.workerIdByThread.set(thread, workerMetaEvent.args['data']['workerId'] || '');
                    this.processThreadEvents(tracingModel, thread, false /* isMainThread */, true /* isWorker */, false /* forMainFrame */, 0 /* WorkletType.NotWorklet */, workerMetaEvent.args['data']['url'] || Platform.DevToolsPath.EmptyUrlString);
                }
                else {
                    let urlForOther = null;
                    let workletTypeForOther = 0 /* WorkletType.NotWorklet */;
                    if (thread.name() === TimelineModelImpl.AuctionWorkletThreadName ||
                        thread.name().endsWith(TimelineModelImpl.UtilityMainThreadNameSuffix)) {
                        if (typeof workletUrl !== 'boolean') {
                            urlForOther = workletUrl;
                        }
                        workletTypeForOther = workletType;
                    }
                    else {
                        // For processes that only do auction worklet things, skip other threads.
                        if (allWorklet) {
                            continue;
                        }
                    }
                    this.processThreadEvents(tracingModel, thread, false /* isMainThread */, false /* isWorker */, false /* forMainFrame */, workletTypeForOther, urlForOther);
                }
            }
        }
    }
    processMetadataEvents(tracingModel) {
        const metadataEvents = tracingModel.devToolsMetadataEvents();
        const pageDevToolsMetadataEvents = [];
        const workersDevToolsMetadataEvents = [];
        for (const event of metadataEvents) {
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.TracingStartedInPage) {
                pageDevToolsMetadataEvents.push(event);
                if (event.args['data'] && event.args['data']['persistentIds']) {
                    this.persistentIds = true;
                }
                const frames = ((event.args['data'] && event.args['data']['frames']) || []);
                frames.forEach((payload) => this.addPageFrame(event, payload));
                this.mainFrame = this.rootFrames()[0];
            }
            else if (event.name === TimelineModelImpl.DevToolsMetadataEvent.TracingSessionIdForWorker) {
                workersDevToolsMetadataEvents.push(event);
            }
            else if (event.name === TimelineModelImpl.DevToolsMetadataEvent.TracingStartedInBrowser) {
                console.assert(!this.mainFrameNodeId, 'Multiple sessions in trace');
                this.mainFrameNodeId = event.args['frameTreeNodeId'];
            }
        }
        if (!pageDevToolsMetadataEvents.length) {
            return null;
        }
        const sessionId = pageDevToolsMetadataEvents[0].args['sessionId'] || pageDevToolsMetadataEvents[0].args['data']['sessionId'];
        this.sessionId = sessionId;
        const mismatchingIds = new Set();
        function checkSessionId(event) {
            let args = event.args;
            // FIXME: put sessionId into args["data"] for TracingStartedInPage event.
            if (args['data']) {
                args = args['data'];
            }
            const id = args['sessionId'];
            if (id === sessionId) {
                return true;
            }
            mismatchingIds.add(id);
            return false;
        }
        const result = {
            page: pageDevToolsMetadataEvents.filter(checkSessionId).sort(SDK.TracingModel.Event.compareStartTime),
            workers: workersDevToolsMetadataEvents.sort(SDK.TracingModel.Event.compareStartTime),
        };
        if (mismatchingIds.size) {
            Common.Console.Console.instance().error('Timeline recording was started in more than one page simultaneously. Session id mismatch: ' +
                this.sessionId + ' and ' + [...mismatchingIds] + '.');
        }
        return result;
    }
    processSyncBrowserEvents(tracingModel) {
        const browserMain = SDK.TracingModel.TracingModel.browserMainThread(tracingModel);
        if (browserMain) {
            browserMain.events().forEach(this.processBrowserEvent, this);
        }
    }
    processAsyncBrowserEvents(tracingModel) {
        const browserMain = SDK.TracingModel.TracingModel.browserMainThread(tracingModel);
        if (browserMain) {
            this.processAsyncEvents(browserMain);
        }
    }
    resetProcessingState() {
        this.asyncEventTracker = new TimelineAsyncEventTracker();
        this.invalidationTracker = new InvalidationTracker();
        this.layoutInvalidate = {};
        this.lastScheduleStyleRecalculation = {};
        this.paintImageEventByPixelRefId = {};
        this.lastPaintForLayer = {};
        this.lastRecalculateStylesEvent = null;
        this.currentScriptEvent = null;
        this.eventStack = [];
        this.browserFrameTracking = false;
        this.persistentIds = false;
        this.legacyCurrentPage = null;
    }
    extractCpuProfileDataModel(tracingModel, thread) {
        const events = thread.events();
        let cpuProfile;
        let target = null;
        // Check for legacy CpuProfile event format first.
        // 'CpuProfile' is currently used by https://webpack.js.org/plugins/profiling-plugin/ and our createFakeTraceFromCpuProfile
        let cpuProfileEvent = events.at(-1);
        if (cpuProfileEvent && cpuProfileEvent.name === RecordType.CpuProfile) {
            const eventData = cpuProfileEvent.args['data'];
            cpuProfile = (eventData && eventData['cpuProfile']);
            target = this.targetByEvent(cpuProfileEvent);
        }
        if (!cpuProfile) {
            cpuProfileEvent = events.find(e => e.name === RecordType.Profile);
            if (!cpuProfileEvent) {
                return null;
            }
            target = this.targetByEvent(cpuProfileEvent);
            // Profile groups are created right after a trace is loaded (in
            // tracing model).
            // They are created using events with the "P" phase (samples),
            // which includes ProfileChunks with the samples themselves but
            // also "Profile" events with metadata of the profile.
            // A group is created for each unique profile in each unique
            // thread.
            const profileGroup = tracingModel.profileGroup(cpuProfileEvent);
            if (!profileGroup) {
                Common.Console.Console.instance().error('Invalid CPU profile format.');
                return null;
            }
            cpuProfile = {
                startTime: cpuProfileEvent.startTime * 1000,
                endTime: 0,
                nodes: [],
                samples: [],
                timeDeltas: [],
                lines: [],
            };
            for (const profileEvent of profileGroup.children) {
                const eventData = profileEvent.args['data'];
                if ('startTime' in eventData) {
                    // Do not use |eventData['startTime']| as it is in CLOCK_MONOTONIC domain,
                    // but use |profileEvent.startTime| (|ts| in the trace event) which has
                    // been translated to Perfetto's clock domain.
                    //
                    // Also convert from ms to us.
                    cpuProfile.startTime = profileEvent.startTime * 1000;
                }
                if ('endTime' in eventData) {
                    // Do not use |eventData['endTime']| as it is in CLOCK_MONOTONIC domain,
                    // but use |profileEvent.startTime| (|ts| in the trace event) which has
                    // been translated to Perfetto's clock domain.
                    //
                    // Despite its name, |profileEvent.startTime| was recorded right after
                    // |eventData['endTime']| within v8 and is a reasonable substitute.
                    //
                    // Also convert from ms to us.
                    cpuProfile.endTime = profileEvent.startTime * 1000;
                }
                const nodesAndSamples = eventData['cpuProfile'] || {};
                const samples = nodesAndSamples['samples'] || [];
                const lines = eventData['lines'] || Array(samples.length).fill(0);
                cpuProfile.nodes.push(...(nodesAndSamples['nodes'] || []));
                cpuProfile.lines.push(...lines);
                if (cpuProfile.samples) {
                    cpuProfile.samples.push(...samples);
                }
                if (cpuProfile.timeDeltas) {
                    cpuProfile.timeDeltas.push(...(eventData['timeDeltas'] || []));
                }
                if (cpuProfile.samples && cpuProfile.timeDeltas && cpuProfile.samples.length !== cpuProfile.timeDeltas.length) {
                    Common.Console.Console.instance().error('Failed to parse CPU profile.');
                    return null;
                }
            }
            if (!cpuProfile.endTime && cpuProfile.timeDeltas) {
                const timeDeltas = cpuProfile.timeDeltas;
                cpuProfile.endTime = timeDeltas.reduce((x, y) => x + y, cpuProfile.startTime);
            }
        }
        try {
            const profile = cpuProfile;
            const jsProfileModel = new SDK.CPUProfileDataModel.CPUProfileDataModel(profile, target);
            this.cpuProfilesInternal.push(jsProfileModel);
            return jsProfileModel;
        }
        catch (e) {
            Common.Console.Console.instance().error('Failed to parse CPU profile.');
        }
        return null;
    }
    injectJSFrameEvents(tracingModel, thread) {
        const jsProfileModel = this.extractCpuProfileDataModel(tracingModel, thread);
        let events = thread.events();
        const jsSamples = jsProfileModel ?
            TimelineJSProfileProcessor.generateConstructedEventsFromCpuProfileDataModel(jsProfileModel, thread) :
            null;
        if (jsSamples && jsSamples.length) {
            events = Platform.ArrayUtilities.mergeOrdered(events, jsSamples, SDK.TracingModel.Event.orderedCompareStartTime);
        }
        if (jsSamples ||
            events.some(e => e.name === RecordType.JSSample || e.name === RecordType.JSSystemSample ||
                e.name === RecordType.JSIdleSample)) {
            const jsFrameEvents = TimelineJSProfileProcessor.generateJSFrameEvents(events, {
                showAllEvents: Root.Runtime.experiments.isEnabled('timelineShowAllEvents'),
                showRuntimeCallStats: Root.Runtime.experiments.isEnabled('timelineV8RuntimeCallStats'),
                showNativeFunctions: Common.Settings.Settings.instance().moduleSetting('showNativeFunctionsInJSProfile').get(),
            });
            if (jsFrameEvents && jsFrameEvents.length) {
                events =
                    Platform.ArrayUtilities.mergeOrdered(jsFrameEvents, events, SDK.TracingModel.Event.orderedCompareStartTime);
            }
        }
        return events;
    }
    static nameAuctionWorklet(workletType, url) {
        switch (workletType) {
            case 1 /* WorkletType.BidderWorklet */:
                return url ? i18nString(UIStrings.bidderWorkletS, { PH1: url }) : i18nString(UIStrings.bidderWorklet);
            case 2 /* WorkletType.SellerWorklet */:
                return url ? i18nString(UIStrings.sellerWorkletS, { PH1: url }) : i18nString(UIStrings.sellerWorklet);
            default:
                return url ? i18nString(UIStrings.unknownWorkletS, { PH1: url }) : i18nString(UIStrings.unknownWorklet);
        }
    }
    processThreadEvents(tracingModel, thread, isMainThread, isWorker, forMainFrame, workletType, url) {
        const track = new Track();
        track.name = thread.name() || i18nString(UIStrings.threadS, { PH1: thread.id() });
        track.type = TrackType.Other;
        track.thread = thread;
        if (isMainThread) {
            track.type = TrackType.MainThread;
            track.url = url || Platform.DevToolsPath.EmptyUrlString;
            track.forMainFrame = forMainFrame;
        }
        else if (isWorker) {
            track.type = TrackType.Worker;
            track.url = url || Platform.DevToolsPath.EmptyUrlString;
            track.name = track.url ? i18nString(UIStrings.workerS, { PH1: track.url }) : i18nString(UIStrings.dedicatedWorker);
        }
        else if (thread.name().startsWith('CompositorTileWorker')) {
            track.type = TrackType.Raster;
        }
        else if (thread.name() === TimelineModelImpl.AuctionWorkletThreadName) {
            track.url = url || Platform.DevToolsPath.EmptyUrlString;
            track.name = TimelineModelImpl.nameAuctionWorklet(workletType, url);
        }
        else if (workletType !== 0 /* WorkletType.NotWorklet */ &&
            thread.name().endsWith(TimelineModelImpl.UtilityMainThreadNameSuffix)) {
            track.url = url || Platform.DevToolsPath.EmptyUrlString;
            track.name = url ? i18nString(UIStrings.workletServiceS, { PH1: url }) : i18nString(UIStrings.workletService);
        }
        this.tracksInternal.push(track);
        const events = this.injectJSFrameEvents(tracingModel, thread);
        this.eventStack = [];
        const eventStack = this.eventStack;
        // Get the worker name from the target.
        if (isWorker) {
            const cpuProfileEvent = events.find(event => event.name === RecordType.Profile);
            if (cpuProfileEvent) {
                const target = this.targetByEvent(cpuProfileEvent);
                if (target) {
                    track.name = i18nString(UIStrings.workerSS, { PH1: target.name(), PH2: track.url });
                }
            }
        }
        for (let i = 0; i < events.length; i++) {
            const event = events[i];
            // There may be several TTI events, only take the first one.
            if (this.isInteractiveTimeEvent(event) && this.totalBlockingTimeInternal === -1) {
                this.totalBlockingTimeInternal = event.args['args']['total_blocking_time_ms'];
            }
            const isLongRunningTask = event.name === RecordType.Task && event.duration && event.duration > 50;
            if (isMainThread && isLongRunningTask && event.duration) {
                // We only track main thread events that are over 50ms, and the amount of time in the
                // event (over 50ms) is what constitutes the blocking time. An event of 70ms, therefore,
                // contributes 20ms to TBT.
                this.estimatedTotalBlockingTime += event.duration - 50;
            }
            let last = eventStack[eventStack.length - 1];
            while (last && last.endTime !== undefined && last.endTime <= event.startTime) {
                eventStack.pop();
                last = eventStack[eventStack.length - 1];
            }
            if (!this.processEvent(event)) {
                continue;
            }
            if (!TraceEngine.Types.TraceEvents.isAsyncPhase(event.phase) && event.duration) {
                if (eventStack.length) {
                    const parent = eventStack[eventStack.length - 1];
                    if (parent) {
                        parent.selfTime -= event.duration;
                        if (parent.selfTime < 0) {
                            this.fixNegativeDuration(parent, event);
                        }
                    }
                }
                event.selfTime = event.duration;
                if (!eventStack.length) {
                    track.tasks.push(event);
                }
                eventStack.push(event);
            }
            if (this.isMarkerEvent(event)) {
                this.timeMarkerEventsInternal.push(event);
            }
            track.events.push(event);
            this.inspectedTargetEventsInternal.push(event);
        }
        this.processAsyncEvents(thread);
    }
    fixNegativeDuration(event, child) {
        const epsilon = 1e-3;
        if (event.selfTime < -epsilon) {
            console.error(`Children are longer than parent at ${event.startTime} ` +
                `(${(child.startTime - this.minimumRecordTime()).toFixed(3)} by ${(-event.selfTime).toFixed(3)}`);
        }
        event.selfTime = 0;
    }
    processAsyncEvents(thread) {
        const asyncEvents = thread.asyncEvents();
        const groups = new Map();
        function group(type) {
            if (!groups.has(type)) {
                groups.set(type, []);
            }
            return groups.get(type);
        }
        for (let i = 0; i < asyncEvents.length; ++i) {
            const asyncEvent = asyncEvents[i];
            if (asyncEvent.name === RecordType.Animation) {
                group(TrackType.Animation).push(asyncEvent);
                continue;
            }
        }
        for (const [type, events] of groups) {
            const track = this.ensureNamedTrack(type);
            track.thread = thread;
            track.asyncEvents =
                Platform.ArrayUtilities.mergeOrdered(track.asyncEvents, events, SDK.TracingModel.Event.compareStartTime);
        }
    }
    processEvent(event) {
        const eventStack = this.eventStack;
        if (!eventStack.length) {
            if (this.currentTaskLayoutAndRecalcEvents && this.currentTaskLayoutAndRecalcEvents.length) {
                const totalTime = this.currentTaskLayoutAndRecalcEvents.reduce((time, event) => {
                    return event.duration === undefined ? time : time + event.duration;
                }, 0);
                if (totalTime > TimelineModelImpl.Thresholds.ForcedLayout) {
                    for (const e of this.currentTaskLayoutAndRecalcEvents) {
                        const timelineData = EventOnTimelineData.forEvent(e);
                        timelineData.warning = e.name === RecordType.Layout ? TimelineModelImpl.WarningType.ForcedLayout :
                            TimelineModelImpl.WarningType.ForcedStyle;
                    }
                }
            }
            this.currentTaskLayoutAndRecalcEvents = [];
        }
        if (this.currentScriptEvent) {
            if (this.currentScriptEvent.endTime !== undefined && event.startTime > this.currentScriptEvent.endTime) {
                this.currentScriptEvent = null;
            }
        }
        const eventData = event.args['data'] || event.args['beginData'] || {};
        const timelineData = EventOnTimelineData.forEvent(event);
        if (eventData['stackTrace']) {
            timelineData.stackTrace = eventData['stackTrace'].map((callFrameOrProfileNode) => {
                // `callFrameOrProfileNode` can also be a `SDK.ProfileTreeModel.ProfileNode` for JSSample; that class
                // has accessors to mimic a `CallFrame`, but apparently we don't adjust stack traces in that case. Whether
                // we should is unclear.
                if (event.name !== RecordType.JSSample && event.name !== RecordType.JSSystemSample &&
                    event.name !== RecordType.JSIdleSample) {
                    // We need to copy the data so we can safely modify it below.
                    const frame = { ...callFrameOrProfileNode };
                    // TraceEvents come with 1-based line & column numbers. The frontend code
                    // requires 0-based ones. Adjust the values.
                    --frame.lineNumber;
                    --frame.columnNumber;
                    return frame;
                }
                return callFrameOrProfileNode;
            });
        }
        let pageFrameId = TimelineModelImpl.eventFrameId(event);
        const last = eventStack[eventStack.length - 1];
        if (!pageFrameId && last) {
            pageFrameId = EventOnTimelineData.forEvent(last).frameId;
        }
        timelineData.frameId = pageFrameId || (this.mainFrame && this.mainFrame.frameId) || '';
        this.asyncEventTracker.processEvent(event);
        switch (event.name) {
            case RecordType.ResourceSendRequest:
            case RecordType.WebSocketCreate: {
                timelineData.setInitiator(eventStack[eventStack.length - 1] || null);
                timelineData.url = eventData['url'];
                break;
            }
            case RecordType.ScheduleStyleRecalculation: {
                this.lastScheduleStyleRecalculation[eventData['frame']] = event;
                break;
            }
            case RecordType.UpdateLayoutTree:
            case RecordType.RecalculateStyles: {
                this.invalidationTracker.didRecalcStyle(event);
                if (event.args['beginData']) {
                    timelineData.setInitiator(this.lastScheduleStyleRecalculation[event.args['beginData']['frame']]);
                }
                this.lastRecalculateStylesEvent = event;
                if (this.currentScriptEvent) {
                    this.currentTaskLayoutAndRecalcEvents.push(event);
                }
                break;
            }
            case RecordType.ScheduleStyleInvalidationTracking:
            case RecordType.StyleRecalcInvalidationTracking:
            case RecordType.StyleInvalidatorInvalidationTracking:
            case RecordType.LayoutInvalidationTracking: {
                this.invalidationTracker.addInvalidation(new InvalidationTrackingEvent(event, timelineData));
                break;
            }
            case RecordType.InvalidateLayout: {
                // Consider style recalculation as a reason for layout invalidation,
                // but only if we had no earlier layout invalidation records.
                let layoutInitator = event;
                const frameId = eventData['frame'];
                if (!this.layoutInvalidate[frameId] && this.lastRecalculateStylesEvent &&
                    this.lastRecalculateStylesEvent.endTime !== undefined &&
                    this.lastRecalculateStylesEvent.endTime > event.startTime) {
                    layoutInitator = EventOnTimelineData.forEvent(this.lastRecalculateStylesEvent).initiator();
                }
                this.layoutInvalidate[frameId] = layoutInitator;
                break;
            }
            case RecordType.Layout: {
                this.invalidationTracker.didLayout(event);
                const frameId = event.args['beginData']['frame'];
                timelineData.setInitiator(this.layoutInvalidate[frameId]);
                // In case we have no closing Layout event, endData is not available.
                if (event.args['endData']) {
                    if (event.args['endData']['layoutRoots']) {
                        for (let i = 0; i < event.args['endData']['layoutRoots'].length; ++i) {
                            timelineData.backendNodeIds.push(event.args['endData']['layoutRoots'][i]['nodeId']);
                        }
                    }
                    else {
                        timelineData.backendNodeIds.push(event.args['endData']['rootNode']);
                    }
                }
                this.layoutInvalidate[frameId] = null;
                if (this.currentScriptEvent) {
                    this.currentTaskLayoutAndRecalcEvents.push(event);
                }
                break;
            }
            case RecordType.Task: {
                if (event.duration !== undefined && event.duration > TimelineModelImpl.Thresholds.LongTask) {
                    timelineData.warning = TimelineModelImpl.WarningType.LongTask;
                }
                break;
            }
            case RecordType.EventDispatch: {
                if (event.duration !== undefined && event.duration > TimelineModelImpl.Thresholds.RecurringHandler) {
                    timelineData.warning = TimelineModelImpl.WarningType.LongHandler;
                }
                break;
            }
            case RecordType.TimerFire:
            case RecordType.FireAnimationFrame: {
                if (event.duration !== undefined && event.duration > TimelineModelImpl.Thresholds.RecurringHandler) {
                    timelineData.warning = TimelineModelImpl.WarningType.LongRecurringHandler;
                }
                break;
            }
            // @ts-ignore fallthrough intended.
            case RecordType.FunctionCall: {
                // Compatibility with old format.
                if (typeof eventData['scriptName'] === 'string') {
                    eventData['url'] = eventData['scriptName'];
                }
                if (typeof eventData['scriptLine'] === 'number') {
                    eventData['lineNumber'] = eventData['scriptLine'];
                }
            }
            case RecordType.EvaluateScript:
            case RecordType.CompileScript:
            // @ts-ignore fallthrough intended.
            case RecordType.CacheScript: {
                if (typeof eventData['lineNumber'] === 'number') {
                    --eventData['lineNumber'];
                }
                if (typeof eventData['columnNumber'] === 'number') {
                    --eventData['columnNumber'];
                }
            }
            case RecordType.RunMicrotasks: {
                // Microtasks technically are not necessarily scripts, but for purpose of
                // forced sync style recalc or layout detection they are.
                if (!this.currentScriptEvent) {
                    this.currentScriptEvent = event;
                }
                break;
            }
            case RecordType.SetLayerTreeId: {
                // This is to support old traces.
                if (this.sessionId && eventData['sessionId'] && this.sessionId === eventData['sessionId']) {
                    this.mainFrameLayerTreeId = eventData['layerTreeId'];
                    break;
                }
                // We currently only show layer tree for the main frame.
                const frameId = TimelineModelImpl.eventFrameId(event);
                const pageFrame = frameId ? this.pageFrames.get(frameId) : null;
                if (!pageFrame || pageFrame.parent) {
                    return false;
                }
                this.mainFrameLayerTreeId = eventData['layerTreeId'];
                break;
            }
            case RecordType.Paint: {
                this.invalidationTracker.didPaint = true;
                // With CompositeAfterPaint enabled, paint events are no longer
                // associated with a Node, and nodeId will not be present.
                if ('nodeId' in eventData) {
                    timelineData.backendNodeIds.push(eventData['nodeId']);
                }
                // Only keep layer paint events, skip paints for subframes that get painted to the same layer as parent.
                if (!eventData['layerId']) {
                    break;
                }
                const layerId = eventData['layerId'];
                this.lastPaintForLayer[layerId] = event;
                break;
            }
            case RecordType.DisplayItemListSnapshot:
            case RecordType.PictureSnapshot: {
                // If we get a snapshot, we try to find the last Paint event for the
                // current layer, and store the snapshot as the relevant picture for
                // that event, thus creating a relationship between the snapshot and
                // the last Paint event for the current timestamp.
                const layerUpdateEvent = this.findAncestorEvent(RecordType.UpdateLayer);
                if (!layerUpdateEvent || layerUpdateEvent.args['layerTreeId'] !== this.mainFrameLayerTreeId) {
                    break;
                }
                const paintEvent = this.lastPaintForLayer[layerUpdateEvent.args['layerId']];
                if (paintEvent) {
                    EventOnTimelineData.forEvent(paintEvent).picture = event;
                }
                break;
            }
            case RecordType.ScrollLayer: {
                timelineData.backendNodeIds.push(eventData['nodeId']);
                break;
            }
            case RecordType.PaintImage: {
                timelineData.backendNodeIds.push(eventData['nodeId']);
                timelineData.url = eventData['url'];
                break;
            }
            case RecordType.DecodeImage:
            case RecordType.ResizeImage: {
                let paintImageEvent = this.findAncestorEvent(RecordType.PaintImage);
                if (!paintImageEvent) {
                    const decodeLazyPixelRefEvent = this.findAncestorEvent(RecordType.DecodeLazyPixelRef);
                    paintImageEvent =
                        decodeLazyPixelRefEvent && this.paintImageEventByPixelRefId[decodeLazyPixelRefEvent.args['LazyPixelRef']];
                }
                if (!paintImageEvent) {
                    break;
                }
                const paintImageData = EventOnTimelineData.forEvent(paintImageEvent);
                timelineData.backendNodeIds.push(paintImageData.backendNodeIds[0]);
                timelineData.url = paintImageData.url;
                break;
            }
            case RecordType.DrawLazyPixelRef: {
                const paintImageEvent = this.findAncestorEvent(RecordType.PaintImage);
                if (!paintImageEvent) {
                    break;
                }
                this.paintImageEventByPixelRefId[event.args['LazyPixelRef']] = paintImageEvent;
                const paintImageData = EventOnTimelineData.forEvent(paintImageEvent);
                timelineData.backendNodeIds.push(paintImageData.backendNodeIds[0]);
                timelineData.url = paintImageData.url;
                break;
            }
            case RecordType.FrameStartedLoading: {
                if (timelineData.frameId !== event.args['frame']) {
                    return false;
                }
                break;
            }
            case RecordType.MarkLCPCandidate: {
                timelineData.backendNodeIds.push(eventData['nodeId']);
                break;
            }
            case RecordType.MarkDOMContent:
            case RecordType.MarkLoad: {
                const frameId = TimelineModelImpl.eventFrameId(event);
                if (!frameId || !this.pageFrames.has(frameId)) {
                    return false;
                }
                break;
            }
            case RecordType.CommitLoad: {
                if (this.browserFrameTracking) {
                    break;
                }
                const frameId = TimelineModelImpl.eventFrameId(event);
                const isOutermostMainFrame = Boolean(eventData['isOutermostMainFrame'] ?? eventData['isMainFrame']);
                const pageFrame = frameId ? this.pageFrames.get(frameId) : null;
                if (pageFrame) {
                    pageFrame.update(event.startTime, eventData);
                }
                else {
                    // We should only have one main frame which has persistent id,
                    // unless it's an old trace without 'persistentIds' flag.
                    if (!this.persistentIds) {
                        if (eventData['page'] && eventData['page'] !== this.legacyCurrentPage) {
                            return false;
                        }
                    }
                    else if (isOutermostMainFrame) {
                        return false;
                    }
                    else if (!this.addPageFrame(event, eventData)) {
                        return false;
                    }
                }
                if (isOutermostMainFrame && frameId) {
                    const frame = this.pageFrames.get(frameId);
                    if (frame) {
                        this.mainFrame = frame;
                    }
                }
                break;
            }
            case RecordType.FireIdleCallback: {
                if (event.duration !== undefined &&
                    event.duration > eventData['allottedMilliseconds'] + TimelineModelImpl.Thresholds.IdleCallbackAddon) {
                    timelineData.warning = TimelineModelImpl.WarningType.IdleDeadlineExceeded;
                }
                break;
            }
        }
        return true;
    }
    processBrowserEvent(event) {
        if (event.name === RecordType.ResourceWillSendRequest) {
            const requestId = event.args?.data?.requestId;
            if (typeof requestId === 'string') {
                this.requestsFromBrowser.set(requestId, event);
            }
            return;
        }
        if (event.hasCategory(SDK.TracingModel.DevToolsMetadataEventCategory) && event.args['data']) {
            const data = event.args['data'];
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.TracingStartedInBrowser) {
                if (!data['persistentIds']) {
                    return;
                }
                this.browserFrameTracking = true;
                this.mainFrameNodeId = data['frameTreeNodeId'];
                const frames = data['frames'] || [];
                frames.forEach(payload => {
                    const parent = payload['parent'] && this.pageFrames.get(payload['parent']);
                    if (payload['parent'] && !parent) {
                        return;
                    }
                    let frame = this.pageFrames.get(payload['frame']);
                    if (!frame) {
                        frame = new PageFrame(payload);
                        this.pageFrames.set(frame.frameId, frame);
                        if (parent) {
                            parent.addChild(frame);
                        }
                        else {
                            this.mainFrame = frame;
                        }
                    }
                    // TODO(dgozman): this should use event.startTime, but due to races between tracing start
                    // in different processes we cannot do this yet.
                    frame.update(this.minimumRecordTimeInternal, payload);
                });
                return;
            }
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.FrameCommittedInBrowser && this.browserFrameTracking) {
                let frame = this.pageFrames.get(data['frame']);
                if (!frame) {
                    const parent = data['parent'] && this.pageFrames.get(data['parent']);
                    if (!parent) {
                        return;
                    }
                    frame = new PageFrame(data);
                    this.pageFrames.set(frame.frameId, frame);
                    parent.addChild(frame);
                }
                frame.update(event.startTime, data);
                return;
            }
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.ProcessReadyInBrowser && this.browserFrameTracking) {
                const frame = this.pageFrames.get(data['frame']);
                if (frame) {
                    frame.processReady(data['processPseudoId'], data['processId']);
                }
                return;
            }
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.FrameDeletedInBrowser && this.browserFrameTracking) {
                const frame = this.pageFrames.get(data['frame']);
                if (frame) {
                    frame.deletedTime = event.startTime;
                }
                return;
            }
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.AuctionWorkletRunningInProcess &&
                this.browserFrameTracking) {
                const worklet = new AuctionWorklet(event, data);
                this.auctionWorklets.set(data['target'], worklet);
            }
            if (event.name === TimelineModelImpl.DevToolsMetadataEvent.AuctionWorkletDoneWithProcess &&
                this.browserFrameTracking) {
                const worklet = this.auctionWorklets.get(data['target']);
                if (worklet) {
                    worklet.endTime = event.startTime;
                }
            }
        }
    }
    ensureNamedTrack(type) {
        let track = this.namedTracks.get(type);
        if (track) {
            return track;
        }
        track = new Track();
        track.type = type;
        this.tracksInternal.push(track);
        this.namedTracks.set(type, track);
        return track;
    }
    findAncestorEvent(name) {
        for (let i = this.eventStack.length - 1; i >= 0; --i) {
            const event = this.eventStack[i];
            if (event.name === name) {
                return event;
            }
        }
        return null;
    }
    addPageFrame(event, payload) {
        const parent = payload['parent'] && this.pageFrames.get(payload['parent']);
        if (payload['parent'] && !parent) {
            return false;
        }
        const pageFrame = new PageFrame(payload);
        this.pageFrames.set(pageFrame.frameId, pageFrame);
        pageFrame.update(event.startTime, payload);
        if (parent) {
            parent.addChild(pageFrame);
        }
        return true;
    }
    reset() {
        this.isGenericTraceInternal = false;
        this.tracksInternal = [];
        this.namedTracks = new Map();
        this.inspectedTargetEventsInternal = [];
        this.timeMarkerEventsInternal = [];
        this.sessionId = null;
        this.mainFrameNodeId = null;
        this.cpuProfilesInternal = [];
        this.workerIdByThread = new WeakMap();
        this.pageFrames = new Map();
        this.auctionWorklets = new Map();
        this.requestsFromBrowser = new Map();
        this.minimumRecordTimeInternal = 0;
        this.maximumRecordTimeInternal = 0;
        this.totalBlockingTimeInternal = -1;
        this.estimatedTotalBlockingTime = 0;
    }
    isGenericTrace() {
        return this.isGenericTraceInternal;
    }
    tracingModel() {
        return this.tracingModelInternal;
    }
    minimumRecordTime() {
        return this.minimumRecordTimeInternal;
    }
    maximumRecordTime() {
        return this.maximumRecordTimeInternal;
    }
    inspectedTargetEvents() {
        return this.inspectedTargetEventsInternal;
    }
    tracks() {
        return this.tracksInternal;
    }
    isEmpty() {
        return this.minimumRecordTime() === 0 && this.maximumRecordTime() === 0;
    }
    timeMarkerEvents() {
        return this.timeMarkerEventsInternal;
    }
    rootFrames() {
        return Array.from(this.pageFrames.values()).filter(frame => !frame.parent);
    }
    pageURL() {
        return this.mainFrame && this.mainFrame.url || Platform.DevToolsPath.EmptyUrlString;
    }
    pageFrameById(frameId) {
        return frameId ? this.pageFrames.get(frameId) || null : null;
    }
    networkRequests() {
        if (this.isGenericTrace()) {
            return [];
        }
        const requests = new Map();
        const requestsList = [];
        const zeroStartRequestsList = [];
        const resourceTypes = new Set([
            RecordType.ResourceWillSendRequest,
            RecordType.ResourceSendRequest,
            RecordType.ResourceReceiveResponse,
            RecordType.ResourceReceivedData,
            RecordType.ResourceFinish,
            RecordType.ResourceMarkAsCached,
        ]);
        const events = this.inspectedTargetEvents();
        for (let i = 0; i < events.length; ++i) {
            const e = events[i];
            if (!resourceTypes.has(e.name)) {
                continue;
            }
            const id = TimelineModelImpl.globalEventId(e, 'requestId');
            const requestId = e.args?.data?.requestId;
            if (e.name === RecordType.ResourceSendRequest && requestId && this.requestsFromBrowser.has(requestId)) {
                const event = this.requestsFromBrowser.get(requestId);
                if (event) {
                    addRequest(event, id);
                }
            }
            addRequest(e, id);
        }
        function addRequest(e, id) {
            let request = requests.get(id);
            if (request) {
                request.addEvent(e);
            }
            else {
                request = new NetworkRequest(e);
                requests.set(id, request);
                if (request.startTime) {
                    requestsList.push(request);
                }
                else {
                    zeroStartRequestsList.push(request);
                }
            }
        }
        return zeroStartRequestsList.concat(requestsList);
    }
}
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var RecordType;
(function (RecordType) {
    RecordType["Task"] = "RunTask";
    RecordType["Program"] = "Program";
    RecordType["EventDispatch"] = "EventDispatch";
    RecordType["GPUTask"] = "GPUTask";
    RecordType["Animation"] = "Animation";
    RecordType["RequestMainThreadFrame"] = "RequestMainThreadFrame";
    RecordType["BeginFrame"] = "BeginFrame";
    RecordType["NeedsBeginFrameChanged"] = "NeedsBeginFrameChanged";
    RecordType["BeginMainThreadFrame"] = "BeginMainThreadFrame";
    RecordType["ActivateLayerTree"] = "ActivateLayerTree";
    RecordType["DrawFrame"] = "DrawFrame";
    RecordType["DroppedFrame"] = "DroppedFrame";
    RecordType["HitTest"] = "HitTest";
    RecordType["ScheduleStyleRecalculation"] = "ScheduleStyleRecalculation";
    RecordType["RecalculateStyles"] = "RecalculateStyles";
    RecordType["UpdateLayoutTree"] = "UpdateLayoutTree";
    RecordType["InvalidateLayout"] = "InvalidateLayout";
    RecordType["Layerize"] = "Layerize";
    RecordType["Layout"] = "Layout";
    RecordType["LayoutShift"] = "LayoutShift";
    RecordType["UpdateLayer"] = "UpdateLayer";
    RecordType["UpdateLayerTree"] = "UpdateLayerTree";
    RecordType["PaintSetup"] = "PaintSetup";
    RecordType["Paint"] = "Paint";
    RecordType["PaintImage"] = "PaintImage";
    RecordType["PrePaint"] = "PrePaint";
    RecordType["Rasterize"] = "Rasterize";
    RecordType["RasterTask"] = "RasterTask";
    RecordType["ScrollLayer"] = "ScrollLayer";
    RecordType["Commit"] = "Commit";
    RecordType["CompositeLayers"] = "CompositeLayers";
    RecordType["ComputeIntersections"] = "IntersectionObserverController::computeIntersections";
    RecordType["InteractiveTime"] = "InteractiveTime";
    RecordType["ScheduleStyleInvalidationTracking"] = "ScheduleStyleInvalidationTracking";
    RecordType["StyleRecalcInvalidationTracking"] = "StyleRecalcInvalidationTracking";
    RecordType["StyleInvalidatorInvalidationTracking"] = "StyleInvalidatorInvalidationTracking";
    RecordType["LayoutInvalidationTracking"] = "LayoutInvalidationTracking";
    RecordType["ParseHTML"] = "ParseHTML";
    RecordType["ParseAuthorStyleSheet"] = "ParseAuthorStyleSheet";
    RecordType["TimerInstall"] = "TimerInstall";
    RecordType["TimerRemove"] = "TimerRemove";
    RecordType["TimerFire"] = "TimerFire";
    RecordType["XHRReadyStateChange"] = "XHRReadyStateChange";
    RecordType["XHRLoad"] = "XHRLoad";
    RecordType["CompileScript"] = "v8.compile";
    RecordType["CompileCode"] = "V8.CompileCode";
    RecordType["OptimizeCode"] = "V8.OptimizeCode";
    RecordType["EvaluateScript"] = "EvaluateScript";
    RecordType["CacheScript"] = "v8.produceCache";
    RecordType["CompileModule"] = "v8.compileModule";
    RecordType["EvaluateModule"] = "v8.evaluateModule";
    RecordType["CacheModule"] = "v8.produceModuleCache";
    RecordType["WasmStreamFromResponseCallback"] = "v8.wasm.streamFromResponseCallback";
    RecordType["WasmCompiledModule"] = "v8.wasm.compiledModule";
    RecordType["WasmCachedModule"] = "v8.wasm.cachedModule";
    RecordType["WasmModuleCacheHit"] = "v8.wasm.moduleCacheHit";
    RecordType["WasmModuleCacheInvalid"] = "v8.wasm.moduleCacheInvalid";
    RecordType["FrameStartedLoading"] = "FrameStartedLoading";
    RecordType["CommitLoad"] = "CommitLoad";
    RecordType["MarkLoad"] = "MarkLoad";
    RecordType["MarkDOMContent"] = "MarkDOMContent";
    RecordType["MarkFirstPaint"] = "firstPaint";
    RecordType["MarkFCP"] = "firstContentfulPaint";
    RecordType["MarkLCPCandidate"] = "largestContentfulPaint::Candidate";
    RecordType["MarkLCPInvalidate"] = "largestContentfulPaint::Invalidate";
    RecordType["NavigationStart"] = "navigationStart";
    RecordType["TimeStamp"] = "TimeStamp";
    RecordType["ConsoleTime"] = "ConsoleTime";
    RecordType["UserTiming"] = "UserTiming";
    RecordType["EventTiming"] = "EventTiming";
    RecordType["ResourceWillSendRequest"] = "ResourceWillSendRequest";
    RecordType["ResourceSendRequest"] = "ResourceSendRequest";
    RecordType["ResourceReceiveResponse"] = "ResourceReceiveResponse";
    RecordType["ResourceReceivedData"] = "ResourceReceivedData";
    RecordType["ResourceFinish"] = "ResourceFinish";
    RecordType["ResourceMarkAsCached"] = "ResourceMarkAsCached";
    RecordType["RunMicrotasks"] = "RunMicrotasks";
    RecordType["FunctionCall"] = "FunctionCall";
    RecordType["GCEvent"] = "GCEvent";
    RecordType["MajorGC"] = "MajorGC";
    RecordType["MinorGC"] = "MinorGC";
    // The following types are used for CPUProfile.
    // JSRoot is used for the root node.
    // JSIdleFrame and JSIdleSample are used for idle nodes.
    // JSSystemFrame and JSSystemSample are used for other system nodes.
    // JSFrame and JSSample are used for other nodes, and will be categorized as |scripting|.
    RecordType["JSFrame"] = "JSFrame";
    RecordType["JSSample"] = "JSSample";
    RecordType["JSIdleFrame"] = "JSIdleFrame";
    RecordType["JSIdleSample"] = "JSIdleSample";
    RecordType["JSSystemFrame"] = "JSSystemFrame";
    RecordType["JSSystemSample"] = "JSSystemSample";
    RecordType["JSRoot"] = "JSRoot";
    // V8Sample events are coming from tracing and contain raw stacks with function addresses.
    // After being processed with help of JitCodeAdded and JitCodeMoved events they
    // get translated into function infos and stored as stacks in JSSample events.
    RecordType["V8Sample"] = "V8Sample";
    RecordType["JitCodeAdded"] = "JitCodeAdded";
    RecordType["JitCodeMoved"] = "JitCodeMoved";
    RecordType["StreamingCompileScript"] = "v8.parseOnBackground";
    RecordType["StreamingCompileScriptWaiting"] = "v8.parseOnBackgroundWaiting";
    RecordType["StreamingCompileScriptParsing"] = "v8.parseOnBackgroundParsing";
    RecordType["BackgroundDeserialize"] = "v8.deserializeOnBackground";
    RecordType["FinalizeDeserialization"] = "V8.FinalizeDeserialization";
    RecordType["V8Execute"] = "V8.Execute";
    RecordType["UpdateCounters"] = "UpdateCounters";
    RecordType["RequestAnimationFrame"] = "RequestAnimationFrame";
    RecordType["CancelAnimationFrame"] = "CancelAnimationFrame";
    RecordType["FireAnimationFrame"] = "FireAnimationFrame";
    RecordType["RequestIdleCallback"] = "RequestIdleCallback";
    RecordType["CancelIdleCallback"] = "CancelIdleCallback";
    RecordType["FireIdleCallback"] = "FireIdleCallback";
    RecordType["WebSocketCreate"] = "WebSocketCreate";
    RecordType["WebSocketSendHandshakeRequest"] = "WebSocketSendHandshakeRequest";
    RecordType["WebSocketReceiveHandshakeResponse"] = "WebSocketReceiveHandshakeResponse";
    RecordType["WebSocketDestroy"] = "WebSocketDestroy";
    RecordType["EmbedderCallback"] = "EmbedderCallback";
    RecordType["SetLayerTreeId"] = "SetLayerTreeId";
    RecordType["TracingStartedInPage"] = "TracingStartedInPage";
    RecordType["TracingSessionIdForWorker"] = "TracingSessionIdForWorker";
    RecordType["StartProfiling"] = "CpuProfiler::StartProfiling";
    RecordType["DecodeImage"] = "Decode Image";
    RecordType["ResizeImage"] = "Resize Image";
    RecordType["DrawLazyPixelRef"] = "Draw LazyPixelRef";
    RecordType["DecodeLazyPixelRef"] = "Decode LazyPixelRef";
    RecordType["LazyPixelRef"] = "LazyPixelRef";
    RecordType["LayerTreeHostImplSnapshot"] = "cc::LayerTreeHostImpl";
    RecordType["PictureSnapshot"] = "cc::Picture";
    RecordType["DisplayItemListSnapshot"] = "cc::DisplayItemList";
    RecordType["InputLatencyMouseMove"] = "InputLatency::MouseMove";
    RecordType["InputLatencyMouseWheel"] = "InputLatency::MouseWheel";
    RecordType["ImplSideFling"] = "InputHandlerProxy::HandleGestureFling::started";
    RecordType["GCCollectGarbage"] = "BlinkGC.AtomicPhase";
    RecordType["CryptoDoEncrypt"] = "DoEncrypt";
    RecordType["CryptoDoEncryptReply"] = "DoEncryptReply";
    RecordType["CryptoDoDecrypt"] = "DoDecrypt";
    RecordType["CryptoDoDecryptReply"] = "DoDecryptReply";
    RecordType["CryptoDoDigest"] = "DoDigest";
    RecordType["CryptoDoDigestReply"] = "DoDigestReply";
    RecordType["CryptoDoSign"] = "DoSign";
    RecordType["CryptoDoSignReply"] = "DoSignReply";
    RecordType["CryptoDoVerify"] = "DoVerify";
    RecordType["CryptoDoVerifyReply"] = "DoVerifyReply";
    // CpuProfile is a virtual event created on frontend to support
    // serialization of CPU Profiles within tracing timeline data.
    RecordType["CpuProfile"] = "CpuProfile";
    RecordType["Profile"] = "Profile";
    RecordType["AsyncTask"] = "AsyncTask";
})(RecordType || (RecordType = {}));
(function (TimelineModelImpl) {
    TimelineModelImpl.Category = {
        Console: 'blink.console',
        UserTiming: 'blink.user_timing',
        Loading: 'loading',
    };
    // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration)
    // eslint-disable-next-line rulesdir/const_enum
    let WarningType;
    (function (WarningType) {
        WarningType["LongTask"] = "LongTask";
        WarningType["ForcedStyle"] = "ForcedStyle";
        WarningType["ForcedLayout"] = "ForcedLayout";
        WarningType["IdleDeadlineExceeded"] = "IdleDeadlineExceeded";
        WarningType["LongHandler"] = "LongHandler";
        WarningType["LongRecurringHandler"] = "LongRecurringHandler";
        WarningType["V8Deopt"] = "V8Deopt";
        WarningType["LongInteraction"] = "LongInteraction";
    })(WarningType = TimelineModelImpl.WarningType || (TimelineModelImpl.WarningType = {}));
    TimelineModelImpl.WorkerThreadName = 'DedicatedWorker thread';
    TimelineModelImpl.WorkerThreadNameLegacy = 'DedicatedWorker Thread';
    TimelineModelImpl.RendererMainThreadName = 'CrRendererMain';
    TimelineModelImpl.BrowserMainThreadName = 'CrBrowserMain';
    // The names of threads before M111 were exactly this, but afterwards have
    // it a suffix after the exact role.
    TimelineModelImpl.UtilityMainThreadNameSuffix = 'CrUtilityMain';
    TimelineModelImpl.AuctionWorkletThreadName = 'AuctionV8HelperThread';
    TimelineModelImpl.DevToolsMetadataEvent = {
        TracingStartedInBrowser: 'TracingStartedInBrowser',
        TracingStartedInPage: 'TracingStartedInPage',
        TracingSessionIdForWorker: 'TracingSessionIdForWorker',
        FrameCommittedInBrowser: 'FrameCommittedInBrowser',
        ProcessReadyInBrowser: 'ProcessReadyInBrowser',
        FrameDeletedInBrowser: 'FrameDeletedInBrowser',
        AuctionWorkletRunningInProcess: 'AuctionWorkletRunningInProcess',
        AuctionWorkletDoneWithProcess: 'AuctionWorkletDoneWithProcess',
    };
    TimelineModelImpl.Thresholds = {
        LongTask: 50,
        Handler: 150,
        RecurringHandler: 50,
        ForcedLayout: 30,
        IdleCallbackAddon: 5,
    };
})(TimelineModelImpl || (TimelineModelImpl = {}));
export class Track {
    name;
    type;
    forMainFrame;
    url;
    /**
     * For tracks that correspond to a thread in a trace, this field contains all the events in the
     * thread (both sync and async). Other tracks (like Timings) only include events with instant
     * ("I") or mark ("R") phases.
     */
    events;
    /**
     * For tracks that correspond to a thread in a trace, this field will be empty. Other tracks (like
     * Interactions and Animations) have non-instant/mark events.
     */
    asyncEvents;
    tasks;
    eventsForTreeViewInternal;
    thread;
    constructor() {
        this.name = '';
        this.type = TrackType.Other;
        // TODO(dgozman): replace forMainFrame with a list of frames, urls and time ranges.
        this.forMainFrame = false;
        this.url = Platform.DevToolsPath.EmptyUrlString;
        // TODO(dgozman): do not distinguish between sync and async events.
        this.events = [];
        this.asyncEvents = [];
        this.tasks = [];
        this.eventsForTreeViewInternal = null;
        this.thread = null;
    }
    /**
     * Gets trace events that can be organized in a tree structure. This
     * is used for the tree views in the Bottom-up, Call tree and Event
     * log view in the details pane.
     *
     * Depending on the type of track, this data can vary:
     * 1. Tracks that correspond to a thread in a trace:
     *    Returns all the events (sync and async). For these tracks, all
     *    events will be inside the `events` field. Async events will be
     *    filtered later when the trees are actually built. For these
     *    tracks, the asyncEvents field will be empty.
     *
     * 2. Other tracks (Interactions, Timings, etc.):
     *    Returns instant events (which for these tracks are stored in the
     *    `events` field) and async events (contained in `syncEvents`) if
     *    they can be organized in a tree structure. This latter condition
     *    is met if there is *not* a pair of async events e1 and e2 where:
     *
     *    e1.startTime <= e2.startTime && e1.endTime > e2.startTime && e1.endTime > e2.endTime.
     *    or, graphically:
     *    |------- e1 ------|
     *      |------- e2 --------|
     *    Because async events are filtered later, fake sync events are
     *    created from the async events when the condition above is met.
     */
    eventsForTreeView() {
        if (this.eventsForTreeViewInternal) {
            return this.eventsForTreeViewInternal;
        }
        const stack = [];
        function peekLastEndTime() {
            const last = stack[stack.length - 1];
            if (last !== undefined) {
                const endTime = last.endTime;
                if (endTime !== undefined) {
                    return endTime;
                }
            }
            throw new Error('End time does not exist on event.');
        }
        this.eventsForTreeViewInternal = [...this.events];
        // Attempt to build a tree from async events, as if they where
        // sync.
        for (const event of this.asyncEvents) {
            const startTime = event.startTime;
            let endTime = event.endTime;
            if (endTime === undefined) {
                endTime = startTime;
            }
            // Look for a potential parent for this event:
            // one whose end time is after this event start time.
            while (stack.length && startTime >= peekLastEndTime()) {
                stack.pop();
            }
            if (stack.length && endTime > peekLastEndTime()) {
                // If such an event exists but its end time is before this
                // event's end time (they cannot be nested), then a tree cannot
                // be made from this track's async events. Return the sync
                // events.
                this.eventsForTreeViewInternal = [...this.events];
                break;
            }
            const fakeSyncEvent = new SDK.TracingModel.ConstructedEvent(event.categoriesString, event.name, "X" /* TraceEngine.Types.TraceEvents.Phase.COMPLETE */, startTime, event.thread);
            fakeSyncEvent.setEndTime(endTime);
            fakeSyncEvent.addArgs(event.args);
            this.eventsForTreeViewInternal.push(fakeSyncEvent);
            stack.push(fakeSyncEvent);
        }
        return this.eventsForTreeViewInternal;
    }
}
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var TrackType;
(function (TrackType) {
    TrackType["MainThread"] = "MainThread";
    TrackType["Worker"] = "Worker";
    TrackType["Animation"] = "Animation";
    TrackType["Raster"] = "Raster";
    TrackType["Experience"] = "Experience";
    TrackType["Other"] = "Other";
})(TrackType || (TrackType = {}));
export class PageFrame {
    frameId;
    url;
    name;
    children;
    parent;
    processes;
    deletedTime;
    ownerNode;
    constructor(payload) {
        this.frameId = payload['frame'];
        this.url = payload['url'] || Platform.DevToolsPath.EmptyUrlString;
        this.name = payload['name'];
        this.children = [];
        this.parent = null;
        this.processes = [];
        this.deletedTime = null;
        // TODO(dgozman): figure this out.
        // this.ownerNode = target && payload['nodeId'] ? new SDK.DOMModel.DeferredDOMNode(target, payload['nodeId']) : null;
        this.ownerNode = null;
    }
    update(time, payload) {
        this.url = payload['url'] || '';
        this.name = payload['name'];
        if (payload['processId']) {
            this.processes.push({ time: time, processId: payload['processId'], processPseudoId: '', url: payload['url'] || '' });
        }
        else {
            this.processes.push({ time: time, processId: -1, processPseudoId: payload['processPseudoId'], url: payload['url'] || '' });
        }
    }
    processReady(processPseudoId, processId) {
        for (const process of this.processes) {
            if (process.processPseudoId === processPseudoId) {
                process.processPseudoId = '';
                process.processId = processId;
            }
        }
    }
    addChild(child) {
        this.children.push(child);
        child.parent = this;
    }
}
export class AuctionWorklet {
    targetId;
    processId;
    host;
    startTime;
    endTime;
    workletType;
    constructor(event, data) {
        this.targetId = (typeof data['target'] === 'string') ? data['target'] : '';
        this.processId = (typeof data['pid'] === 'number') ? data['pid'] : 0;
        this.host = (typeof data['host'] === 'string') ? data['host'] : undefined;
        this.startTime = event.startTime;
        this.endTime = Infinity;
        if (data['type'] === 'bidder') {
            this.workletType = 1 /* WorkletType.BidderWorklet */;
        }
        else if (data['type'] === 'seller') {
            this.workletType = 2 /* WorkletType.SellerWorklet */;
        }
        else {
            this.workletType = 3 /* WorkletType.UnknownWorklet */;
        }
    }
}
export class NetworkRequest {
    startTime;
    endTime;
    encodedDataLength;
    decodedBodyLength;
    children;
    timing;
    mimeType;
    url;
    requestMethod;
    transferSize;
    maybeDiskCached;
    memoryCachedInternal;
    priority;
    finishTime;
    responseTime;
    fromServiceWorker;
    hasCachedResource;
    constructor(event) {
        const isInitial = event.name === RecordType.ResourceSendRequest || event.name === RecordType.ResourceWillSendRequest;
        this.startTime = isInitial ? event.startTime : 0;
        this.endTime = Infinity;
        this.encodedDataLength = 0;
        this.decodedBodyLength = 0;
        this.children = [];
        this.transferSize = 0;
        this.maybeDiskCached = false;
        this.memoryCachedInternal = false;
        this.addEvent(event);
    }
    addEvent(event) {
        this.children.push(event);
        // This Math.min is likely because of BUG(chromium:865066).
        this.startTime = Math.min(this.startTime, event.startTime);
        const eventData = event.args['data'];
        if (eventData['mimeType']) {
            this.mimeType = eventData['mimeType'];
        }
        if ('priority' in eventData) {
            this.priority = eventData['priority'];
        }
        if (event.name === RecordType.ResourceFinish) {
            this.endTime = event.startTime;
        }
        if (eventData['finishTime']) {
            this.finishTime = eventData['finishTime'] * 1000;
        }
        if (!this.responseTime &&
            (event.name === RecordType.ResourceReceiveResponse || event.name === RecordType.ResourceReceivedData)) {
            this.responseTime = event.startTime;
        }
        const encodedDataLength = eventData['encodedDataLength'] || 0;
        if (event.name === RecordType.ResourceMarkAsCached) {
            // This is a reliable signal for memory caching.
            this.memoryCachedInternal = true;
        }
        if (event.name === RecordType.ResourceReceiveResponse) {
            if (eventData['fromCache']) {
                // See BUG(chromium:998397): back-end over-approximates caching.
                this.maybeDiskCached = true;
            }
            if (eventData['fromServiceWorker']) {
                this.fromServiceWorker = true;
            }
            if (eventData['hasCachedResource']) {
                this.hasCachedResource = true;
            }
            this.encodedDataLength = encodedDataLength;
        }
        if (event.name === RecordType.ResourceReceivedData) {
            this.encodedDataLength += encodedDataLength;
        }
        if (event.name === RecordType.ResourceFinish && encodedDataLength) {
            this.encodedDataLength = encodedDataLength;
            // If a ResourceFinish event with an encoded data length is received,
            // then the resource was not cached; it was fetched before it was
            // requested, e.g. because it was pushed in this navigation.
            this.transferSize = encodedDataLength;
        }
        const decodedBodyLength = eventData['decodedBodyLength'];
        if (event.name === RecordType.ResourceFinish && decodedBodyLength) {
            this.decodedBodyLength = decodedBodyLength;
        }
        if (!this.url) {
            this.url = eventData['url'];
        }
        if (!this.requestMethod) {
            this.requestMethod = eventData['requestMethod'];
        }
        if (!this.timing) {
            this.timing = eventData['timing'];
        }
        if (eventData['fromServiceWorker']) {
            this.fromServiceWorker = true;
        }
    }
    /**
     * Return whether this request was cached. This works around BUG(chromium:998397),
     * which reports pushed resources, and resources serverd by a service worker as
     * disk cached. Pushed resources that were not disk cached, however, have a non-zero
     * `transferSize`.
     */
    cached() {
        return Boolean(this.memoryCachedInternal) ||
            (Boolean(this.maybeDiskCached) && !this.transferSize && !this.fromServiceWorker);
    }
    /**
     * Return whether this request was served from a memory cache.
     */
    memoryCached() {
        return this.memoryCachedInternal;
    }
    /**
     * Get the timing information for this request. If the request was cached,
     * the timing refers to the original (uncached) load, and should not be used.
     */
    getSendReceiveTiming() {
        if (this.cached() || !this.timing) {
            // If the request is served from cache, the timing refers to the original
            // resource load, and should not be used.
            return { sendStartTime: this.startTime, headersEndTime: this.startTime };
        }
        const requestTime = this.timing.requestTime * 1000;
        const sendStartTime = requestTime + this.timing.sendStart;
        const headersEndTime = requestTime + this.timing.receiveHeadersEnd;
        return { sendStartTime, headersEndTime };
    }
    /**
     * Get the start time of this request, i.e. the time when the browser or
     * renderer queued this request. There are two cases where request time is
     * earlier than `startTime`: (1) if the request is served from cache, because
     * it refers to the original load of the resource. (2) if the request was
     * initiated by the browser instead of the renderer. Only in case (2) the
     * the request time must be used instead of the start time to work around
     * BUG(chromium:865066).
     */
    getStartTime() {
        return Math.min(this.startTime, !this.cached() && this.timing && this.timing.requestTime * 1000 || Infinity);
    }
    /**
     * Returns the time where the earliest event belonging to this request starts.
     * This differs from `getStartTime()` if a previous HTTP/2 request pushed the
     * resource proactively: Then `beginTime()` refers to the time the push was received.
     */
    beginTime() {
        // `pushStart` is referring to the original push if the request was cached (i.e. in
        // general not the most recent push), and should hence only be used for requests that were not cached.
        return Math.min(this.getStartTime(), !this.cached() && this.timing && this.timing.pushStart * 1000 || Infinity);
    }
}
export class InvalidationTrackingEvent {
    type;
    startTime;
    tracingEvent;
    frame;
    nodeId;
    nodeName;
    invalidationSet;
    invalidatedSelectorId;
    changedId;
    changedClass;
    changedAttribute;
    changedPseudo;
    selectorPart;
    extraData;
    invalidationList;
    cause;
    linkedRecalcStyleEvent;
    linkedLayoutEvent;
    constructor(event, timelineData) {
        this.type = event.name;
        this.startTime = event.startTime;
        this.tracingEvent = event;
        const eventData = event.args['data'];
        this.frame = eventData['frame'];
        this.nodeId = eventData['nodeId'];
        this.nodeName = eventData['nodeName'];
        this.invalidationSet = eventData['invalidationSet'];
        this.invalidatedSelectorId = eventData['invalidatedSelectorId'];
        this.changedId = eventData['changedId'];
        this.changedClass = eventData['changedClass'];
        this.changedAttribute = eventData['changedAttribute'];
        this.changedPseudo = eventData['changedPseudo'];
        this.selectorPart = eventData['selectorPart'];
        this.extraData = eventData['extraData'];
        this.invalidationList = eventData['invalidationList'];
        this.cause = { reason: eventData['reason'], stackTrace: timelineData.stackTrace };
        this.linkedRecalcStyleEvent = false;
        this.linkedLayoutEvent = false;
        // FIXME: Move this to TimelineUIUtils.js.
        if (!this.cause.reason && this.cause.stackTrace && this.type === RecordType.LayoutInvalidationTracking) {
            this.cause.reason = 'Layout forced';
        }
    }
}
export class InvalidationTracker {
    lastRecalcStyle;
    didPaint;
    invalidations;
    invalidationsByNodeId;
    constructor() {
        this.lastRecalcStyle = null;
        this.didPaint = false;
        this.initializePerFrameState();
        this.invalidations = {};
        this.invalidationsByNodeId = {};
    }
    static invalidationEventsFor(event) {
        return eventToInvalidation.get(event) || null;
    }
    addInvalidation(invalidation) {
        this.startNewFrameIfNeeded();
        if (!invalidation.nodeId) {
            console.error('Invalidation lacks node information.');
            console.error(invalidation);
            return;
        }
        // Suppress StyleInvalidator StyleRecalcInvalidationTracking invalidations because they
        // will be handled by StyleInvalidatorInvalidationTracking.
        // FIXME: Investigate if we can remove StyleInvalidator invalidations entirely.
        if (invalidation.type === RecordType.StyleRecalcInvalidationTracking &&
            invalidation.cause.reason === 'StyleInvalidator') {
            return;
        }
        // Style invalidation events can occur before and during recalc style. didRecalcStyle
        // handles style invalidations that occur before the recalc style event but we need to
        // handle style recalc invalidations during recalc style here.
        const styleRecalcInvalidation = (invalidation.type === RecordType.ScheduleStyleInvalidationTracking ||
            invalidation.type === RecordType.StyleInvalidatorInvalidationTracking ||
            invalidation.type === RecordType.StyleRecalcInvalidationTracking);
        if (styleRecalcInvalidation) {
            const duringRecalcStyle = invalidation.startTime && this.lastRecalcStyle &&
                this.lastRecalcStyle.endTime !== undefined && invalidation.startTime >= this.lastRecalcStyle.startTime &&
                invalidation.startTime <= this.lastRecalcStyle.endTime;
            if (duringRecalcStyle) {
                this.associateWithLastRecalcStyleEvent(invalidation);
            }
        }
        // Record the invalidation so later events can look it up.
        if (this.invalidations[invalidation.type]) {
            this.invalidations[invalidation.type].push(invalidation);
        }
        else {
            this.invalidations[invalidation.type] = [invalidation];
        }
        if (invalidation.nodeId) {
            if (this.invalidationsByNodeId[invalidation.nodeId]) {
                this.invalidationsByNodeId[invalidation.nodeId].push(invalidation);
            }
            else {
                this.invalidationsByNodeId[invalidation.nodeId] = [invalidation];
            }
        }
    }
    didRecalcStyle(recalcStyleEvent) {
        this.lastRecalcStyle = recalcStyleEvent;
        const types = [
            RecordType.ScheduleStyleInvalidationTracking,
            RecordType.StyleInvalidatorInvalidationTracking,
            RecordType.StyleRecalcInvalidationTracking,
        ];
        for (const invalidation of this.invalidationsOfTypes(types)) {
            this.associateWithLastRecalcStyleEvent(invalidation);
        }
    }
    associateWithLastRecalcStyleEvent(invalidation) {
        if (invalidation.linkedRecalcStyleEvent) {
            return;
        }
        if (!this.lastRecalcStyle) {
            throw new Error('Last recalculate style event not set.');
        }
        const recalcStyleFrameId = this.lastRecalcStyle.args['beginData']['frame'];
        if (invalidation.type === RecordType.StyleInvalidatorInvalidationTracking) {
            // Instead of calling addInvalidationToEvent directly, we create synthetic
            // StyleRecalcInvalidationTracking events which will be added in addInvalidationToEvent.
            this.addSyntheticStyleRecalcInvalidations(this.lastRecalcStyle, recalcStyleFrameId, invalidation);
        }
        else if (invalidation.type === RecordType.ScheduleStyleInvalidationTracking) {
            // ScheduleStyleInvalidationTracking events are only used for adding information to
            // StyleInvalidatorInvalidationTracking events. See: addSyntheticStyleRecalcInvalidations.
        }
        else {
            this.addInvalidationToEvent(this.lastRecalcStyle, recalcStyleFrameId, invalidation);
        }
        invalidation.linkedRecalcStyleEvent = true;
    }
    addSyntheticStyleRecalcInvalidations(event, frameId, styleInvalidatorInvalidation) {
        if (!styleInvalidatorInvalidation.invalidationList) {
            this.addSyntheticStyleRecalcInvalidation(styleInvalidatorInvalidation.tracingEvent, styleInvalidatorInvalidation);
            return;
        }
        if (!styleInvalidatorInvalidation.nodeId) {
            console.error('Invalidation lacks node information.');
            console.error(styleInvalidatorInvalidation);
            return;
        }
        for (let i = 0; i < styleInvalidatorInvalidation.invalidationList.length; i++) {
            const setId = styleInvalidatorInvalidation.invalidationList[i]['id'];
            let lastScheduleStyleRecalculation;
            const nodeInvalidations = this.invalidationsByNodeId[styleInvalidatorInvalidation.nodeId] || [];
            for (let j = 0; j < nodeInvalidations.length; j++) {
                const invalidation = nodeInvalidations[j];
                if (invalidation.frame !== frameId || invalidation.invalidationSet !== setId ||
                    invalidation.type !== RecordType.ScheduleStyleInvalidationTracking) {
                    continue;
                }
                lastScheduleStyleRecalculation = invalidation;
            }
            if (!lastScheduleStyleRecalculation) {
                continue;
            }
            this.addSyntheticStyleRecalcInvalidation(lastScheduleStyleRecalculation.tracingEvent, styleInvalidatorInvalidation);
        }
    }
    addSyntheticStyleRecalcInvalidation(baseEvent, styleInvalidatorInvalidation) {
        const timelineData = EventOnTimelineData.forEvent(baseEvent);
        const invalidation = new InvalidationTrackingEvent(baseEvent, timelineData);
        invalidation.type = RecordType.StyleRecalcInvalidationTracking;
        if (styleInvalidatorInvalidation.cause.reason) {
            invalidation.cause.reason = styleInvalidatorInvalidation.cause.reason;
        }
        if (styleInvalidatorInvalidation.selectorPart) {
            invalidation.selectorPart = styleInvalidatorInvalidation.selectorPart;
        }
        if (!invalidation.linkedRecalcStyleEvent) {
            this.associateWithLastRecalcStyleEvent(invalidation);
        }
    }
    didLayout(layoutEvent) {
        const layoutFrameId = layoutEvent.args['beginData']['frame'];
        for (const invalidation of this.invalidationsOfTypes([RecordType.LayoutInvalidationTracking])) {
            if (invalidation.linkedLayoutEvent) {
                continue;
            }
            this.addInvalidationToEvent(layoutEvent, layoutFrameId, invalidation);
            invalidation.linkedLayoutEvent = true;
        }
    }
    addInvalidationToEvent(event, eventFrameId, invalidation) {
        if (eventFrameId !== invalidation.frame) {
            return;
        }
        const invalidations = eventToInvalidation.get(event);
        if (!invalidations) {
            eventToInvalidation.set(event, [invalidation]);
        }
        else {
            invalidations.push(invalidation);
        }
    }
    invalidationsOfTypes(types) {
        const invalidations = this.invalidations;
        if (!types) {
            types = Object.keys(invalidations);
        }
        function* generator() {
            if (!types) {
                return;
            }
            for (let i = 0; i < types.length; ++i) {
                const invalidationList = invalidations[types[i]] || [];
                for (let j = 0; j < invalidationList.length; ++j) {
                    yield invalidationList[j];
                }
            }
        }
        return generator();
    }
    startNewFrameIfNeeded() {
        if (!this.didPaint) {
            return;
        }
        this.initializePerFrameState();
    }
    initializePerFrameState() {
        this.invalidations = {};
        this.invalidationsByNodeId = {};
        this.lastRecalcStyle = null;
        this.didPaint = false;
    }
}
export class TimelineAsyncEventTracker {
    initiatorByType;
    constructor() {
        TimelineAsyncEventTracker.initialize();
        this.initiatorByType = new Map();
        if (TimelineAsyncEventTracker.asyncEvents) {
            for (const initiator of TimelineAsyncEventTracker.asyncEvents.keys()) {
                this.initiatorByType.set(initiator, new Map());
            }
        }
    }
    static initialize() {
        if (TimelineAsyncEventTracker.asyncEvents) {
            return;
        }
        const events = new Map();
        events.set(RecordType.TimerInstall, { causes: [RecordType.TimerFire], joinBy: 'timerId' });
        events.set(RecordType.ResourceSendRequest, {
            causes: [
                RecordType.ResourceMarkAsCached,
                RecordType.ResourceReceiveResponse,
                RecordType.ResourceReceivedData,
                RecordType.ResourceFinish,
            ],
            joinBy: 'requestId',
        });
        events.set(RecordType.RequestAnimationFrame, { causes: [RecordType.FireAnimationFrame], joinBy: 'id' });
        events.set(RecordType.RequestIdleCallback, { causes: [RecordType.FireIdleCallback], joinBy: 'id' });
        events.set(RecordType.WebSocketCreate, {
            causes: [
                RecordType.WebSocketSendHandshakeRequest,
                RecordType.WebSocketReceiveHandshakeResponse,
                RecordType.WebSocketDestroy,
            ],
            joinBy: 'identifier',
        });
        TimelineAsyncEventTracker.asyncEvents = events;
        TimelineAsyncEventTracker.typeToInitiator = new Map();
        for (const entry of events) {
            const types = entry[1].causes;
            for (const currentType of types) {
                TimelineAsyncEventTracker.typeToInitiator.set(currentType, entry[0]);
            }
        }
    }
    processEvent(event) {
        if (!TimelineAsyncEventTracker.typeToInitiator || !TimelineAsyncEventTracker.asyncEvents) {
            return;
        }
        let initiatorType = TimelineAsyncEventTracker.typeToInitiator.get(event.name);
        const isInitiator = !initiatorType;
        if (!initiatorType) {
            initiatorType = event.name;
        }
        const initiatorInfo = TimelineAsyncEventTracker.asyncEvents.get(initiatorType);
        if (!initiatorInfo) {
            return;
        }
        const id = TimelineModelImpl.globalEventId(event, initiatorInfo.joinBy);
        if (!id) {
            return;
        }
        const initiatorMap = this.initiatorByType.get(initiatorType);
        if (initiatorMap) {
            if (isInitiator) {
                initiatorMap.set(id, event);
                return;
            }
            const initiator = initiatorMap.get(id);
            const timelineData = EventOnTimelineData.forEvent(event);
            timelineData.setInitiator(initiator ? initiator : null);
            if (!timelineData.frameId && initiator) {
                timelineData.frameId = TimelineModelImpl.eventFrameId(initiator);
            }
        }
    }
    static asyncEvents = null;
    static typeToInitiator = null;
}
export class EventOnTimelineData {
    warning;
    previewElement;
    url;
    backendNodeIds;
    stackTrace;
    picture;
    initiatorInternal;
    frameId;
    timeWaitingForMainThread;
    constructor() {
        this.warning = null;
        this.previewElement = null;
        this.url = null;
        this.backendNodeIds = [];
        this.stackTrace = null;
        this.picture = null;
        this.initiatorInternal = null;
        this.frameId = null;
    }
    setInitiator(initiator) {
        this.initiatorInternal = initiator;
        if (!initiator || this.url) {
            return;
        }
        const initiatorURL = EventOnTimelineData.forEvent(initiator).url;
        if (initiatorURL) {
            this.url = initiatorURL;
        }
    }
    initiator() {
        return this.initiatorInternal;
    }
    topFrame() {
        const stackTrace = this.stackTraceForSelfOrInitiator();
        return stackTrace && stackTrace[0] || null;
    }
    stackTraceForSelfOrInitiator() {
        return this.stackTrace ||
            (this.initiatorInternal && EventOnTimelineData.forEvent(this.initiatorInternal).stackTrace);
    }
    static forEvent(event) {
        if (event instanceof SDK.TracingModel.PayloadEvent) {
            return EventOnTimelineData.forTraceEventData(event.rawPayload());
        }
        if (!(event instanceof SDK.TracingModel.Event)) {
            return EventOnTimelineData.forTraceEventData(event);
        }
        return getOrCreateEventData(event);
    }
    static forTraceEventData(event) {
        return getOrCreateEventData(event);
    }
}
function getOrCreateEventData(event) {
    let data = eventToData.get(event);
    if (!data) {
        data = new EventOnTimelineData();
        eventToData.set(event, data);
    }
    return data;
}
const eventToData = new WeakMap();
const eventToInvalidation = new WeakMap();
//# sourceMappingURL=TimelineModel.js.map