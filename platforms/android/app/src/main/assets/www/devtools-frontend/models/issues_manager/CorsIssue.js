// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as i18n from '../../core/i18n/i18n.js';
import { Issue, IssueCategory, IssueKind } from './Issue.js';
const UIStrings = {
    /**
     *@description Label for the link for CORS private network issues
     */
    corsPrivateNetworkAccess: 'Private Network Access',
    /**
     *@description Label for the link for CORS network issues
     */
    CORS: 'Cross-Origin Resource Sharing (`CORS`)',
};
const str_ = i18n.i18n.registerUIStrings('models/issues_manager/CorsIssue.ts', UIStrings);
const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
// TODO(crbug.com/1167717): Make this a const enum again
// eslint-disable-next-line rulesdir/const_enum
export var IssueCode;
(function (IssueCode) {
    IssueCode["InsecurePrivateNetwork"] = "CorsIssue::InsecurePrivateNetwork";
    IssueCode["InvalidHeaderValues"] = "CorsIssue::InvalidHeaders";
    IssueCode["WildcardOriginNotAllowed"] = "CorsIssue::WildcardOriginWithCredentials";
    IssueCode["PreflightResponseInvalid"] = "CorsIssue::PreflightResponseInvalid";
    IssueCode["OriginMismatch"] = "CorsIssue::OriginMismatch";
    IssueCode["AllowCredentialsRequired"] = "CorsIssue::AllowCredentialsRequired";
    IssueCode["MethodDisallowedByPreflightResponse"] = "CorsIssue::MethodDisallowedByPreflightResponse";
    IssueCode["HeaderDisallowedByPreflightResponse"] = "CorsIssue::HeaderDisallowedByPreflightResponse";
    IssueCode["RedirectContainsCredentials"] = "CorsIssue::RedirectContainsCredentials";
    IssueCode["DisallowedByMode"] = "CorsIssue::DisallowedByMode";
    IssueCode["CorsDisabledScheme"] = "CorsIssue::CorsDisabledScheme";
    // TODO(https://crbug.com/1263483): Remove this once it's removed from CDP.
    IssueCode["PreflightMissingAllowExternal"] = "CorsIssue::PreflightMissingAllowExternal";
    // TODO(https://crbug.com/1263483): Remove this once it's removed from CDP.
    IssueCode["PreflightInvalidAllowExternal"] = "CorsIssue::PreflightInvalidAllowExternal";
    IssueCode["NoCorsRedirectModeNotFollow"] = "CorsIssue::NoCorsRedirectModeNotFollow";
    IssueCode["InvalidPrivateNetworkAccess"] = "CorsIssue::InvalidPrivateNetworkAccess";
    IssueCode["UnexpectedPrivateNetworkAccess"] = "CorsIssue::UnexpectedPrivateNetworkAccess";
    IssueCode["PreflightAllowPrivateNetworkError"] = "CorsIssue::PreflightAllowPrivateNetworkError";
})(IssueCode || (IssueCode = {}));
function getIssueCode(details) {
    switch (details.corsErrorStatus.corsError) {
        case "InvalidAllowMethodsPreflightResponse" /* Protocol.Network.CorsError.InvalidAllowMethodsPreflightResponse */:
        case "InvalidAllowHeadersPreflightResponse" /* Protocol.Network.CorsError.InvalidAllowHeadersPreflightResponse */:
        case "PreflightMissingAllowOriginHeader" /* Protocol.Network.CorsError.PreflightMissingAllowOriginHeader */:
        case "PreflightMultipleAllowOriginValues" /* Protocol.Network.CorsError.PreflightMultipleAllowOriginValues */:
        case "PreflightInvalidAllowOriginValue" /* Protocol.Network.CorsError.PreflightInvalidAllowOriginValue */:
        case "MissingAllowOriginHeader" /* Protocol.Network.CorsError.MissingAllowOriginHeader */:
        case "MultipleAllowOriginValues" /* Protocol.Network.CorsError.MultipleAllowOriginValues */:
        case "InvalidAllowOriginValue" /* Protocol.Network.CorsError.InvalidAllowOriginValue */:
            return IssueCode.InvalidHeaderValues;
        case "PreflightWildcardOriginNotAllowed" /* Protocol.Network.CorsError.PreflightWildcardOriginNotAllowed */:
        case "WildcardOriginNotAllowed" /* Protocol.Network.CorsError.WildcardOriginNotAllowed */:
            return IssueCode.WildcardOriginNotAllowed;
        case "PreflightInvalidStatus" /* Protocol.Network.CorsError.PreflightInvalidStatus */:
        case "PreflightDisallowedRedirect" /* Protocol.Network.CorsError.PreflightDisallowedRedirect */:
        case "InvalidResponse" /* Protocol.Network.CorsError.InvalidResponse */:
            return IssueCode.PreflightResponseInvalid;
        case "AllowOriginMismatch" /* Protocol.Network.CorsError.AllowOriginMismatch */:
        case "PreflightAllowOriginMismatch" /* Protocol.Network.CorsError.PreflightAllowOriginMismatch */:
            return IssueCode.OriginMismatch;
        case "InvalidAllowCredentials" /* Protocol.Network.CorsError.InvalidAllowCredentials */:
        case "PreflightInvalidAllowCredentials" /* Protocol.Network.CorsError.PreflightInvalidAllowCredentials */:
            return IssueCode.AllowCredentialsRequired;
        case "MethodDisallowedByPreflightResponse" /* Protocol.Network.CorsError.MethodDisallowedByPreflightResponse */:
            return IssueCode.MethodDisallowedByPreflightResponse;
        case "HeaderDisallowedByPreflightResponse" /* Protocol.Network.CorsError.HeaderDisallowedByPreflightResponse */:
            return IssueCode.HeaderDisallowedByPreflightResponse;
        case "RedirectContainsCredentials" /* Protocol.Network.CorsError.RedirectContainsCredentials */:
            return IssueCode.RedirectContainsCredentials;
        case "DisallowedByMode" /* Protocol.Network.CorsError.DisallowedByMode */:
            return IssueCode.DisallowedByMode;
        case "CorsDisabledScheme" /* Protocol.Network.CorsError.CorsDisabledScheme */:
            return IssueCode.CorsDisabledScheme;
        case "PreflightMissingAllowExternal" /* Protocol.Network.CorsError.PreflightMissingAllowExternal */:
            return IssueCode.PreflightMissingAllowExternal;
        case "PreflightInvalidAllowExternal" /* Protocol.Network.CorsError.PreflightInvalidAllowExternal */:
            return IssueCode.PreflightInvalidAllowExternal;
        case "InsecurePrivateNetwork" /* Protocol.Network.CorsError.InsecurePrivateNetwork */:
            return IssueCode.InsecurePrivateNetwork;
        case "NoCorsRedirectModeNotFollow" /* Protocol.Network.CorsError.NoCorsRedirectModeNotFollow */:
            return IssueCode.NoCorsRedirectModeNotFollow;
        case "InvalidPrivateNetworkAccess" /* Protocol.Network.CorsError.InvalidPrivateNetworkAccess */:
            return IssueCode.InvalidPrivateNetworkAccess;
        case "UnexpectedPrivateNetworkAccess" /* Protocol.Network.CorsError.UnexpectedPrivateNetworkAccess */:
            return IssueCode.UnexpectedPrivateNetworkAccess;
        case "PreflightMissingAllowPrivateNetwork" /* Protocol.Network.CorsError.PreflightMissingAllowPrivateNetwork */:
        case "PreflightInvalidAllowPrivateNetwork" /* Protocol.Network.CorsError.PreflightInvalidAllowPrivateNetwork */:
            return IssueCode.PreflightAllowPrivateNetworkError;
    }
}
export class CorsIssue extends Issue {
    #issueDetails;
    constructor(issueDetails, issuesModel, issueId) {
        super(getIssueCode(issueDetails), issuesModel, issueId);
        this.#issueDetails = issueDetails;
    }
    getCategory() {
        return IssueCategory.Cors;
    }
    details() {
        return this.#issueDetails;
    }
    getDescription() {
        switch (getIssueCode(this.#issueDetails)) {
            case IssueCode.InsecurePrivateNetwork:
                return {
                    file: 'corsInsecurePrivateNetwork.md',
                    links: [{
                            link: 'https://developer.chrome.com/blog/private-network-access-update',
                            linkTitle: i18nString(UIStrings.corsPrivateNetworkAccess),
                        }],
                };
            case IssueCode.PreflightAllowPrivateNetworkError:
                return {
                    file: 'corsPreflightAllowPrivateNetworkError.md',
                    links: [{
                            link: 'https://developer.chrome.com/blog/private-network-access-update',
                            linkTitle: i18nString(UIStrings.corsPrivateNetworkAccess),
                        }],
                };
            case IssueCode.InvalidHeaderValues:
                return {
                    file: 'corsInvalidHeaderValues.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.WildcardOriginNotAllowed:
                return {
                    file: 'corsWildcardOriginNotAllowed.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.PreflightResponseInvalid:
                return {
                    file: 'corsPreflightResponseInvalid.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.OriginMismatch:
                return {
                    file: 'corsOriginMismatch.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.AllowCredentialsRequired:
                return {
                    file: 'corsAllowCredentialsRequired.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.MethodDisallowedByPreflightResponse:
                return {
                    file: 'corsMethodDisallowedByPreflightResponse.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.HeaderDisallowedByPreflightResponse:
                return {
                    file: 'corsHeaderDisallowedByPreflightResponse.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.RedirectContainsCredentials:
                return {
                    file: 'corsRedirectContainsCredentials.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.DisallowedByMode:
                return {
                    file: 'corsDisallowedByMode.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.CorsDisabledScheme:
                return {
                    file: 'corsDisabledScheme.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.NoCorsRedirectModeNotFollow:
                return {
                    file: 'corsNoCorsRedirectModeNotFollow.md',
                    links: [{
                            link: 'https://web.dev/cross-origin-resource-sharing',
                            linkTitle: i18nString(UIStrings.CORS),
                        }],
                };
            case IssueCode.PreflightMissingAllowExternal:
            case IssueCode.PreflightInvalidAllowExternal:
            case IssueCode.InvalidPrivateNetworkAccess:
            case IssueCode.UnexpectedPrivateNetworkAccess:
                return null;
        }
    }
    primaryKey() {
        return JSON.stringify(this.#issueDetails);
    }
    getKind() {
        if (this.#issueDetails.isWarning &&
            (this.#issueDetails.corsErrorStatus.corsError === "InsecurePrivateNetwork" /* Protocol.Network.CorsError.InsecurePrivateNetwork */ ||
                this.#issueDetails.corsErrorStatus.corsError ===
                    "PreflightMissingAllowPrivateNetwork" /* Protocol.Network.CorsError.PreflightMissingAllowPrivateNetwork */ ||
                this.#issueDetails.corsErrorStatus.corsError ===
                    "PreflightInvalidAllowPrivateNetwork" /* Protocol.Network.CorsError.PreflightInvalidAllowPrivateNetwork */)) {
            return IssueKind.BreakingChange;
        }
        return IssueKind.PageError;
    }
    static fromInspectorIssue(issuesModel, inspectorIssue) {
        const corsIssueDetails = inspectorIssue.details.corsIssueDetails;
        if (!corsIssueDetails) {
            console.warn('Cors issue without details received.');
            return [];
        }
        return [new CorsIssue(corsIssueDetails, issuesModel, inspectorIssue.issueId)];
    }
}
//# sourceMappingURL=CorsIssue.js.map