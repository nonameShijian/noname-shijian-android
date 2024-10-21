// Copyright (c) 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as Common from '../../core/common/common.js';
import * as Platform from '../../core/platform/platform.js';
import * as SDK from '../../core/sdk/sdk.js';
import * as TextUtils from '../../models/text_utils/text_utils.js';
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var Events;
(function (Events) {
    Events["CoverageUpdated"] = "CoverageUpdated";
    Events["CoverageReset"] = "CoverageReset";
})(Events || (Events = {}));
const COVERAGE_POLLING_PERIOD_MS = 200;
export class CoverageModel extends SDK.SDKModel.SDKModel {
    cpuProfilerModel;
    cssModel;
    debuggerModel;
    coverageByURL;
    coverageByContentProvider;
    coverageUpdateTimes;
    suspensionState;
    pollTimer;
    currentPollPromise;
    shouldResumePollingOnResume;
    jsBacklog;
    cssBacklog;
    performanceTraceRecording;
    constructor(target) {
        super(target);
        this.cpuProfilerModel = target.model(SDK.CPUProfilerModel.CPUProfilerModel);
        this.cssModel = target.model(SDK.CSSModel.CSSModel);
        this.debuggerModel = target.model(SDK.DebuggerModel.DebuggerModel);
        this.coverageByURL = new Map();
        this.coverageByContentProvider = new Map();
        // We keep track of the update times, because the other data-structures don't change if an
        // update doesn't change the coverage. Some visualizations want to convey to the user that
        // an update was received at a certain time, but did not result in a coverage change.
        this.coverageUpdateTimes = new Set();
        this.suspensionState = "Active" /* SuspensionState.Active */;
        this.pollTimer = null;
        this.currentPollPromise = null;
        this.shouldResumePollingOnResume = false;
        this.jsBacklog = [];
        this.cssBacklog = [];
        this.performanceTraceRecording = false;
    }
    async start(jsCoveragePerBlock) {
        if (this.suspensionState !== "Active" /* SuspensionState.Active */) {
            throw Error('Cannot start CoverageModel while it is not active.');
        }
        const promises = [];
        if (this.cssModel) {
            // Note there's no JS coverage since JS won't ever return
            // coverage twice, even after it's restarted.
            this.clearCSS();
            this.cssModel.addEventListener(SDK.CSSModel.Events.StyleSheetAdded, this.handleStyleSheetAdded, this);
            promises.push(this.cssModel.startCoverage());
        }
        if (this.cpuProfilerModel) {
            promises.push(this.cpuProfilerModel.startPreciseCoverage(jsCoveragePerBlock, this.preciseCoverageDeltaUpdate.bind(this)));
        }
        await Promise.all(promises);
        return Boolean(this.cssModel || this.cpuProfilerModel);
    }
    preciseCoverageDeltaUpdate(timestamp, occasion, coverageData) {
        this.coverageUpdateTimes.add(timestamp);
        void this.backlogOrProcessJSCoverage(coverageData, timestamp);
    }
    async stop() {
        await this.stopPolling();
        const promises = [];
        if (this.cpuProfilerModel) {
            promises.push(this.cpuProfilerModel.stopPreciseCoverage());
        }
        if (this.cssModel) {
            promises.push(this.cssModel.stopCoverage());
            this.cssModel.removeEventListener(SDK.CSSModel.Events.StyleSheetAdded, this.handleStyleSheetAdded, this);
        }
        await Promise.all(promises);
    }
    reset() {
        this.coverageByURL = new Map();
        this.coverageByContentProvider = new Map();
        this.coverageUpdateTimes = new Set();
        this.dispatchEventToListeners(Events.CoverageReset);
    }
    async startPolling() {
        if (this.currentPollPromise || this.suspensionState !== "Active" /* SuspensionState.Active */) {
            return;
        }
        await this.pollLoop();
    }
    async pollLoop() {
        this.clearTimer();
        this.currentPollPromise = this.pollAndCallback();
        await this.currentPollPromise;
        if (this.suspensionState === "Active" /* SuspensionState.Active */ || this.performanceTraceRecording) {
            this.pollTimer = window.setTimeout(() => this.pollLoop(), COVERAGE_POLLING_PERIOD_MS);
        }
    }
    async stopPolling() {
        this.clearTimer();
        await this.currentPollPromise;
        this.currentPollPromise = null;
        // Do one last poll to get the final data.
        await this.pollAndCallback();
    }
    async pollAndCallback() {
        if (this.suspensionState === "Suspended" /* SuspensionState.Suspended */ && !this.performanceTraceRecording) {
            return;
        }
        const updates = await this.takeAllCoverage();
        // This conditional should never trigger, as all intended ways to stop
        // polling are awaiting the `_currentPollPromise` before suspending.
        console.assert(this.suspensionState !== "Suspended" /* SuspensionState.Suspended */ || Boolean(this.performanceTraceRecording), 'CoverageModel was suspended while polling.');
        if (updates.length) {
            this.dispatchEventToListeners(Events.CoverageUpdated, updates);
        }
    }
    clearTimer() {
        if (this.pollTimer) {
            clearTimeout(this.pollTimer);
            this.pollTimer = null;
        }
    }
    /**
     * Stops polling as preparation for suspension. This function is idempotent
     * due because it changes the state to suspending.
     */
    async preSuspendModel(reason) {
        if (this.suspensionState !== "Active" /* SuspensionState.Active */) {
            return;
        }
        this.suspensionState = "Suspending" /* SuspensionState.Suspending */;
        if (reason === 'performance-timeline') {
            this.performanceTraceRecording = true;
            // Keep polling to the backlog if a performance trace is recorded.
            return;
        }
        if (this.currentPollPromise) {
            await this.stopPolling();
            this.shouldResumePollingOnResume = true;
        }
    }
    async suspendModel(_reason) {
        this.suspensionState = "Suspended" /* SuspensionState.Suspended */;
    }
    async resumeModel() {
    }
    /**
     * Restarts polling after suspension. Note that the function is idempotent
     * because starting polling is idempotent.
     */
    async postResumeModel() {
        this.suspensionState = "Active" /* SuspensionState.Active */;
        this.performanceTraceRecording = false;
        if (this.shouldResumePollingOnResume) {
            this.shouldResumePollingOnResume = false;
            await this.startPolling();
        }
    }
    entries() {
        return Array.from(this.coverageByURL.values());
    }
    getCoverageForUrl(url) {
        return this.coverageByURL.get(url) || null;
    }
    usageForRange(contentProvider, startOffset, endOffset) {
        const coverageInfo = this.coverageByContentProvider.get(contentProvider);
        return coverageInfo && coverageInfo.usageForRange(startOffset, endOffset);
    }
    clearCSS() {
        for (const entry of this.coverageByContentProvider.values()) {
            if (entry.type() !== 1 /* CoverageType.CSS */) {
                continue;
            }
            const contentProvider = entry.getContentProvider();
            this.coverageByContentProvider.delete(contentProvider);
            const urlEntry = this.coverageByURL.get(entry.url());
            if (!urlEntry) {
                continue;
            }
            const key = `${contentProvider.startLine}:${contentProvider.startColumn}`;
            urlEntry.removeCoverageEntry(key, entry);
            if (urlEntry.numberOfEntries() === 0) {
                this.coverageByURL.delete(entry.url());
            }
        }
        if (this.cssModel) {
            for (const styleSheetHeader of this.cssModel.getAllStyleSheetHeaders()) {
                this.addStyleSheetToCSSCoverage(styleSheetHeader);
            }
        }
    }
    async takeAllCoverage() {
        const [updatesCSS, updatesJS] = await Promise.all([this.takeCSSCoverage(), this.takeJSCoverage()]);
        return [...updatesCSS, ...updatesJS];
    }
    async takeJSCoverage() {
        if (!this.cpuProfilerModel) {
            return [];
        }
        const { coverage, timestamp } = await this.cpuProfilerModel.takePreciseCoverage();
        this.coverageUpdateTimes.add(timestamp);
        return this.backlogOrProcessJSCoverage(coverage, timestamp);
    }
    getCoverageUpdateTimes() {
        return this.coverageUpdateTimes;
    }
    async backlogOrProcessJSCoverage(freshRawCoverageData, freshTimestamp) {
        if (freshRawCoverageData.length > 0) {
            this.jsBacklog.push({ rawCoverageData: freshRawCoverageData, stamp: freshTimestamp });
        }
        if (this.suspensionState !== "Active" /* SuspensionState.Active */) {
            return [];
        }
        const ascendingByTimestamp = (x, y) => x.stamp - y.stamp;
        const results = [];
        for (const { rawCoverageData, stamp } of this.jsBacklog.sort(ascendingByTimestamp)) {
            results.push(this.processJSCoverage(rawCoverageData, stamp));
        }
        this.jsBacklog = [];
        return results.flat();
    }
    async processJSBacklog() {
        void this.backlogOrProcessJSCoverage([], 0);
    }
    processJSCoverage(scriptsCoverage, stamp) {
        if (!this.debuggerModel) {
            return [];
        }
        const updatedEntries = [];
        for (const entry of scriptsCoverage) {
            const script = this.debuggerModel.scriptForId(entry.scriptId);
            if (!script) {
                continue;
            }
            const ranges = [];
            let type = 2 /* CoverageType.JavaScript */;
            for (const func of entry.functions) {
                // Do not coerce undefined to false, i.e. only consider blockLevel to be false
                // if back-end explicitly provides blockLevel field, otherwise presume blockLevel
                // coverage is not available. Also, ignore non-block level functions that weren't
                // ever called.
                if (func.isBlockCoverage === false && !(func.ranges.length === 1 && !func.ranges[0].count)) {
                    type |= 4 /* CoverageType.JavaScriptPerFunction */;
                }
                for (const range of func.ranges) {
                    ranges.push(range);
                }
            }
            const subentry = this.addCoverage(script, script.contentLength, script.lineOffset, script.columnOffset, ranges, type, stamp);
            if (subentry) {
                updatedEntries.push(subentry);
            }
        }
        return updatedEntries;
    }
    handleStyleSheetAdded(event) {
        this.addStyleSheetToCSSCoverage(event.data);
    }
    async takeCSSCoverage() {
        // Don't poll if we have no model, or are suspended.
        if (!this.cssModel || this.suspensionState !== "Active" /* SuspensionState.Active */) {
            return [];
        }
        const { coverage, timestamp } = await this.cssModel.takeCoverageDelta();
        this.coverageUpdateTimes.add(timestamp);
        return this.backlogOrProcessCSSCoverage(coverage, timestamp);
    }
    async backlogOrProcessCSSCoverage(freshRawCoverageData, freshTimestamp) {
        if (freshRawCoverageData.length > 0) {
            this.cssBacklog.push({ rawCoverageData: freshRawCoverageData, stamp: freshTimestamp });
        }
        if (this.suspensionState !== "Active" /* SuspensionState.Active */) {
            return [];
        }
        const ascendingByTimestamp = (x, y) => x.stamp - y.stamp;
        const results = [];
        for (const { rawCoverageData, stamp } of this.cssBacklog.sort(ascendingByTimestamp)) {
            results.push(this.processCSSCoverage(rawCoverageData, stamp));
        }
        this.cssBacklog = [];
        return results.flat();
    }
    processCSSCoverage(ruleUsageList, stamp) {
        if (!this.cssModel) {
            return [];
        }
        const updatedEntries = [];
        const rulesByStyleSheet = new Map();
        for (const rule of ruleUsageList) {
            const styleSheetHeader = this.cssModel.styleSheetHeaderForId(rule.styleSheetId);
            if (!styleSheetHeader) {
                continue;
            }
            let ranges = rulesByStyleSheet.get(styleSheetHeader);
            if (!ranges) {
                ranges = [];
                rulesByStyleSheet.set(styleSheetHeader, ranges);
            }
            ranges.push({ startOffset: rule.startOffset, endOffset: rule.endOffset, count: Number(rule.used) });
        }
        for (const entry of rulesByStyleSheet) {
            const styleSheetHeader = entry[0];
            const ranges = entry[1];
            const subentry = this.addCoverage(styleSheetHeader, styleSheetHeader.contentLength, styleSheetHeader.startLine, styleSheetHeader.startColumn, ranges, 1 /* CoverageType.CSS */, stamp);
            if (subentry) {
                updatedEntries.push(subentry);
            }
        }
        return updatedEntries;
    }
    static convertToDisjointSegments(ranges, stamp) {
        ranges.sort((a, b) => a.startOffset - b.startOffset);
        const result = [];
        const stack = [];
        for (const entry of ranges) {
            let top = stack[stack.length - 1];
            while (top && top.endOffset <= entry.startOffset) {
                append(top.endOffset, top.count);
                stack.pop();
                top = stack[stack.length - 1];
            }
            append(entry.startOffset, top ? top.count : 0);
            stack.push(entry);
        }
        for (let top = stack.pop(); top; top = stack.pop()) {
            append(top.endOffset, top.count);
        }
        function append(end, count) {
            const last = result[result.length - 1];
            if (last) {
                if (last.end === end) {
                    return;
                }
                if (last.count === count) {
                    last.end = end;
                    return;
                }
            }
            result.push({ end: end, count: count, stamp: stamp });
        }
        return result;
    }
    addStyleSheetToCSSCoverage(styleSheetHeader) {
        this.addCoverage(styleSheetHeader, styleSheetHeader.contentLength, styleSheetHeader.startLine, styleSheetHeader.startColumn, [], 1 /* CoverageType.CSS */, Date.now());
    }
    addCoverage(contentProvider, contentLength, startLine, startColumn, ranges, type, stamp) {
        const url = contentProvider.contentURL();
        if (!url) {
            return null;
        }
        let urlCoverage = this.coverageByURL.get(url);
        let isNewUrlCoverage = false;
        if (!urlCoverage) {
            isNewUrlCoverage = true;
            urlCoverage = new URLCoverageInfo(url);
            this.coverageByURL.set(url, urlCoverage);
        }
        const coverageInfo = urlCoverage.ensureEntry(contentProvider, contentLength, startLine, startColumn, type);
        this.coverageByContentProvider.set(contentProvider, coverageInfo);
        const segments = CoverageModel.convertToDisjointSegments(ranges, stamp);
        const last = segments[segments.length - 1];
        if (last && last.end < contentLength) {
            segments.push({ end: contentLength, stamp: stamp, count: 0 });
        }
        const usedSizeDelta = coverageInfo.mergeCoverage(segments);
        if (!isNewUrlCoverage && usedSizeDelta === 0) {
            return null;
        }
        urlCoverage.addToSizes(usedSizeDelta, 0);
        return coverageInfo;
    }
    async exportReport(fos) {
        const result = [];
        const coverageByUrlKeys = Array.from(this.coverageByURL.keys()).sort();
        for (const urlInfoKey of coverageByUrlKeys) {
            const urlInfo = this.coverageByURL.get(urlInfoKey);
            if (!urlInfo) {
                continue;
            }
            const url = urlInfo.url();
            if (url.startsWith('extensions::') || url.startsWith('chrome-extension://')) {
                continue;
            }
            result.push(...await urlInfo.entriesForExport());
        }
        await fos.write(JSON.stringify(result, undefined, 2));
        void fos.close();
    }
}
SDK.SDKModel.SDKModel.register(CoverageModel, { capabilities: SDK.Target.Capability.None, autostart: false });
function locationCompare(a, b) {
    const [aLine, aPos] = a.split(':');
    const [bLine, bPos] = b.split(':');
    return Number.parseInt(aLine, 10) - Number.parseInt(bLine, 10) ||
        Number.parseInt(aPos, 10) - Number.parseInt(bPos, 10);
}
export class URLCoverageInfo extends Common.ObjectWrapper.ObjectWrapper {
    urlInternal;
    coverageInfoByLocation;
    sizeInternal;
    usedSizeInternal;
    typeInternal;
    isContentScriptInternal;
    constructor(url) {
        super();
        this.urlInternal = url;
        this.coverageInfoByLocation = new Map();
        this.sizeInternal = 0;
        this.usedSizeInternal = 0;
        this.isContentScriptInternal = false;
    }
    url() {
        return this.urlInternal;
    }
    type() {
        return this.typeInternal;
    }
    size() {
        return this.sizeInternal;
    }
    usedSize() {
        return this.usedSizeInternal;
    }
    unusedSize() {
        return this.sizeInternal - this.usedSizeInternal;
    }
    usedPercentage() {
        // Per convention, empty files are reported as 100 % uncovered
        if (this.sizeInternal === 0) {
            return 0;
        }
        return this.usedSize() / this.size();
    }
    unusedPercentage() {
        // Per convention, empty files are reported as 100 % uncovered
        if (this.sizeInternal === 0) {
            return 100;
        }
        return this.unusedSize() / this.size();
    }
    isContentScript() {
        return this.isContentScriptInternal;
    }
    entries() {
        return this.coverageInfoByLocation.values();
    }
    numberOfEntries() {
        return this.coverageInfoByLocation.size;
    }
    removeCoverageEntry(key, entry) {
        if (!this.coverageInfoByLocation.delete(key)) {
            return;
        }
        this.addToSizes(-entry.getUsedSize(), -entry.getSize());
    }
    addToSizes(usedSize, size) {
        this.usedSizeInternal += usedSize;
        this.sizeInternal += size;
        if (usedSize !== 0 || size !== 0) {
            this.dispatchEventToListeners(URLCoverageInfo.Events.SizesChanged);
        }
    }
    ensureEntry(contentProvider, contentLength, lineOffset, columnOffset, type) {
        const key = `${lineOffset}:${columnOffset}`;
        let entry = this.coverageInfoByLocation.get(key);
        if ((type & 2 /* CoverageType.JavaScript */) && !this.coverageInfoByLocation.size) {
            this.isContentScriptInternal = contentProvider.isContentScript();
        }
        this.typeInternal |= type;
        if (entry) {
            entry.addCoverageType(type);
            return entry;
        }
        if ((type & 2 /* CoverageType.JavaScript */) && !this.coverageInfoByLocation.size) {
            this.isContentScriptInternal = contentProvider.isContentScript();
        }
        entry = new CoverageInfo(contentProvider, contentLength, lineOffset, columnOffset, type);
        this.coverageInfoByLocation.set(key, entry);
        this.addToSizes(0, contentLength);
        return entry;
    }
    async getFullText() {
        // For .html resources, multiple scripts share URL, but have different offsets.
        let useFullText = false;
        const url = this.url();
        for (const info of this.coverageInfoByLocation.values()) {
            const { lineOffset, columnOffset } = info.getOffsets();
            if (lineOffset || columnOffset) {
                useFullText = Boolean(url);
                break;
            }
        }
        if (!useFullText) {
            return null;
        }
        const resource = SDK.ResourceTreeModel.ResourceTreeModel.resourceForURL(url);
        if (!resource) {
            return null;
        }
        const content = (await resource.requestContent()).content;
        return new TextUtils.Text.Text(content || '');
    }
    entriesForExportBasedOnFullText(fullText) {
        const coverageByLocationKeys = Array.from(this.coverageInfoByLocation.keys()).sort(locationCompare);
        const entry = { url: this.url(), ranges: [], text: fullText.value() };
        for (const infoKey of coverageByLocationKeys) {
            const info = this.coverageInfoByLocation.get(infoKey);
            if (!info) {
                continue;
            }
            const { lineOffset, columnOffset } = info.getOffsets();
            const offset = fullText ? fullText.offsetFromPosition(lineOffset, columnOffset) : 0;
            entry.ranges.push(...info.rangesForExport(offset));
        }
        return entry;
    }
    async entriesForExportBasedOnContent() {
        const coverageByLocationKeys = Array.from(this.coverageInfoByLocation.keys()).sort(locationCompare);
        const result = [];
        for (const infoKey of coverageByLocationKeys) {
            const info = this.coverageInfoByLocation.get(infoKey);
            if (!info) {
                continue;
            }
            const entry = {
                url: this.url(),
                ranges: info.rangesForExport(),
                text: (await info.getContentProvider().requestContent()).content,
            };
            result.push(entry);
        }
        return result;
    }
    async entriesForExport() {
        const fullText = await this.getFullText();
        // We have full text for this resource, resolve the offsets using the text line endings.
        if (fullText) {
            return [await this.entriesForExportBasedOnFullText(fullText)];
        }
        // Fall back to the per-script operation.
        return this.entriesForExportBasedOnContent();
    }
}
(function (URLCoverageInfo) {
    // TODO(crbug.com/1167717): Make this a const enum again
    // eslint-disable-next-line rulesdir/const_enum
    let Events;
    (function (Events) {
        Events["SizesChanged"] = "SizesChanged";
    })(Events = URLCoverageInfo.Events || (URLCoverageInfo.Events = {}));
})(URLCoverageInfo || (URLCoverageInfo = {}));
export const mergeSegments = (segmentsA, segmentsB) => {
    const result = [];
    let indexA = 0;
    let indexB = 0;
    while (indexA < segmentsA.length && indexB < segmentsB.length) {
        const a = segmentsA[indexA];
        const b = segmentsB[indexB];
        const count = (a.count || 0) + (b.count || 0);
        const end = Math.min(a.end, b.end);
        const last = result[result.length - 1];
        const stamp = Math.min(a.stamp, b.stamp);
        if (!last || last.count !== count || last.stamp !== stamp) {
            result.push({ end: end, count: count, stamp: stamp });
        }
        else {
            last.end = end;
        }
        if (a.end <= b.end) {
            indexA++;
        }
        if (a.end >= b.end) {
            indexB++;
        }
    }
    for (; indexA < segmentsA.length; indexA++) {
        result.push(segmentsA[indexA]);
    }
    for (; indexB < segmentsB.length; indexB++) {
        result.push(segmentsB[indexB]);
    }
    return result;
};
export class CoverageInfo {
    contentProvider;
    size;
    usedSize;
    statsByTimestamp;
    lineOffset;
    columnOffset;
    coverageType;
    segments;
    constructor(contentProvider, size, lineOffset, columnOffset, type) {
        this.contentProvider = contentProvider;
        this.size = size;
        this.usedSize = 0;
        this.statsByTimestamp = new Map();
        this.lineOffset = lineOffset;
        this.columnOffset = columnOffset;
        this.coverageType = type;
        this.segments = [];
    }
    getContentProvider() {
        return this.contentProvider;
    }
    url() {
        return this.contentProvider.contentURL();
    }
    type() {
        return this.coverageType;
    }
    addCoverageType(type) {
        this.coverageType |= type;
    }
    getOffsets() {
        return { lineOffset: this.lineOffset, columnOffset: this.columnOffset };
    }
    /**
     * Returns the delta by which usedSize increased.
     */
    mergeCoverage(segments) {
        const oldUsedSize = this.usedSize;
        this.segments = mergeSegments(this.segments, segments);
        this.updateStats();
        return this.usedSize - oldUsedSize;
    }
    usedByTimestamp() {
        return this.statsByTimestamp;
    }
    getSize() {
        return this.size;
    }
    getUsedSize() {
        return this.usedSize;
    }
    usageForRange(start, end) {
        let index = Platform.ArrayUtilities.upperBound(this.segments, start, (position, segment) => position - segment.end);
        for (; index < this.segments.length && this.segments[index].end < end; ++index) {
            if (this.segments[index].count) {
                return true;
            }
        }
        return index < this.segments.length && Boolean(this.segments[index].count);
    }
    updateStats() {
        this.statsByTimestamp = new Map();
        this.usedSize = 0;
        let last = 0;
        for (const segment of this.segments) {
            let previousCount = this.statsByTimestamp.get(segment.stamp);
            if (previousCount === undefined) {
                previousCount = 0;
            }
            if (segment.count) {
                const used = segment.end - last;
                this.usedSize += used;
                this.statsByTimestamp.set(segment.stamp, previousCount + used);
            }
            last = segment.end;
        }
    }
    rangesForExport(offset = 0) {
        const ranges = [];
        let start = 0;
        for (const segment of this.segments) {
            if (segment.count) {
                const last = ranges.length > 0 ? ranges[ranges.length - 1] : null;
                if (last && last.end === start + offset) {
                    // We can extend the last segment.
                    last.end = segment.end + offset;
                }
                else {
                    // There was a gap, add a new segment.
                    ranges.push({ start: start + offset, end: segment.end + offset });
                }
            }
            start = segment.end;
        }
        return ranges;
    }
}
//# sourceMappingURL=CoverageModel.js.map