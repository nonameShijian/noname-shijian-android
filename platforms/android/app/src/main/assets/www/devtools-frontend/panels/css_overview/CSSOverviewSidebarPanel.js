// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as Common from '../../core/common/common.js';
import * as i18n from '../../core/i18n/i18n.js';
import * as UI from '../../ui/legacy/legacy.js';
import cssOverviewSidebarPanelStyles from './cssOverviewSidebarPanel.css.js';
const UIStrings = {
    /**
     *@description Label for the 'Clear overview' button in the CSS Overview report
     */
    clearOverview: 'Clear overview',
    /**
     * @description Accessible label for the CSS Overview panel sidebar
     */
    cssOverviewPanelSidebar: 'CSS Overview panel sidebar',
};
const str_ = i18n.i18n.registerUIStrings('panels/css_overview/CSSOverviewSidebarPanel.ts', UIStrings);
const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
export class CSSOverviewSidebarPanel extends Common.ObjectWrapper.eventMixin(UI.Widget.VBox) {
    containerElement;
    // eslint-disable-next-line @typescript-eslint/naming-convention
    static get ITEM_CLASS_NAME() {
        return 'overview-sidebar-panel-item';
    }
    // eslint-disable-next-line @typescript-eslint/naming-convention
    static get SELECTED() {
        return 'selected';
    }
    constructor() {
        super(true);
        this.contentElement.classList.add('overview-sidebar-panel');
        this.contentElement.addEventListener('click', this.#onItemClick.bind(this));
        this.contentElement.addEventListener('keydown', this.#onItemKeyDown.bind(this));
        // We need a container so that each item covers the full width of the
        // longest item, so that the selected item's background expands fully
        // even when the sidebar overflows.
        // Also see crbug/1408003
        this.containerElement =
            this.contentElement.createChild('div', 'overview-sidebar-panel-container');
        UI.ARIAUtils.setLabel(this.containerElement, i18nString(UIStrings.cssOverviewPanelSidebar));
        UI.ARIAUtils.markAsTree(this.containerElement);
        // Clear overview.
        const clearResultsButton = new UI.Toolbar.ToolbarButton(i18nString(UIStrings.clearOverview), 'clear');
        clearResultsButton.addEventListener(UI.Toolbar.ToolbarButton.Events.Click, this.#reset, this);
        // Toolbar.
        const toolbarElement = this.containerElement.createChild('div', 'overview-toolbar');
        const toolbar = new UI.Toolbar.Toolbar('', toolbarElement);
        toolbar.appendToolbarItem(clearResultsButton);
    }
    addItem(name, id) {
        const item = this.containerElement.createChild('div', CSSOverviewSidebarPanel.ITEM_CLASS_NAME);
        UI.ARIAUtils.markAsTreeitem(item);
        item.textContent = name;
        item.dataset.id = id;
        item.tabIndex = 0;
    }
    #reset() {
        this.dispatchEventToListeners("Reset" /* SidebarEvents.Reset */);
    }
    #deselectAllItems() {
        const items = this.containerElement.querySelectorAll(`.${CSSOverviewSidebarPanel.ITEM_CLASS_NAME}`);
        items.forEach(item => {
            item.classList.remove(CSSOverviewSidebarPanel.SELECTED);
        });
    }
    #onItemClick(event) {
        const target = event.composedPath()[0];
        if (!target.classList.contains(CSSOverviewSidebarPanel.ITEM_CLASS_NAME)) {
            return;
        }
        const { id } = target.dataset;
        if (!id) {
            return;
        }
        this.select(id);
        this.dispatchEventToListeners("ItemSelected" /* SidebarEvents.ItemSelected */, { id, isMouseEvent: true });
    }
    #onItemKeyDown(event) {
        if (event.key !== 'Enter') {
            return;
        }
        const target = event.composedPath()[0];
        if (!target.classList.contains(CSSOverviewSidebarPanel.ITEM_CLASS_NAME)) {
            return;
        }
        const { id } = target.dataset;
        if (!id) {
            return;
        }
        this.select(id);
        this.dispatchEventToListeners("ItemSelected" /* SidebarEvents.ItemSelected */, { id, isMouseEvent: false });
        event.consume(true);
    }
    select(id) {
        const target = this.containerElement.querySelector(`[data-id=${CSS.escape(id)}]`);
        if (!target) {
            return;
        }
        if (target.classList.contains(CSSOverviewSidebarPanel.SELECTED)) {
            return;
        }
        this.#deselectAllItems();
        target.classList.add(CSSOverviewSidebarPanel.SELECTED);
    }
    wasShown() {
        super.wasShown();
        this.registerCSSFiles([cssOverviewSidebarPanelStyles]);
    }
}
//# sourceMappingURL=CSSOverviewSidebarPanel.js.map