// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as i18n from '../../core/i18n/i18n.js';
import * as SDK from '../../core/sdk/sdk.js';
import * as TimelineModel from '../../models/timeline_model/timeline_model.js';
import * as Components from '../../ui/legacy/components/utils/utils.js';
import * as UI from '../../ui/legacy/legacy.js';
import { EventsTimelineTreeView } from './EventsTimelineTreeView.js';
import { Events } from './PerformanceModel.js';
import { TimelineLayersView } from './TimelineLayersView.js';
import { TimelinePaintProfilerView } from './TimelinePaintProfilerView.js';
import { TimelineSelection } from './TimelineSelection.js';
import { BottomUpTimelineTreeView, CallTreeTimelineTreeView } from './TimelineTreeView.js';
import { TimelineDetailsContentHelper, TimelineUIUtils } from './TimelineUIUtils.js';
const UIStrings = {
    /**
     *@description Text for the summary view
     */
    summary: 'Summary',
    /**
     *@description Text in Timeline Details View of the Performance panel
     */
    bottomup: 'Bottom-Up',
    /**
     *@description Text in Timeline Details View of the Performance panel
     */
    callTree: 'Call Tree',
    /**
     *@description Text in Timeline Details View of the Performance panel
     */
    eventLog: 'Event Log',
    /**
     *@description The label for estimated total blocking time in the performance panel
     */
    estimated: 'estimated',
    /**
     *@description Label for the total blocking time in the Performance Panel
     *@example {320.23} PH1
     *@example {(estimated)} PH2
     */
    totalBlockingTimeSmss: 'Total blocking time: {PH1}ms{PH2}',
    /**
     *@description Text that is usually a hyperlink to more documentation
     */
    learnMore: 'Learn more',
    /**
     *@description Title of the Layers tool
     */
    layers: 'Layers',
    /**
     *@description Title of the paint profiler, old name of the performance pane
     */
    paintProfiler: 'Paint Profiler',
    /**
     *@description Text in Timeline Details View of the Performance panel
     *@example {1ms} PH1
     *@example {10ms} PH2
     */
    rangeSS: 'Range:  {PH1} – {PH2}',
};
const str_ = i18n.i18n.registerUIStrings('panels/timeline/TimelineDetailsView.ts', UIStrings);
const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
export class TimelineDetailsView extends UI.Widget.VBox {
    detailsLinkifier;
    tabbedPane;
    defaultDetailsWidget;
    defaultDetailsContentElement;
    rangeDetailViews;
    additionalMetricsToolbar;
    model;
    #selectedEvents;
    lazyPaintProfilerView;
    lazyLayersView;
    preferredTabId;
    selection;
    #traceEngineData = null;
    #filmStripModel = null;
    constructor(delegate) {
        super();
        this.element.classList.add('timeline-details');
        this.detailsLinkifier = new Components.Linkifier.Linkifier();
        this.tabbedPane = new UI.TabbedPane.TabbedPane();
        this.tabbedPane.show(this.element);
        this.defaultDetailsWidget = new UI.Widget.VBox();
        this.defaultDetailsWidget.element.classList.add('timeline-details-view');
        this.defaultDetailsContentElement =
            this.defaultDetailsWidget.element.createChild('div', 'timeline-details-view-body vbox');
        this.appendTab(Tab.Details, i18nString(UIStrings.summary), this.defaultDetailsWidget);
        this.setPreferredTab(Tab.Details);
        this.rangeDetailViews = new Map();
        const bottomUpView = new BottomUpTimelineTreeView();
        this.appendTab(Tab.BottomUp, i18nString(UIStrings.bottomup), bottomUpView);
        this.rangeDetailViews.set(Tab.BottomUp, bottomUpView);
        const callTreeView = new CallTreeTimelineTreeView();
        this.appendTab(Tab.CallTree, i18nString(UIStrings.callTree), callTreeView);
        this.rangeDetailViews.set(Tab.CallTree, callTreeView);
        const eventsView = new EventsTimelineTreeView(delegate);
        this.appendTab(Tab.EventLog, i18nString(UIStrings.eventLog), eventsView);
        this.rangeDetailViews.set(Tab.EventLog, eventsView);
        this.additionalMetricsToolbar = new UI.Toolbar.Toolbar('timeline-additional-metrics');
        this.element.appendChild(this.additionalMetricsToolbar.element);
        this.tabbedPane.addEventListener(UI.TabbedPane.Events.TabSelected, this.tabSelected, this);
    }
    setModel(model, traceEngineData, filmStripModel, selectedEvents) {
        if (this.model !== model) {
            if (this.model) {
                this.model.removeEventListener(Events.WindowChanged, this.onWindowChanged, this);
            }
            this.model = model;
            if (this.model) {
                this.model.addEventListener(Events.WindowChanged, this.onWindowChanged, this);
            }
        }
        this.#traceEngineData = traceEngineData;
        this.#selectedEvents = selectedEvents;
        this.#filmStripModel = filmStripModel;
        this.tabbedPane.closeTabs([Tab.PaintProfiler, Tab.LayerViewer], false);
        for (const view of this.rangeDetailViews.values()) {
            view.setModelWithEvents(model, selectedEvents, traceEngineData);
        }
        this.lazyPaintProfilerView = null;
        this.lazyLayersView = null;
        this.setSelection(null);
        // Add TBT info to the footer.
        this.additionalMetricsToolbar.removeToolbarItems();
        if (model && model.timelineModel()) {
            const { estimated, time } = model.timelineModel().totalBlockingTime();
            const isEstimate = estimated ? ` (${i18nString(UIStrings.estimated)})` : '';
            const message = i18nString(UIStrings.totalBlockingTimeSmss, { PH1: time.toFixed(2), PH2: isEstimate });
            const warning = document.createElement('span');
            const clsLink = UI.XLink.XLink.create('https://web.dev/tbt/', i18nString(UIStrings.learnMore));
            // Prevent focus ring from being cut off.
            clsLink.style.margin = '3px';
            warning.appendChild(clsLink);
            this.additionalMetricsToolbar.appendText(message);
            this.additionalMetricsToolbar.appendToolbarItem(new UI.Toolbar.ToolbarItem(warning));
        }
    }
    setContent(node) {
        const allTabs = this.tabbedPane.otherTabs(Tab.Details);
        for (let i = 0; i < allTabs.length; ++i) {
            if (!this.rangeDetailViews.has(allTabs[i])) {
                this.tabbedPane.closeTab(allTabs[i]);
            }
        }
        this.defaultDetailsContentElement.removeChildren();
        this.defaultDetailsContentElement.appendChild(node);
    }
    updateContents() {
        const view = this.rangeDetailViews.get(this.tabbedPane.selectedTabId || '');
        if (view) {
            const window = this.model.window();
            view.updateContents(this.selection || TimelineSelection.fromRange(window.left, window.right));
        }
    }
    appendTab(id, tabTitle, view, isCloseable) {
        this.tabbedPane.appendTab(id, tabTitle, view, undefined, undefined, isCloseable);
        if (this.preferredTabId !== this.tabbedPane.selectedTabId) {
            this.tabbedPane.selectTab(id);
        }
    }
    headerElement() {
        return this.tabbedPane.headerElement();
    }
    setPreferredTab(tabId) {
        this.preferredTabId = tabId;
    }
    onWindowChanged() {
        if (!this.selection) {
            this.updateContentsFromWindow();
        }
    }
    updateContentsFromWindow() {
        if (!this.model) {
            this.setContent(UI.Fragment.html `<div/>`);
            return;
        }
        const window = this.model.window();
        this.updateSelectedRangeStats(window.left, window.right);
        this.updateContents();
    }
    #getFilmStripFrame(frame) {
        if (!this.#filmStripModel) {
            return null;
        }
        // For idle frames, look at the state at the beginning of the frame.
        const screenshotTime = frame.idle ? frame.startTime : frame.endTime;
        const filmStripFrame = this.#filmStripModel.frameByTimestamp(screenshotTime);
        return filmStripFrame && filmStripFrame.timestamp - frame.endTime < 10 ? filmStripFrame : null;
    }
    setSelection(selection) {
        this.detailsLinkifier.reset();
        this.selection = selection;
        if (!this.selection) {
            this.updateContentsFromWindow();
            return;
        }
        const selectionObject = this.selection.object;
        if (TimelineSelection.isTraceEventSelection(selectionObject)) {
            const event = selectionObject;
            void TimelineUIUtils
                .buildTraceEventDetails(event, this.model.timelineModel(), this.detailsLinkifier, true, this.#traceEngineData)
                .then(fragment => this.appendDetailsTabsForTraceEventAndShowDetails(event, fragment));
        }
        else if (TimelineSelection.isFrameObject(selectionObject)) {
            const frame = selectionObject;
            const filmStripFrame = this.#getFilmStripFrame(frame);
            this.setContent(TimelineUIUtils.generateDetailsContentForFrame(frame, filmStripFrame));
            if (frame.layerTree) {
                const layersView = this.layersView();
                layersView.showLayerTree(frame.layerTree);
                if (!this.tabbedPane.hasTab(Tab.LayerViewer)) {
                    this.appendTab(Tab.LayerViewer, i18nString(UIStrings.layers), layersView);
                }
            }
        }
        else if (TimelineSelection.isNetworkRequestSelection(selectionObject)) {
            const request = selectionObject;
            void TimelineUIUtils.buildNetworkRequestDetails(request, this.model.timelineModel(), this.detailsLinkifier)
                .then(this.setContent.bind(this));
        }
        else if (TimelineSelection.isRangeSelection(selectionObject)) {
            this.updateSelectedRangeStats(this.selection.startTime, this.selection.endTime);
        }
        this.updateContents();
    }
    tabSelected(event) {
        if (!event.data.isUserGesture) {
            return;
        }
        this.setPreferredTab(event.data.tabId);
        this.updateContents();
    }
    layersView() {
        if (this.lazyLayersView) {
            return this.lazyLayersView;
        }
        this.lazyLayersView =
            new TimelineLayersView(this.model.timelineModel(), this.showSnapshotInPaintProfiler.bind(this));
        return this.lazyLayersView;
    }
    paintProfilerView() {
        if (this.lazyPaintProfilerView) {
            return this.lazyPaintProfilerView;
        }
        this.lazyPaintProfilerView = new TimelinePaintProfilerView(this.model.frameModel());
        return this.lazyPaintProfilerView;
    }
    showSnapshotInPaintProfiler(snapshot) {
        const paintProfilerView = this.paintProfilerView();
        paintProfilerView.setSnapshot(snapshot);
        if (!this.tabbedPane.hasTab(Tab.PaintProfiler)) {
            this.appendTab(Tab.PaintProfiler, i18nString(UIStrings.paintProfiler), paintProfilerView, true);
        }
        this.tabbedPane.selectTab(Tab.PaintProfiler, true);
    }
    appendDetailsTabsForTraceEventAndShowDetails(event, content) {
        this.setContent(content);
        if (SDK.TracingModel.eventIsFromNewEngine(event)) {
            // TODO(crbug.com/1386091): Add support for this use case in the
            // new engine.
            return;
        }
        if (event.name === TimelineModel.TimelineModel.RecordType.Paint ||
            event.name === TimelineModel.TimelineModel.RecordType.RasterTask) {
            this.showEventInPaintProfiler(event);
        }
    }
    showEventInPaintProfiler(event) {
        const paintProfilerModel = SDK.TargetManager.TargetManager.instance().models(SDK.PaintProfiler.PaintProfilerModel)[0];
        if (!paintProfilerModel) {
            return;
        }
        const paintProfilerView = this.paintProfilerView();
        const hasProfileData = paintProfilerView.setEvent(paintProfilerModel, event);
        if (!hasProfileData) {
            return;
        }
        if (this.tabbedPane.hasTab(Tab.PaintProfiler)) {
            return;
        }
        this.appendTab(Tab.PaintProfiler, i18nString(UIStrings.paintProfiler), paintProfilerView);
    }
    updateSelectedRangeStats(startTime, endTime) {
        if (!this.model || !this.#selectedEvents) {
            return;
        }
        const aggregatedStats = TimelineUIUtils.statsForTimeRange(this.#selectedEvents, startTime, endTime);
        const startOffset = startTime - this.model.timelineModel().minimumRecordTime();
        const endOffset = endTime - this.model.timelineModel().minimumRecordTime();
        const contentHelper = new TimelineDetailsContentHelper(null, null);
        contentHelper.addSection(i18nString(UIStrings.rangeSS, { PH1: i18n.TimeUtilities.millisToString(startOffset), PH2: i18n.TimeUtilities.millisToString(endOffset) }));
        const pieChart = TimelineUIUtils.generatePieChart(aggregatedStats);
        contentHelper.appendElementRow('', pieChart);
        this.setContent(contentHelper.fragment);
    }
}
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var Tab;
(function (Tab) {
    Tab["Details"] = "Details";
    Tab["EventLog"] = "EventLog";
    Tab["CallTree"] = "CallTree";
    Tab["BottomUp"] = "BottomUp";
    Tab["PaintProfiler"] = "PaintProfiler";
    Tab["LayerViewer"] = "LayerViewer";
})(Tab || (Tab = {}));
//# sourceMappingURL=TimelineDetailsView.js.map