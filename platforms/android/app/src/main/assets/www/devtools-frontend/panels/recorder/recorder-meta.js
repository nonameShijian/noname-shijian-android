// Copyright 2023 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as i18n from '../../core/i18n/i18n.js';
import * as UI from '../../ui/legacy/legacy.js';
const UIStrings = {
    /**
     *@description Title of the Recorder Panel
     */
    recorder: 'Recorder',
    /**
     *@description Title of the Recorder Panel
     */
    showRecorder: 'Show Recorder',
    /**
     *@description Title of start/stop recording action in command menu
     */
    startStopRecording: 'Start/Stop recording',
    /**
     *@description Title of create a new recording action in command menu
     */
    createRecording: 'Create a new recording',
    /**
     *@description Title of start a new recording action in command menu
     */
    replayRecording: 'Replay recording',
    /**
     * @description Title for toggling code action in command menu
     */
    toggleCode: 'Toggle code view',
};
// TODO: (crbug.com/1181019)
const RecorderCategory = 'Recorder';
const str_ = i18n.i18n.registerUIStrings('panels/recorder/recorder-meta.ts', UIStrings);
const i18nLazyString = i18n.i18n.getLazilyComputedLocalizedString.bind(undefined, str_);
let loadedRecorderModule;
async function loadRecorderModule() {
    if (!loadedRecorderModule) {
        loadedRecorderModule = await import('./recorder.js');
    }
    return loadedRecorderModule;
}
function maybeRetrieveContextTypes(getClassCallBack, actionId) {
    if (loadedRecorderModule === undefined) {
        return [];
    }
    if (actionId &&
        loadedRecorderModule.RecorderPanel.RecorderPanel.instance().isActionPossible(actionId)) {
        return getClassCallBack(loadedRecorderModule);
    }
    return [];
}
UI.ViewManager.defaultOptionsForTabs.chrome_recorder = true;
UI.ViewManager.registerViewExtension({
    location: "panel" /* UI.ViewManager.ViewLocationValues.PANEL */,
    id: 'chrome_recorder',
    commandPrompt: i18nLazyString(UIStrings.showRecorder),
    title: i18nLazyString(UIStrings.recorder),
    order: 90,
    persistence: "closeable" /* UI.ViewManager.ViewPersistence.CLOSEABLE */,
    isPreviewFeature: true,
    async loadView() {
        const Recorder = await loadRecorderModule();
        return Recorder.RecorderPanel.RecorderPanel.instance();
    },
});
UI.ActionRegistration.registerActionExtension({
    category: RecorderCategory,
    actionId: "chrome_recorder.create-recording" /* Actions.RecorderActions.CreateRecording */,
    title: i18nLazyString(UIStrings.createRecording),
    async loadActionDelegate() {
        const Recorder = await loadRecorderModule();
        return Recorder.RecorderPanel.ActionDelegate.instance();
    },
});
UI.ActionRegistration.registerActionExtension({
    category: RecorderCategory,
    actionId: "chrome_recorder.start-recording" /* Actions.RecorderActions.StartRecording */,
    title: i18nLazyString(UIStrings.startStopRecording),
    contextTypes() {
        return maybeRetrieveContextTypes(Recorder => [Recorder.RecorderPanel.RecorderPanel], "chrome_recorder.start-recording" /* Actions.RecorderActions.StartRecording */);
    },
    async loadActionDelegate() {
        const Recorder = await loadRecorderModule();
        return Recorder.RecorderPanel.ActionDelegate.instance();
    },
    bindings: [
        {
            shortcut: 'Ctrl+E',
            platform: "windows,linux" /* UI.ActionRegistration.Platforms.WindowsLinux */,
        },
        { shortcut: 'Meta+E', platform: "mac" /* UI.ActionRegistration.Platforms.Mac */ },
    ],
});
UI.ActionRegistration.registerActionExtension({
    category: RecorderCategory,
    actionId: "chrome_recorder.replay-recording" /* Actions.RecorderActions.ReplayRecording */,
    title: i18nLazyString(UIStrings.replayRecording),
    contextTypes() {
        return maybeRetrieveContextTypes(Recorder => [Recorder.RecorderPanel.RecorderPanel], "chrome_recorder.replay-recording" /* Actions.RecorderActions.ReplayRecording */);
    },
    async loadActionDelegate() {
        const Recorder = await loadRecorderModule();
        return Recorder.RecorderPanel.ActionDelegate.instance();
    },
    bindings: [
        {
            shortcut: 'Ctrl+Enter',
            platform: "windows,linux" /* UI.ActionRegistration.Platforms.WindowsLinux */,
        },
        { shortcut: 'Meta+Enter', platform: "mac" /* UI.ActionRegistration.Platforms.Mac */ },
    ],
});
UI.ActionRegistration.registerActionExtension({
    category: RecorderCategory,
    actionId: "chrome_recorder.toggle-code-view" /* Actions.RecorderActions.ToggleCodeView */,
    title: i18nLazyString(UIStrings.toggleCode),
    contextTypes() {
        return maybeRetrieveContextTypes(Recorder => [Recorder.RecorderPanel.RecorderPanel], "chrome_recorder.toggle-code-view" /* Actions.RecorderActions.ToggleCodeView */);
    },
    async loadActionDelegate() {
        const Recorder = await loadRecorderModule();
        return Recorder.RecorderPanel.ActionDelegate.instance();
    },
    bindings: [
        {
            shortcut: 'Ctrl+B',
            platform: "windows,linux" /* UI.ActionRegistration.Platforms.WindowsLinux */,
        },
        { shortcut: 'Meta+B', platform: "mac" /* UI.ActionRegistration.Platforms.Mac */ },
    ],
});
//# sourceMappingURL=recorder-meta.js.map