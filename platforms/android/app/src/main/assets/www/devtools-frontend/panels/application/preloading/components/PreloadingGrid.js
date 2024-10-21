// Copyright 2022 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as i18n from '../../../../core/i18n/i18n.js';
import * as DataGrid from '../../../../ui/components/data_grid/data_grid.js';
import * as ComponentHelpers from '../../../../ui/components/helpers/helpers.js';
import * as LegacyWrapper from '../../../../ui/components/legacy_wrapper/legacy_wrapper.js';
import * as LitHtml from '../../../../ui/lit-html/lit-html.js';
import preloadingGridStyles from './preloadingGrid.css.js';
const UIStrings = {
    /**
     *@description Column header: Action of preloading (prefetch/prerender)
     */
    action: 'Action',
    /**
     *@description Column header: Status of preloading attempt
     */
    status: 'Status',
};
const str_ = i18n.i18n.registerUIStrings('panels/application/preloading/components/PreloadingGrid.ts', UIStrings);
export const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
const { render, html } = LitHtml;
// Grid component to show prerendering attempts.
export class PreloadingGrid extends LegacyWrapper.LegacyWrapper.WrappableComponent {
    static litTagName = LitHtml.literal `devtools-resources-preloading-grid`;
    #shadow = this.attachShadow({ mode: 'open' });
    #rows = [];
    connectedCallback() {
        this.#shadow.adoptedStyleSheets = [preloadingGridStyles];
        this.#render();
    }
    update(rows) {
        this.#rows = rows;
        this.#render();
    }
    #render() {
        const reportsGridData = {
            columns: [
                {
                    id: 'url',
                    title: i18n.i18n.lockedString('URL'),
                    widthWeighting: 40,
                    hideable: false,
                    visible: true,
                },
                {
                    id: 'action',
                    title: i18nString(UIStrings.action),
                    widthWeighting: 15,
                    hideable: false,
                    visible: true,
                },
                {
                    id: 'status',
                    title: i18nString(UIStrings.status),
                    widthWeighting: 15,
                    hideable: false,
                    visible: true,
                },
            ],
            rows: this.#buildReportRows(),
        };
        // Disabled until https://crbug.com/1079231 is fixed.
        // clang-format off
        render(html `
      <div class="preloading-container">
        <${DataGrid.DataGridController.DataGridController.litTagName} .data=${reportsGridData}>
        </${DataGrid.DataGridController.DataGridController.litTagName}>
      </div>
    `, this.#shadow, { host: this });
        // clang-format on
    }
    #buildReportRows() {
        return this.#rows.map(row => ({
            cells: [
                { columnId: 'id', value: row.id },
                { columnId: 'url', value: row.url },
                { columnId: 'action', value: row.action },
                { columnId: 'status', value: row.status },
            ],
        }));
    }
}
ComponentHelpers.CustomElements.defineComponent('devtools-resources-preloading-grid', PreloadingGrid);
//# sourceMappingURL=PreloadingGrid.js.map