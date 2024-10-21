// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as LitHtml from '../../lit-html/lit-html.js';
import * as ComponentHelpers from '../../components/helpers/helpers.js';
import markdownViewStyles from './markdownView.css.js';
import { MarkdownLink } from './MarkdownLink.js';
import { MarkdownImage } from './MarkdownImage.js';
const html = LitHtml.html;
const render = LitHtml.render;
export class MarkdownView extends HTMLElement {
    static litTagName = LitHtml.literal `devtools-markdown-view`;
    #shadow = this.attachShadow({ mode: 'open' });
    #tokenData = [];
    connectedCallback() {
        this.#shadow.adoptedStyleSheets = [markdownViewStyles];
    }
    set data(data) {
        this.#tokenData = data.tokens;
        this.#update();
    }
    #update() {
        this.#render();
    }
    #render() {
        // Disabled until https://crbug.com/1079231 is fixed.
        // clang-format off
        render(html `
      <div class='message'>
        ${this.#tokenData.map(renderToken)}
      </div>
    `, this.#shadow, { host: this });
        // clang-format on
    }
}
ComponentHelpers.CustomElements.defineComponent('devtools-markdown-view', MarkdownView);
const renderChildTokens = (token) => {
    if ('tokens' in token && token.tokens) {
        return token.tokens.map(renderToken);
    }
    throw new Error('Tokens not found');
};
const unescape = (text) => {
    // Unescape will get rid of the escaping done by Marked to avoid double escaping due to escaping it also with Lit-html
    // Table taken from: front_end/third_party/marked/package/src/helpers.js
    const escapeReplacements = new Map([
        ['&amp;', '&'],
        ['&lt;', '<'],
        ['&gt;', '>'],
        ['&quot;', '"'],
        ['&#39;', '\''],
    ]);
    return text.replace(/&(amp|lt|gt|quot|#39);/g, (matchedString) => {
        const replacement = escapeReplacements.get(matchedString);
        return replacement ? replacement : matchedString;
    });
};
const renderText = (token) => {
    if ('tokens' in token && token.tokens) {
        return html `${renderChildTokens(token)}`;
    }
    // Due to unescaping, unescaped html entities (see escapeReplacements' keys) will be rendered
    // as their corresponding symbol while the rest will be rendered as verbatim.
    // Marked's escape function can be found in front_end/third_party/marked/package/src/helpers.js
    return html `${unescape('text' in token ? token.text : '')}`;
};
const renderHeading = (heading) => {
    switch (heading.depth) {
        case 1:
            return html `<h1>${renderText(heading)}</h1>`;
        case 2:
            return html `<h2>${renderText(heading)}</h2>`;
        case 3:
            return html `<h3>${renderText(heading)}</h3>`;
        case 4:
            return html `<h4>${renderText(heading)}</h4>`;
        case 5:
            return html `<h5>${renderText(heading)}</h5>`;
        default:
            return html `<h6>${renderText(heading)}</h6>`;
    }
};
function templateForToken(token) {
    switch (token.type) {
        case 'paragraph':
            return html `<p>${renderChildTokens(token)}`;
        case 'list':
            return html `<ul>${token.items.map(renderToken)}</ul>`;
        case 'list_item':
            return html `<li>${renderChildTokens(token)}`;
        case 'text':
            return renderText(token);
        case 'codespan':
            return html `<code>${unescape(token.text)}</code>`;
        case 'space':
            return html ``;
        case 'link':
            return html `<${MarkdownLink.litTagName} .data=${{ key: token.href, title: token.text }}></${MarkdownLink.litTagName}>`;
        case 'image':
            return html `<${MarkdownImage.litTagName} .data=${{ key: token.href, title: token.text }}></${MarkdownImage.litTagName}>`;
        case 'heading':
            return renderHeading(token);
        case 'strong':
            return html `<strong>${renderText(token)}</strong>`;
        case 'em':
            return html `<em>${renderText(token)}</em>`;
        default:
            return null;
    }
}
export const renderToken = (token) => {
    const template = templateForToken(token);
    if (template === null) {
        throw new Error(`Markdown token type '${token.type}' not supported.`);
    }
    return template;
};
//# sourceMappingURL=MarkdownView.js.map