/*
 * Copyright (C) 2009 Google Inc. All rights reserved.
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
import * as Root from '../root/root.js';
import { Console } from './Console.js';
import { ObjectWrapper } from './Object.js';
import { getLocalizedSettingsCategory, getRegisteredSettings, maybeRemoveSettingExtension, registerSettingExtension, registerSettingsForTest, resetSettings, SettingCategory, SettingType, } from './SettingRegistration.js';
let settingsInstance;
export class Settings {
    syncedStorage;
    globalStorage;
    localStorage;
    #sessionStorage;
    settingNameSet;
    orderValuesBySettingCategory;
    #eventSupport;
    #registry;
    moduleSettings;
    constructor(syncedStorage, globalStorage, localStorage) {
        this.syncedStorage = syncedStorage;
        this.globalStorage = globalStorage;
        this.localStorage = localStorage;
        this.#sessionStorage = new SettingsStorage({});
        this.settingNameSet = new Set();
        this.orderValuesBySettingCategory = new Map();
        this.#eventSupport = new ObjectWrapper();
        this.#registry = new Map();
        this.moduleSettings = new Map();
        for (const registration of getRegisteredSettings()) {
            const { settingName, defaultValue, storageType } = registration;
            const isRegex = registration.settingType === SettingType.REGEX;
            const setting = isRegex && typeof defaultValue === 'string' ?
                this.createRegExpSetting(settingName, defaultValue, undefined, storageType) :
                this.createSetting(settingName, defaultValue, storageType);
            if (Root.Runtime.Runtime.platform() === 'mac' && registration.titleMac) {
                setting.setTitleFunction(registration.titleMac);
            }
            else {
                setting.setTitleFunction(registration.title);
            }
            if (registration.userActionCondition) {
                setting.setRequiresUserAction(Boolean(Root.Runtime.Runtime.queryParam(registration.userActionCondition)));
            }
            setting.setRegistration(registration);
            this.registerModuleSetting(setting);
        }
    }
    static hasInstance() {
        return typeof settingsInstance !== 'undefined';
    }
    static instance(opts = { forceNew: null, syncedStorage: null, globalStorage: null, localStorage: null }) {
        const { forceNew, syncedStorage, globalStorage, localStorage } = opts;
        if (!settingsInstance || forceNew) {
            if (!syncedStorage || !globalStorage || !localStorage) {
                throw new Error(`Unable to create settings: global and local storage must be provided: ${new Error().stack}`);
            }
            settingsInstance = new Settings(syncedStorage, globalStorage, localStorage);
        }
        return settingsInstance;
    }
    static removeInstance() {
        settingsInstance = undefined;
    }
    registerModuleSetting(setting) {
        const settingName = setting.name;
        const category = setting.category();
        const order = setting.order();
        if (this.settingNameSet.has(settingName)) {
            throw new Error(`Duplicate Setting name '${settingName}'`);
        }
        if (category && order) {
            const orderValues = this.orderValuesBySettingCategory.get(category) || new Set();
            if (orderValues.has(order)) {
                throw new Error(`Duplicate order value '${order}' for settings category '${category}'`);
            }
            orderValues.add(order);
            this.orderValuesBySettingCategory.set(category, orderValues);
        }
        this.settingNameSet.add(settingName);
        this.moduleSettings.set(setting.name, setting);
    }
    // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    moduleSetting(settingName) {
        const setting = this.moduleSettings.get(settingName);
        if (!setting) {
            throw new Error('No setting registered: ' + settingName);
        }
        return setting;
    }
    settingForTest(settingName) {
        const setting = this.#registry.get(settingName);
        if (!setting) {
            throw new Error('No setting registered: ' + settingName);
        }
        return setting;
    }
    createSetting(key, defaultValue, storageType) {
        const storage = this.storageFromType(storageType);
        let setting = this.#registry.get(key);
        if (!setting) {
            setting = new Setting(key, defaultValue, this.#eventSupport, storage);
            this.#registry.set(key, setting);
        }
        return setting;
    }
    createLocalSetting(key, defaultValue) {
        return this.createSetting(key, defaultValue, SettingStorageType.Local);
    }
    createRegExpSetting(key, defaultValue, regexFlags, storageType) {
        if (!this.#registry.get(key)) {
            this.#registry.set(key, new RegExpSetting(key, defaultValue, this.#eventSupport, this.storageFromType(storageType), regexFlags));
        }
        return this.#registry.get(key);
    }
    clearAll() {
        this.globalStorage.removeAll();
        this.syncedStorage.removeAll();
        this.localStorage.removeAll();
        new VersionController().resetToCurrent();
    }
    storageFromType(storageType) {
        switch (storageType) {
            case SettingStorageType.Local:
                return this.localStorage;
            case SettingStorageType.Session:
                return this.#sessionStorage;
            case SettingStorageType.Global:
                return this.globalStorage;
            case SettingStorageType.Synced:
                return this.syncedStorage;
        }
        return this.globalStorage;
    }
    getRegistry() {
        return this.#registry;
    }
}
export const NOOP_STORAGE = {
    register: () => { },
    set: () => { },
    get: () => Promise.resolve(''),
    remove: () => { },
    clear: () => { },
};
export class SettingsStorage {
    object;
    backingStore;
    storagePrefix;
    constructor(object, backingStore = NOOP_STORAGE, storagePrefix = '') {
        this.object = object;
        this.backingStore = backingStore;
        this.storagePrefix = storagePrefix;
    }
    register(name) {
        name = this.storagePrefix + name;
        this.backingStore.register(name);
    }
    set(name, value) {
        name = this.storagePrefix + name;
        this.object[name] = value;
        this.backingStore.set(name, value);
    }
    has(name) {
        name = this.storagePrefix + name;
        return name in this.object;
    }
    get(name) {
        name = this.storagePrefix + name;
        return this.object[name];
    }
    async forceGet(originalName) {
        const name = this.storagePrefix + originalName;
        const value = await this.backingStore.get(name);
        if (value && value !== this.object[name]) {
            this.set(originalName, value);
        }
        else if (!value) {
            this.remove(originalName);
        }
        return value;
    }
    remove(name) {
        name = this.storagePrefix + name;
        delete this.object[name];
        this.backingStore.remove(name);
    }
    removeAll() {
        this.object = {};
        this.backingStore.clear();
    }
    dumpSizes() {
        Console.instance().log('Ten largest settings: ');
        const sizes = { __proto__: null };
        for (const key in this.object) {
            sizes[key] = this.object[key].length;
        }
        const keys = Object.keys(sizes);
        function comparator(key1, key2) {
            return sizes[key2] - sizes[key1];
        }
        keys.sort(comparator);
        for (let i = 0; i < 10 && i < keys.length; ++i) {
            Console.instance().log('Setting: \'' + keys[i] + '\', size: ' + sizes[keys[i]]);
        }
    }
}
function removeSetting(setting) {
    const name = setting.name;
    const settings = Settings.instance();
    settings.getRegistry().delete(name);
    settings.moduleSettings.delete(name);
    setting.storage.remove(name);
}
export class Deprecation {
    disabled;
    warning;
    experiment;
    constructor({ deprecationNotice }) {
        if (!deprecationNotice) {
            throw new Error('Cannot create deprecation info for a non-deprecated setting');
        }
        this.disabled = deprecationNotice.disabled;
        this.warning = deprecationNotice.warning();
        this.experiment = deprecationNotice.experiment ?
            Root.Runtime.experiments.allConfigurableExperiments().find(e => e.name === deprecationNotice.experiment) :
            undefined;
    }
}
export class Setting {
    name;
    defaultValue;
    eventSupport;
    storage;
    #titleFunction;
    #titleInternal;
    #registration = null;
    #requiresUserAction;
    #value;
    // TODO(crbug.com/1172300) Type cannot be inferred without changes to consumers. See above.
    #serializer = JSON;
    #hadUserAction;
    #disabled;
    #deprecation = null;
    constructor(name, defaultValue, eventSupport, storage) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.eventSupport = eventSupport;
        this.storage = storage;
        storage.register(name);
    }
    setSerializer(serializer) {
        this.#serializer = serializer;
    }
    addChangeListener(listener, thisObject) {
        return this.eventSupport.addEventListener(this.name, listener, thisObject);
    }
    removeChangeListener(listener, thisObject) {
        this.eventSupport.removeEventListener(this.name, listener, thisObject);
    }
    title() {
        if (this.#titleInternal) {
            return this.#titleInternal;
        }
        if (this.#titleFunction) {
            return this.#titleFunction();
        }
        return '';
    }
    setTitleFunction(titleFunction) {
        if (titleFunction) {
            this.#titleFunction = titleFunction;
        }
    }
    setTitle(title) {
        this.#titleInternal = title;
    }
    setRequiresUserAction(requiresUserAction) {
        this.#requiresUserAction = requiresUserAction;
    }
    disabled() {
        return this.#disabled || false;
    }
    setDisabled(disabled) {
        this.#disabled = disabled;
        this.eventSupport.dispatchEventToListeners(this.name);
    }
    get() {
        if (this.#requiresUserAction && !this.#hadUserAction) {
            return this.defaultValue;
        }
        if (typeof this.#value !== 'undefined') {
            return this.#value;
        }
        this.#value = this.defaultValue;
        if (this.storage.has(this.name)) {
            try {
                this.#value = this.#serializer.parse(this.storage.get(this.name));
            }
            catch (e) {
                this.storage.remove(this.name);
            }
        }
        return this.#value;
    }
    async forceGet() {
        const name = this.name;
        const oldValue = this.storage.get(name);
        const value = await this.storage.forceGet(name);
        this.#value = this.defaultValue;
        if (value) {
            try {
                this.#value = this.#serializer.parse(value);
            }
            catch (e) {
                this.storage.remove(this.name);
            }
        }
        if (oldValue !== value) {
            this.eventSupport.dispatchEventToListeners(this.name, this.#value);
        }
        return this.#value;
    }
    set(value) {
        this.#hadUserAction = true;
        this.#value = value;
        try {
            const settingString = this.#serializer.stringify(value);
            try {
                this.storage.set(this.name, settingString);
            }
            catch (e) {
                this.printSettingsSavingError(e.message, this.name, settingString);
            }
        }
        catch (e) {
            Console.instance().error('Cannot stringify setting with name: ' + this.name + ', error: ' + e.message);
        }
        this.eventSupport.dispatchEventToListeners(this.name, value);
    }
    setRegistration(registration) {
        this.#registration = registration;
        const { deprecationNotice } = registration;
        if (deprecationNotice?.disabled) {
            const experiment = deprecationNotice.experiment ?
                Root.Runtime.experiments.allConfigurableExperiments().find(e => e.name === deprecationNotice.experiment) :
                undefined;
            if ((!experiment || experiment.isEnabled())) {
                this.set(this.defaultValue);
                this.setDisabled(true);
            }
        }
    }
    type() {
        if (this.#registration) {
            return this.#registration.settingType;
        }
        return null;
    }
    options() {
        if (this.#registration && this.#registration.options) {
            return this.#registration.options.map(opt => {
                const { value, title, text, raw } = opt;
                return {
                    value: value,
                    title: title(),
                    text: typeof text === 'function' ? text() : text,
                    raw: raw,
                };
            });
        }
        return [];
    }
    reloadRequired() {
        if (this.#registration) {
            return this.#registration.reloadRequired || null;
        }
        return null;
    }
    category() {
        if (this.#registration) {
            return this.#registration.category || null;
        }
        return null;
    }
    tags() {
        if (this.#registration && this.#registration.tags) {
            // Get localized keys and separate by null character to prevent fuzzy matching from matching across them.
            return this.#registration.tags.map(tag => tag()).join('\0');
        }
        return null;
    }
    order() {
        if (this.#registration) {
            return this.#registration.order || null;
        }
        return null;
    }
    get deprecation() {
        if (!this.#registration || !this.#registration.deprecationNotice) {
            return null;
        }
        if (!this.#deprecation) {
            this.#deprecation = new Deprecation(this.#registration);
        }
        return this.#deprecation;
    }
    printSettingsSavingError(message, name, value) {
        const errorMessage = 'Error saving setting with name: ' + this.name + ', value length: ' + value.length + '. Error: ' + message;
        console.error(errorMessage);
        Console.instance().error(errorMessage);
        this.storage.dumpSizes();
    }
}
// TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export class RegExpSetting extends Setting {
    #regexFlags;
    #regex;
    constructor(name, defaultValue, eventSupport, storage, regexFlags) {
        super(name, defaultValue ? [{ pattern: defaultValue }] : [], eventSupport, storage);
        this.#regexFlags = regexFlags;
    }
    get() {
        const result = [];
        const items = this.getAsArray();
        for (let i = 0; i < items.length; ++i) {
            const item = items[i];
            if (item.pattern && !item.disabled) {
                result.push(item.pattern);
            }
        }
        return result.join('|');
    }
    getAsArray() {
        return super.get();
    }
    set(value) {
        this.setAsArray([{ pattern: value, disabled: false }]);
    }
    setAsArray(value) {
        this.#regex = undefined;
        super.set(value);
    }
    asRegExp() {
        if (typeof this.#regex !== 'undefined') {
            return this.#regex;
        }
        this.#regex = null;
        try {
            const pattern = this.get();
            if (pattern) {
                this.#regex = new RegExp(pattern, this.#regexFlags || '');
            }
        }
        catch (e) {
        }
        return this.#regex;
    }
}
export class VersionController {
    static GLOBAL_VERSION_SETTING_NAME = 'inspectorVersion';
    static SYNCED_VERSION_SETTING_NAME = 'syncedInspectorVersion';
    static LOCAL_VERSION_SETTING_NAME = 'localInspectorVersion';
    static CURRENT_VERSION = 35;
    #globalVersionSetting;
    #syncedVersionSetting;
    #localVersionSetting;
    constructor() {
        // If no version setting is found, we initialize with the current version and don't do anything.
        this.#globalVersionSetting = Settings.instance().createSetting(VersionController.GLOBAL_VERSION_SETTING_NAME, VersionController.CURRENT_VERSION, SettingStorageType.Global);
        this.#syncedVersionSetting = Settings.instance().createSetting(VersionController.SYNCED_VERSION_SETTING_NAME, VersionController.CURRENT_VERSION, SettingStorageType.Synced);
        this.#localVersionSetting = Settings.instance().createSetting(VersionController.LOCAL_VERSION_SETTING_NAME, VersionController.CURRENT_VERSION, SettingStorageType.Local);
    }
    /**
     * Force re-sets all version number settings to the current version without
     * running any migrations.
     */
    resetToCurrent() {
        this.#globalVersionSetting.set(VersionController.CURRENT_VERSION);
        this.#syncedVersionSetting.set(VersionController.CURRENT_VERSION);
        this.#localVersionSetting.set(VersionController.CURRENT_VERSION);
    }
    /**
     * Runs the appropriate migrations and updates the version settings accordingly.
     *
     * To determine what migrations to run we take the minimum of all version number settings.
     *
     * IMPORTANT: All migrations must be idempotent since they might be applied multiple times.
     */
    updateVersion() {
        const currentVersion = VersionController.CURRENT_VERSION;
        const minimumVersion = Math.min(this.#globalVersionSetting.get(), this.#syncedVersionSetting.get(), this.#localVersionSetting.get());
        const methodsToRun = this.methodsToRunToUpdateVersion(minimumVersion, currentVersion);
        console.assert(
        // @ts-ignore
        this[`updateVersionFrom${currentVersion}To${currentVersion + 1}`] === undefined, 'Unexpected migration method found. Increment CURRENT_VERSION or remove the method.');
        for (const method of methodsToRun) {
            // @ts-ignore Special version method matching
            this[method].call(this);
        }
        this.resetToCurrent();
    }
    methodsToRunToUpdateVersion(oldVersion, currentVersion) {
        const result = [];
        for (let i = oldVersion; i < currentVersion; ++i) {
            result.push('updateVersionFrom' + i + 'To' + (i + 1));
        }
        return result;
    }
    updateVersionFrom0To1() {
        this.clearBreakpointsWhenTooMany(Settings.instance().createLocalSetting('breakpoints', []), 500000);
    }
    updateVersionFrom1To2() {
        Settings.instance().createSetting('previouslyViewedFiles', []).set([]);
    }
    updateVersionFrom2To3() {
        Settings.instance().createSetting('fileSystemMapping', {}).set({});
        removeSetting(Settings.instance().createSetting('fileMappingEntries', []));
    }
    updateVersionFrom3To4() {
        const advancedMode = Settings.instance().createSetting('showHeaSnapshotObjectsHiddenProperties', false);
        moduleSetting('showAdvancedHeapSnapshotProperties').set(advancedMode.get());
        removeSetting(advancedMode);
    }
    updateVersionFrom4To5() {
        const settingNames = {
            'FileSystemViewSidebarWidth': 'fileSystemViewSplitViewState',
            'elementsSidebarWidth': 'elementsPanelSplitViewState',
            'StylesPaneSplitRatio': 'stylesPaneSplitViewState',
            'heapSnapshotRetainersViewSize': 'heapSnapshotSplitViewState',
            'InspectorView.splitView': 'InspectorView.splitViewState',
            'InspectorView.screencastSplitView': 'InspectorView.screencastSplitViewState',
            'Inspector.drawerSplitView': 'Inspector.drawerSplitViewState',
            'layerDetailsSplitView': 'layerDetailsSplitViewState',
            'networkSidebarWidth': 'networkPanelSplitViewState',
            'sourcesSidebarWidth': 'sourcesPanelSplitViewState',
            'scriptsPanelNavigatorSidebarWidth': 'sourcesPanelNavigatorSplitViewState',
            'sourcesPanelSplitSidebarRatio': 'sourcesPanelDebuggerSidebarSplitViewState',
            'timeline-details': 'timelinePanelDetailsSplitViewState',
            'timeline-split': 'timelinePanelRecorsSplitViewState',
            'timeline-view': 'timelinePanelTimelineStackSplitViewState',
            'auditsSidebarWidth': 'auditsPanelSplitViewState',
            'layersSidebarWidth': 'layersPanelSplitViewState',
            'profilesSidebarWidth': 'profilesPanelSplitViewState',
            'resourcesSidebarWidth': 'resourcesPanelSplitViewState',
        };
        const empty = {};
        for (const oldName in settingNames) {
            const newName = settingNames[oldName];
            const oldNameH = oldName + 'H';
            let newValue = null;
            const oldSetting = Settings.instance().createSetting(oldName, empty);
            if (oldSetting.get() !== empty) {
                newValue = newValue || {};
                // @ts-expect-error
                newValue.vertical = {};
                // @ts-expect-error
                newValue.vertical.size = oldSetting.get();
                removeSetting(oldSetting);
            }
            const oldSettingH = Settings.instance().createSetting(oldNameH, empty);
            if (oldSettingH.get() !== empty) {
                newValue = newValue || {};
                // @ts-expect-error
                newValue.horizontal = {};
                // @ts-expect-error
                newValue.horizontal.size = oldSettingH.get();
                removeSetting(oldSettingH);
            }
            if (newValue) {
                Settings.instance().createSetting(newName, {}).set(newValue);
            }
        }
    }
    updateVersionFrom5To6() {
        const settingNames = {
            'debuggerSidebarHidden': 'sourcesPanelSplitViewState',
            'navigatorHidden': 'sourcesPanelNavigatorSplitViewState',
            'WebInspector.Drawer.showOnLoad': 'Inspector.drawerSplitViewState',
        };
        for (const oldName in settingNames) {
            const oldSetting = Settings.instance().createSetting(oldName, null);
            if (oldSetting.get() === null) {
                removeSetting(oldSetting);
                continue;
            }
            const newName = settingNames[oldName];
            const invert = oldName === 'WebInspector.Drawer.showOnLoad';
            const hidden = oldSetting.get() !== invert;
            removeSetting(oldSetting);
            const showMode = hidden ? 'OnlyMain' : 'Both';
            const newSetting = Settings.instance().createSetting(newName, {});
            const newValue = newSetting.get() || {};
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // @ts-expect-error
            newValue.vertical = newValue.vertical || {};
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // @ts-expect-error
            newValue.vertical.showMode = showMode;
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // @ts-expect-error
            newValue.horizontal = newValue.horizontal || {};
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // @ts-expect-error
            newValue.horizontal.showMode = showMode;
            newSetting.set(newValue);
        }
    }
    updateVersionFrom6To7() {
        const settingNames = {
            'sourcesPanelNavigatorSplitViewState': 'sourcesPanelNavigatorSplitViewState',
            'elementsPanelSplitViewState': 'elementsPanelSplitViewState',
            'stylesPaneSplitViewState': 'stylesPaneSplitViewState',
            'sourcesPanelDebuggerSidebarSplitViewState': 'sourcesPanelDebuggerSidebarSplitViewState',
        };
        const empty = {};
        for (const name in settingNames) {
            const setting = Settings.instance().createSetting(name, empty);
            const value = setting.get();
            if (value === empty) {
                continue;
            }
            // Zero out saved percentage sizes, and they will be restored to defaults.
            if (value.vertical && value.vertical.size && value.vertical.size < 1) {
                value.vertical.size = 0;
            }
            if (value.horizontal && value.horizontal.size && value.horizontal.size < 1) {
                value.horizontal.size = 0;
            }
            setting.set(value);
        }
    }
    updateVersionFrom7To8() {
    }
    updateVersionFrom8To9() {
        const settingNames = ['skipStackFramesPattern', 'workspaceFolderExcludePattern'];
        for (let i = 0; i < settingNames.length; ++i) {
            const setting = Settings.instance().createSetting(settingNames[i], '');
            let value = setting.get();
            if (!value) {
                return;
            }
            if (typeof value === 'string') {
                value = [value];
            }
            for (let j = 0; j < value.length; ++j) {
                if (typeof value[j] === 'string') {
                    value[j] = { pattern: value[j] };
                }
            }
            setting.set(value);
        }
    }
    updateVersionFrom9To10() {
        // This one is localStorage specific, which is fine.
        if (!window.localStorage) {
            return;
        }
        for (const key in window.localStorage) {
            if (key.startsWith('revision-history')) {
                window.localStorage.removeItem(key);
            }
        }
    }
    updateVersionFrom10To11() {
        const oldSettingName = 'customDevicePresets';
        const newSettingName = 'customEmulatedDeviceList';
        const oldSetting = Settings.instance().createSetting(oldSettingName, undefined);
        const list = oldSetting.get();
        if (!Array.isArray(list)) {
            return;
        }
        const newList = [];
        for (let i = 0; i < list.length; ++i) {
            const value = list[i];
            const device = {};
            device['title'] = value['title'];
            device['type'] = 'unknown';
            device['user-agent'] = value['userAgent'];
            device['capabilities'] = [];
            if (value['touch']) {
                device['capabilities'].push('touch');
            }
            if (value['mobile']) {
                device['capabilities'].push('mobile');
            }
            device['screen'] = {};
            device['screen']['vertical'] = { width: value['width'], height: value['height'] };
            device['screen']['horizontal'] = { width: value['height'], height: value['width'] };
            device['screen']['device-pixel-ratio'] = value['deviceScaleFactor'];
            device['modes'] = [];
            device['show-by-default'] = true;
            device['show'] = 'Default';
            newList.push(device);
        }
        if (newList.length) {
            Settings.instance().createSetting(newSettingName, []).set(newList);
        }
        removeSetting(oldSetting);
    }
    updateVersionFrom11To12() {
        this.migrateSettingsFromLocalStorage();
    }
    updateVersionFrom12To13() {
        this.migrateSettingsFromLocalStorage();
        removeSetting(Settings.instance().createSetting('timelineOverviewMode', ''));
    }
    updateVersionFrom13To14() {
        const defaultValue = { 'throughput': -1, 'latency': 0 };
        Settings.instance().createSetting('networkConditions', defaultValue).set(defaultValue);
    }
    updateVersionFrom14To15() {
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const setting = Settings.instance().createLocalSetting('workspaceExcludedFolders', {});
        const oldValue = setting.get();
        const newValue = {};
        for (const fileSystemPath in oldValue) {
            newValue[fileSystemPath] = [];
            for (const entry of oldValue[fileSystemPath]) {
                newValue[fileSystemPath].push(entry.path);
            }
        }
        setting.set(newValue);
    }
    updateVersionFrom15To16() {
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const setting = Settings.instance().createSetting('InspectorView.panelOrder', {});
        const tabOrders = setting.get();
        for (const key of Object.keys(tabOrders)) {
            tabOrders[key] = (tabOrders[key] + 1) * 10;
        }
        setting.set(tabOrders);
    }
    updateVersionFrom16To17() {
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const setting = Settings.instance().createSetting('networkConditionsCustomProfiles', []);
        const oldValue = setting.get();
        const newValue = [];
        if (Array.isArray(oldValue)) {
            for (const preset of oldValue) {
                if (typeof preset.title === 'string' && typeof preset.value === 'object' &&
                    typeof preset.value.throughput === 'number' && typeof preset.value.latency === 'number') {
                    newValue.push({
                        title: preset.title,
                        value: { download: preset.value.throughput, upload: preset.value.throughput, latency: preset.value.latency },
                    });
                }
            }
        }
        setting.set(newValue);
    }
    updateVersionFrom17To18() {
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const setting = Settings.instance().createLocalSetting('workspaceExcludedFolders', {});
        const oldValue = setting.get();
        const newValue = {};
        for (const oldKey in oldValue) {
            let newKey = oldKey.replace(/\\/g, '/');
            if (!newKey.startsWith('file://')) {
                if (newKey.startsWith('/')) {
                    newKey = 'file://' + newKey;
                }
                else {
                    newKey = 'file:///' + newKey;
                }
            }
            newValue[newKey] = oldValue[oldKey];
        }
        setting.set(newValue);
    }
    updateVersionFrom18To19() {
        const defaultColumns = { status: true, type: true, initiator: true, size: true, time: true };
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const visibleColumnSettings = Settings.instance().createSetting('networkLogColumnsVisibility', defaultColumns);
        const visibleColumns = visibleColumnSettings.get();
        visibleColumns.name = true;
        visibleColumns.timeline = true;
        const configs = {};
        for (const columnId in visibleColumns) {
            if (!visibleColumns.hasOwnProperty(columnId)) {
                continue;
            }
            configs[columnId.toLowerCase()] = { visible: visibleColumns[columnId] };
        }
        const newSetting = Settings.instance().createSetting('networkLogColumns', {});
        newSetting.set(configs);
        removeSetting(visibleColumnSettings);
    }
    updateVersionFrom19To20() {
        const oldSetting = Settings.instance().createSetting('InspectorView.panelOrder', {});
        const newSetting = Settings.instance().createSetting('panel-tabOrder', {});
        newSetting.set(oldSetting.get());
        removeSetting(oldSetting);
    }
    updateVersionFrom20To21() {
        const networkColumns = Settings.instance().createSetting('networkLogColumns', {});
        const columns = networkColumns.get();
        delete columns['timeline'];
        delete columns['waterfall'];
        networkColumns.set(columns);
    }
    updateVersionFrom21To22() {
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const breakpointsSetting = Settings.instance().createLocalSetting('breakpoints', []);
        const breakpoints = breakpointsSetting.get();
        for (const breakpoint of breakpoints) {
            breakpoint['url'] = breakpoint['sourceFileId'];
            delete breakpoint['sourceFileId'];
        }
        breakpointsSetting.set(breakpoints);
    }
    updateVersionFrom22To23() {
        // This update is no-op.
    }
    updateVersionFrom23To24() {
        const oldSetting = Settings.instance().createSetting('searchInContentScripts', false);
        const newSetting = Settings.instance().createSetting('searchInAnonymousAndContentScripts', false);
        newSetting.set(oldSetting.get());
        removeSetting(oldSetting);
    }
    updateVersionFrom24To25() {
        const defaultColumns = { status: true, type: true, initiator: true, size: true, time: true };
        // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const networkLogColumnsSetting = Settings.instance().createSetting('networkLogColumns', defaultColumns);
        const columns = networkLogColumnsSetting.get();
        delete columns.product;
        networkLogColumnsSetting.set(columns);
    }
    updateVersionFrom25To26() {
        const oldSetting = Settings.instance().createSetting('messageURLFilters', {});
        const urls = Object.keys(oldSetting.get());
        const textFilter = urls.map(url => `-url:${url}`).join(' ');
        if (textFilter) {
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const textFilterSetting = Settings.instance().createSetting('console.textFilter', '');
            const suffix = textFilterSetting.get() ? ` ${textFilterSetting.get()}` : '';
            textFilterSetting.set(`${textFilter}${suffix}`);
        }
        removeSetting(oldSetting);
    }
    updateVersionFrom26To27() {
        function renameKeyInObjectSetting(settingName, from, to) {
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const setting = Settings.instance().createSetting(settingName, {});
            const value = setting.get();
            if (from in value) {
                value[to] = value[from];
                delete value[from];
                setting.set(value);
            }
        }
        function renameInStringSetting(settingName, from, to) {
            const setting = Settings.instance().createSetting(settingName, '');
            const value = setting.get();
            if (value === from) {
                setting.set(to);
            }
        }
        renameKeyInObjectSetting('panel-tabOrder', 'audits2', 'audits');
        renameKeyInObjectSetting('panel-closeableTabs', 'audits2', 'audits');
        renameInStringSetting('panel-selectedTab', 'audits2', 'audits');
    }
    updateVersionFrom27To28() {
        const setting = Settings.instance().createSetting('uiTheme', 'systemPreferred');
        if (setting.get() === 'default') {
            setting.set('systemPreferred');
        }
    }
    updateVersionFrom28To29() {
        function renameKeyInObjectSetting(settingName, from, to) {
            // TODO(crbug.com/1172300) Ignored during the jsdoc to ts migration
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const setting = Settings.instance().createSetting(settingName, {});
            const value = setting.get();
            if (from in value) {
                value[to] = value[from];
                delete value[from];
                setting.set(value);
            }
        }
        function renameInStringSetting(settingName, from, to) {
            const setting = Settings.instance().createSetting(settingName, '');
            const value = setting.get();
            if (value === from) {
                setting.set(to);
            }
        }
        renameKeyInObjectSetting('panel-tabOrder', 'audits', 'lighthouse');
        renameKeyInObjectSetting('panel-closeableTabs', 'audits', 'lighthouse');
        renameInStringSetting('panel-selectedTab', 'audits', 'lighthouse');
    }
    updateVersionFrom29To30() {
        // Create new location agnostic setting
        const closeableTabSetting = Settings.instance().createSetting('closeableTabs', {});
        // Read current settings
        const panelCloseableTabSetting = Settings.instance().createSetting('panel-closeableTabs', {});
        const drawerCloseableTabSetting = Settings.instance().createSetting('drawer-view-closeableTabs', {});
        const openTabsInPanel = panelCloseableTabSetting.get();
        const openTabsInDrawer = panelCloseableTabSetting.get();
        // Set #value of new setting
        const newValue = Object.assign(openTabsInDrawer, openTabsInPanel);
        closeableTabSetting.set(newValue);
        // Remove old settings
        removeSetting(panelCloseableTabSetting);
        removeSetting(drawerCloseableTabSetting);
    }
    updateVersionFrom30To31() {
        // Remove recorder_recordings setting that was used for storing recordings
        // by an old recorder experiment.
        const recordingsSetting = Settings.instance().createSetting('recorder_recordings', []);
        removeSetting(recordingsSetting);
    }
    updateVersionFrom31To32() {
        // Introduce the new 'resourceTypeName' property on stored breakpoints. Prior to
        // this change we synchronized the breakpoint only by URL, but since we don't
        // know on which resource type the given breakpoint was set, we just assume
        // 'script' here to keep things simple.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const breakpointsSetting = Settings.instance().createLocalSetting('breakpoints', []);
        const breakpoints = breakpointsSetting.get();
        for (const breakpoint of breakpoints) {
            breakpoint['resourceTypeName'] = 'script';
        }
        breakpointsSetting.set(breakpoints);
    }
    updateVersionFrom32To33() {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const previouslyViewedFilesSetting = Settings.instance().createLocalSetting('previouslyViewedFiles', []);
        let previouslyViewedFiles = previouslyViewedFilesSetting.get();
        // Discard old 'previouslyViewedFiles' items that don't have a 'url' property.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        previouslyViewedFiles = previouslyViewedFiles.filter((previouslyViewedFile) => 'url' in previouslyViewedFile);
        // Introduce the new 'resourceTypeName' property on previously viewed files.
        // Prior to this change we only keyed them based on the URL, but since we
        // don't know which resource type the given file had, we just assume 'script'
        // here to keep things simple.
        for (const previouslyViewedFile of previouslyViewedFiles) {
            previouslyViewedFile['resourceTypeName'] = 'script';
        }
        previouslyViewedFilesSetting.set(previouslyViewedFiles);
    }
    updateVersionFrom33To34() {
        // Introduces the 'isLogpoint' property on stored breakpoints. This information was
        // previously encoded in the 'condition' itself. This migration leaves the condition
        // alone but ensures that 'isLogpoint' is accurate for already stored breakpoints.
        // This enables us to use the 'isLogpoint' property in code.
        // A separate migration will remove the special encoding from the condition itself
        // once all refactorings are done.
        // The prefix/suffix are hardcoded here, since these constants will be removed in
        // the future.
        const logpointPrefix = '/** DEVTOOLS_LOGPOINT */ console.log(';
        const logpointSuffix = ')';
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const breakpointsSetting = Settings.instance().createLocalSetting('breakpoints', []);
        const breakpoints = breakpointsSetting.get();
        for (const breakpoint of breakpoints) {
            const isLogpoint = breakpoint.condition.startsWith(logpointPrefix) && breakpoint.condition.endsWith(logpointSuffix);
            breakpoint['isLogpoint'] = isLogpoint;
        }
        breakpointsSetting.set(breakpoints);
    }
    updateVersionFrom34To35() {
        // Uses the 'isLogpoint' property on stored breakpoints to remove the prefix/suffix
        // from logpoints. This way, we store the entered log point condition as the user
        // entered it.
        // The prefix/suffix are hardcoded here, since these constants will be removed in
        // the future.
        const logpointPrefix = '/** DEVTOOLS_LOGPOINT */ console.log(';
        const logpointSuffix = ')';
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const breakpointsSetting = Settings.instance().createLocalSetting('breakpoints', []);
        const breakpoints = breakpointsSetting.get();
        for (const breakpoint of breakpoints) {
            const { condition, isLogpoint } = breakpoint;
            if (isLogpoint) {
                breakpoint.condition = condition.slice(logpointPrefix.length, condition.length - logpointSuffix.length);
            }
        }
        breakpointsSetting.set(breakpoints);
    }
    /*
     * Any new migration should be added before this comment.
     *
     * IMPORTANT: Migrations must be idempotent, since they may be applied
     * multiple times! E.g. when renaming a setting one has to check that the
     * a setting with the new name does not yet exist.
     * ----------------------------------------------------------------------- */
    migrateSettingsFromLocalStorage() {
        // This step migrates all the settings except for the ones below into the browser profile.
        const localSettings = new Set([
            'advancedSearchConfig',
            'breakpoints',
            'consoleHistory',
            'domBreakpoints',
            'eventListenerBreakpoints',
            'fileSystemMapping',
            'lastSelectedSourcesSidebarPaneTab',
            'previouslyViewedFiles',
            'savedURLs',
            'watchExpressions',
            'workspaceExcludedFolders',
            'xhrBreakpoints',
        ]);
        if (!window.localStorage) {
            return;
        }
        for (const key in window.localStorage) {
            if (localSettings.has(key)) {
                continue;
            }
            const value = window.localStorage[key];
            window.localStorage.removeItem(key);
            Settings.instance().globalStorage.set(key, value);
        }
    }
    clearBreakpointsWhenTooMany(breakpointsSetting, maxBreakpointsCount) {
        // If there are too many breakpoints in a storage, it is likely due to a recent bug that caused
        // periodical breakpoints duplication leading to inspector slowness.
        if (breakpointsSetting.get().length > maxBreakpointsCount) {
            breakpointsSetting.set([]);
        }
    }
}
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var SettingStorageType;
(function (SettingStorageType) {
    /**
     * Synced storage persists settings with the active Chrome profile but also
     * syncs the settings across devices via Chrome Sync.
     */
    SettingStorageType["Synced"] = "Synced";
    /** Global storage persists settings with the active Chrome profile */
    SettingStorageType["Global"] = "Global";
    /** Uses Window.localStorage */
    SettingStorageType["Local"] = "Local";
    /** Session storage dies when DevTools window closes */
    SettingStorageType["Session"] = "Session";
})(SettingStorageType || (SettingStorageType = {}));
export function moduleSetting(settingName) {
    return Settings.instance().moduleSetting(settingName);
}
export function settingForTest(settingName) {
    return Settings.instance().settingForTest(settingName);
}
export function detectColorFormat(color) {
    let format;
    const formatSetting = Settings.instance().moduleSetting('colorFormat').get();
    if (formatSetting === "rgb" /* Format.RGB */) {
        format = "rgb" /* Format.RGB */;
    }
    else if (formatSetting === "hsl" /* Format.HSL */) {
        format = "hsl" /* Format.HSL */;
    }
    else if (formatSetting === "hwb" /* Format.HWB */) {
        format = "hwb" /* Format.HWB */;
    }
    else if (formatSetting === "hex" /* Format.HEX */) {
        format = color.asLegacyColor().detectHEXFormat();
    }
    else {
        format = color.format();
    }
    return format;
}
export { getLocalizedSettingsCategory, getRegisteredSettings, maybeRemoveSettingExtension, registerSettingExtension, SettingCategory, SettingType, registerSettingsForTest, resetSettings, };
//# sourceMappingURL=Settings.js.map