// Copyright 2022 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import * as i18n from '../../../../core/i18n/i18n.js';
import { assertNotNullOrUndefined } from '../../../../core/platform/platform.js';
import * as SDK from '../../../../core/sdk/sdk.js';
import * as ComponentHelpers from '../../../../ui/components/helpers/helpers.js';
import * as LegacyWrapper from '../../../../ui/components/legacy_wrapper/legacy_wrapper.js';
import * as Coordinator from '../../../../ui/components/render_coordinator/render_coordinator.js';
import * as ReportView from '../../../../ui/components/report_view/report_view.js';
import * as LitHtml from '../../../../ui/lit-html/lit-html.js';
import preloadingDetailsReportViewStyles from './preloadingDetailsReportView.css.js';
import { PrefetchReasonDescription } from './PreloadingString.js';
const UIStrings = {
    /**
     *@description Text in PreloadingDetailsReportView of the Application panel
     */
    selectAnElementForMoreDetails: 'Select an element for more details',
    /**
     *@description Text in details
     */
    detailsDetailedInformation: 'Detailed information',
    /**
     *@description Text in details
     */
    detailsAction: 'Action',
    /**
     *@description Text in details
     */
    detailsStatus: 'Status',
    /**
     *@description Text in details
     */
    detailsFailureReason: 'Failure reason',
    /**
     *@description Header of rule set
     */
    detailsRuleSet: 'Rule set',
    /**
     *@description Description: status
     */
    detailedStatusNotTriggered: 'Preloading attempt is not yet triggered.',
    /**
     *@description Description: status
     */
    detailedStatusPending: 'Preloading attempt is eligible but pending.',
    /**
     *@description Description: status
     */
    detailedStatusRunning: 'Preloading is running.',
    /**
     *@description Description: status
     */
    detailedStatusReady: 'Preloading finished and the result is ready for the next navigation.',
    /**
     *@description Description: status
     */
    detailedStatusSuccess: 'Preloading finished and used for a navigation.',
    /**
     *@description Description: status
     */
    detailedStatusFailure: 'Preloading failed.',
    /**
     *  Description text for PrerenderFinalStatus::kLowEndDevice.
     */
    prerenderFinalStatusLowEndDevice: 'The prerender was not performed because this device does not have enough total system memory to support prerendering.',
    /**
     *  Description text for PrerenderFinalStatus::kInvalidSchemeRedirect.
     */
    prerenderFinalStatusInvalidSchemeRedirect: 'The prerendering navigation failed because it redirected to a URL whose scheme was not http: or https:.',
    /**
     *  Description text for PrerenderFinalStatus::kInvalidSchemeNavigation.
     */
    prerenderFinalStatusInvalidSchemeNavigation: 'The URL was not eligible to be prerendered because its scheme was not http: or https:.',
    /**
     *  Description text for PrerenderFinalStatus::kNavigationRequestBlockedByCsp.
     */
    prerenderFinalStatusNavigationRequestBlockedByCsp: 'The prerendering navigation was blocked by a Content Security Policy.',
    /**
     *  Description text for PrerenderFinalStatus::kMainFrameNavigation.
     */
    prerenderFinalStatusMainFrameNavigation: 'The prerendered page navigated itself to another URL, which is currently not supported.',
    /**
     *  Description text for PrerenderFinalStatus::kMojoBinderPolicy.
     */
    prerenderFinalStatusMojoBinderPolicy: 'The prerendered page used a forbidden JavaScript API that is currently not supported.',
    /**
     *  Description text for PrerenderFinalStatus::kRendererProcessCrashed.
     */
    prerenderFinalStatusRendererProcessCrashed: 'The prerendered page crashed.',
    /**
     *  Description text for PrerenderFinalStatus::kRendererProcessKilled.
     */
    prerenderFinalStatusRendererProcessKilled: 'The prerendered page was killed.',
    /**
     *  Description text for PrerenderFinalStatus::kDownload.
     */
    prerenderFinalStatusDownload: 'The prerendered page attempted to initiate a download, which is currently not supported.',
    /**
     *  Description text for PrerenderFinalStatus::kNavigationBadHttpStatus.
     */
    prerenderFinalStatusNavigationBadHttpStatus: 'The prerendering navigation failed because of a non-2xx HTTP response status code.',
    /**
     *  Description text for PrerenderFinalStatus::kClientCertRequested.
     */
    prerenderFinalStatusClientCertRequested: 'The prerendering navigation required a HTTP client certificate.',
    /**
     *  Description text for PrerenderFinalStatus::kNavigationRequestNetworkError.
     */
    prerenderFinalStatusNavigationRequestNetworkError: 'The prerendering navigation encountered a network error.',
    /**
     *  Description text for PrerenderFinalStatus::kMaxNumOfRunningPrerendersExceeded.
     */
    prerenderFinalStatusMaxNumOfRunningPrerendersExceeded: 'The prerender was not performed because the initiating page already has too many prerenders ongoing. Remove other speculation rules to enable further prerendering.',
    /**
     *  Description text for PrerenderFinalStatus::kSslCertificateError.
     */
    prerenderFinalStatusSslCertificateError: 'The prerendering navigation failed because of an invalid SSL certificate.',
    /**
     *  Description text for PrerenderFinalStatus::kLoginAuthRequested.
     */
    prerenderFinalStatusLoginAuthRequested: 'The prerendering navigation required HTTP authentication, which is currently not supported.',
    /**
     *  Description text for PrerenderFinalStatus::kUaChangeRequiresReload.
     */
    prerenderFinalStatusUaChangeRequiresReload: 'Changing User Agent occured in prerendering navigation.',
    /**
     *  Description text for PrerenderFinalStatus::kBlockedByClient.
     */
    prerenderFinalStatusBlockedByClient: 'Some resource load was blocked.',
    /**
     *  Description text for PrerenderFinalStatus::kAudioOutputDeviceRequested.
     */
    prerenderFinalStatusAudioOutputDeviceRequested: 'The prerendered page requested audio output, which is currently not supported.',
    /**
     *  Description text for PrerenderFinalStatus::kMixedContent.
     */
    prerenderFinalStatusMixedContent: 'The prerendered page contained mixed content.',
    /**
     *  Description text for PrerenderFinalStatus::kTriggerBackgrounded.
     */
    prerenderFinalStatusTriggerBackgrounded: 'The initiating page was backgrounded, so the prerendered page was discarded.',
    /**
     *  Description text for PrerenderFinalStatus::kMemoryLimitExceeded.
     */
    prerenderFinalStatusMemoryLimitExceeded: 'The prerender was not performed because the browser exceeded the prerendering memory limit.',
    /**
     *  Description text for PrerenderFinalStatus::kFailToGetMemoryUsage.
     */
    prerenderFinalStatusFailToGetMemoryUsage: 'The prerender was not performed because the browser encountered an internal error attempting to determine current memory usage.',
    /**
     *  Description text for PrerenderFinalStatus::kDataSaverEnabled.
     */
    prerenderFinalStatusDataSaverEnabled: 'The prerender was not performed because the user requested that the browser use less data.',
    /**
     *  Description text for PrerenderFinalStatus::kHasEffectiveUrl.
     */
    prerenderFinalStatusHasEffectiveUrl: 'The initiating page cannot perform prerendering, because it has an effective URL that is different from its normal URL. (For example, the New Tab Page, or hosted apps.)',
    /**
     *  Description text for PrerenderFinalStatus::kTimeoutBackgrounded.
     */
    prerenderFinalStatusTimeoutBackgrounded: 'The initiating page was backgrounded for a long time, so the prerendered page was discarded.',
    /**
     *  Description text for PrerenderFinalStatus::kCrossSiteRedirectInInitialNavigation.
     */
    prerenderFinalStatusCrossSiteRedirectInInitialNavigation: 'The prerendering navigation failed because the prerendered URL redirected to a cross-site URL.',
    /**
     *  Description text for PrerenderFinalStatus::kCrossSiteNavigationInInitialNavigation.
     */
    prerenderFinalStatusCrossSiteNavigationInInitialNavigation: 'The prerendering navigation failed because it targeted a cross-site URL.',
    /**
     *  Description text for PrerenderFinalStatus::kSameSiteCrossOriginRedirectNotOptInInInitialNavigation.
     */
    prerenderFinalStatusSameSiteCrossOriginRedirectNotOptInInInitialNavigation: 'The prerendering navigation failed because the prerendered URL redirected to a cross-origin same-site URL, but the destination response did not include the appropriate Supports-Loading-Mode header.',
    /**
     *  Description text for PrerenderFinalStatus::kSameSiteCrossOriginNavigationNotOptInInInitialNavigation.
     */
    prerenderFinalStatusSameSiteCrossOriginNavigationNotOptInInInitialNavigation: 'The prerendered page navigated itself to a cross-origin same-site URL, but the destination response did not include the appropriate Supports-Loading-Mode header.',
    /**
     *  Description text for PrerenderFinalStatus::kActivationNavigationParameterMismatch.
     */
    prerenderFinalStatusActivationNavigationParameterMismatch: 'The prerender was not used because during activation time, different navigation parameters (e.g., HTTP headers) were calculated than during the original prerendering navigation request.',
    /**
     *  Description text for PrerenderFinalStatus::kPrimaryMainFrameRendererProcessCrashed.
     */
    prerenderFinalStatusPrimaryMainFrameRendererProcessCrashed: 'The initiating page crashed.',
    /**
     *  Description text for PrerenderFinalStatus::kPrimaryMainFrameRendererProcessKilled.
     */
    prerenderFinalStatusPrimaryMainFrameRendererProcessKilled: 'The initiating page was killed.',
    /**
     *  Description text for PrerenderFinalStatus::kActivationFramePolicyNotCompatible.
     */
    prerenderFinalStatusActivationFramePolicyNotCompatible: 'The prerender was not used because the sandboxing flags or permissions policy of the initiating page was not compatible with those of the prerendering page.',
    /**
     *  Description text for PrerenderFinalStatus::kPreloadingDisabled.
     */
    prerenderFinalStatusPreloadingDisabled: 'The prerender was not performed because the user disabled preloading in their browser settings.',
    /**
     *  Description text for PrerenderFinalStatus::kBatterySaverEnabled.
     */
    prerenderFinalStatusBatterySaverEnabled: 'The prerender was not performed because the user requested that the browser use less battery.',
    /**
     *  Description text for PrerenderFinalStatus::kActivatedDuringMainFrameNavigation.
     */
    prerenderFinalStatusActivatedDuringMainFrameNavigation: 'Prerendered page activated during initiating page\'s main frame navigation.',
    /**
     *  Description text for PrerenderFinalStatus::kCrossSiteRedirectInMainFrameNavigation.
     */
    prerenderFinalStatusCrossSiteRedirectInMainFrameNavigation: 'The prerendered page navigated to a URL which redirected to a cross-site URL.',
    /**
     *  Description text for PrerenderFinalStatus::kCrossSiteNavigationInMainFrameNavigation.
     */
    prerenderFinalStatusCrossSiteNavigationInMainFrameNavigation: 'The prerendered page navigated to a cross-site URL.',
    /**
     *  Description text for PrerenderFinalStatus::kSameSiteCrossOriginRedirectNotOptInInMainFrameNavigation.
     */
    prerenderFinalStatusSameSiteCrossOriginRedirectNotOptInInMainFrameNavigation: 'The prerendered page navigated to a URL which redirected to a cross-origin same-site URL, but the destination response did not include the appropriate Supports-Loading-Mode header.',
    /**
     *  Description text for PrerenderFinalStatus::kSameSiteCrossOriginNavigationNotOptInInMainFrameNavigation.
     */
    prerenderFinalStatusSameSiteCrossOriginNavigationNotOptInInMainFrameNavigation: 'The prerendered page navigated to a cross-origin same-site URL, but the destination response did not include the appropriate Supports-Loading-Mode header.',
    /**
     *  Description text for PrerenderFinalStatus::kMemoryPressureOnTrigger.
     */
    prerenderFinalStatusMemoryPressureOnTrigger: 'The prerender was not performed because the browser was under critical memory pressure.',
    /**
     *  Description text for PrerenderFinalStatus::kMemoryPressureAfterTriggered.
     */
    prerenderFinalStatusMemoryPressureAfterTriggered: 'The prerendered page was unloaded because the browser came under critical memory pressure.',
    /**
     *  Description text for PrerenderFinalStatus::kPrerenderingDisabledByDevTools.
     */
    prerenderFinalStatusPrerenderingDisabledByDevTools: 'The prerender was not performed because DevTools has been used to disable prerendering.',
    /**
     *  Description text for PrerenderFinalStatus::kResourceLoadBlockedByClient.
     */
    prerenderFinalStatusResourceLoadBlockedByClient: 'Some resource load was blocked.',
};
const str_ = i18n.i18n.registerUIStrings('panels/application/preloading/components/PreloadingDetailsReportView.ts', UIStrings);
const i18nString = i18n.i18n.getLocalizedString.bind(undefined, str_);
class PreloadingUIUtils {
    static action({ key }) {
        // Use "prefetch"/"prerender" as is in SpeculationRules.
        switch (key.action) {
            case "Prefetch" /* Protocol.Preload.SpeculationAction.Prefetch */:
                return i18n.i18n.lockedString('prefetch');
            case "Prerender" /* Protocol.Preload.SpeculationAction.Prerender */:
                return i18n.i18n.lockedString('prerender');
        }
    }
    static detailedStatus({ status }) {
        // See content/public/browser/preloading.h PreloadingAttemptOutcome.
        switch (status) {
            case "NotTriggered" /* SDK.PreloadingModel.PreloadingStatus.NotTriggered */:
                return i18nString(UIStrings.detailedStatusNotTriggered);
            case "Pending" /* SDK.PreloadingModel.PreloadingStatus.Pending */:
                return i18nString(UIStrings.detailedStatusPending);
            case "Running" /* SDK.PreloadingModel.PreloadingStatus.Running */:
                return i18nString(UIStrings.detailedStatusRunning);
            case "Ready" /* SDK.PreloadingModel.PreloadingStatus.Ready */:
                return i18nString(UIStrings.detailedStatusReady);
            case "Success" /* SDK.PreloadingModel.PreloadingStatus.Success */:
                return i18nString(UIStrings.detailedStatusSuccess);
            case "Failure" /* SDK.PreloadingModel.PreloadingStatus.Failure */:
                return i18nString(UIStrings.detailedStatusFailure);
            // NotSupported is used to handle unreachable case. For example,
            // there is no code path for
            // PreloadingTriggeringOutcome::kTriggeredButPending in prefetch,
            // which is mapped to NotSupported. So, we regard it as an
            // internal error.
            case "NotSupported" /* SDK.PreloadingModel.PreloadingStatus.NotSupported */:
                return i18n.i18n.lockedString('Internal error');
        }
    }
    // Detailed failure reason for PrerenderFinalStatus.
    static failureReason({ prerenderStatus }) {
        // If you face an error on rolling CDP changes, see
        // https://docs.google.com/document/d/1PnrfowsZMt62PX1EvvTp2Nqs3ji1zrklrAEe1JYbkTk
        switch (prerenderStatus) {
            case null:
            case "Activated" /* Protocol.Preload.PrerenderFinalStatus.Activated */:
                return null;
            case "Destroyed" /* Protocol.Preload.PrerenderFinalStatus.Destroyed */:
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Unknown');
            case "LowEndDevice" /* Protocol.Preload.PrerenderFinalStatus.LowEndDevice */:
                return i18nString(UIStrings.prerenderFinalStatusLowEndDevice);
            case "InvalidSchemeRedirect" /* Protocol.Preload.PrerenderFinalStatus.InvalidSchemeRedirect */:
                return i18nString(UIStrings.prerenderFinalStatusInvalidSchemeRedirect);
            case "InvalidSchemeNavigation" /* Protocol.Preload.PrerenderFinalStatus.InvalidSchemeNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusInvalidSchemeNavigation);
            case "InProgressNavigation" /* Protocol.Preload.PrerenderFinalStatus.InProgressNavigation */:
                // Not used.
                return i18n.i18n.lockedString('Internal error');
            case "NavigationRequestBlockedByCsp" /* Protocol.Preload.PrerenderFinalStatus.NavigationRequestBlockedByCsp */:
                return i18nString(UIStrings.prerenderFinalStatusNavigationRequestBlockedByCsp);
            case "MainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.MainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusMainFrameNavigation);
            case "MojoBinderPolicy" /* Protocol.Preload.PrerenderFinalStatus.MojoBinderPolicy */:
                // TODO(https://crbug.com/1410709): Improve these messages with disallowedApiMethod.
                return i18nString(UIStrings.prerenderFinalStatusMojoBinderPolicy);
            case "RendererProcessCrashed" /* Protocol.Preload.PrerenderFinalStatus.RendererProcessCrashed */:
                return i18nString(UIStrings.prerenderFinalStatusRendererProcessCrashed);
            case "RendererProcessKilled" /* Protocol.Preload.PrerenderFinalStatus.RendererProcessKilled */:
                return i18nString(UIStrings.prerenderFinalStatusRendererProcessKilled);
            case "Download" /* Protocol.Preload.PrerenderFinalStatus.Download */:
                return i18nString(UIStrings.prerenderFinalStatusDownload);
            case "TriggerDestroyed" /* Protocol.Preload.PrerenderFinalStatus.TriggerDestroyed */:
                // After https://crrev.com/c/4515841, this won't occur if DevTools is opened.
                return i18n.i18n.lockedString('Internal error');
            case "NavigationNotCommitted" /* Protocol.Preload.PrerenderFinalStatus.NavigationNotCommitted */:
                // This looks internal error.
                //
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Internal error');
            case "NavigationBadHttpStatus" /* Protocol.Preload.PrerenderFinalStatus.NavigationBadHttpStatus */:
                return i18nString(UIStrings.prerenderFinalStatusNavigationBadHttpStatus);
            case "ClientCertRequested" /* Protocol.Preload.PrerenderFinalStatus.ClientCertRequested */:
                return i18nString(UIStrings.prerenderFinalStatusClientCertRequested);
            case "NavigationRequestNetworkError" /* Protocol.Preload.PrerenderFinalStatus.NavigationRequestNetworkError */:
                return i18nString(UIStrings.prerenderFinalStatusNavigationRequestNetworkError);
            case "MaxNumOfRunningPrerendersExceeded" /* Protocol.Preload.PrerenderFinalStatus.MaxNumOfRunningPrerendersExceeded */:
                return i18nString(UIStrings.prerenderFinalStatusMaxNumOfRunningPrerendersExceeded);
            case "CancelAllHostsForTesting" /* Protocol.Preload.PrerenderFinalStatus.CancelAllHostsForTesting */:
                // Used only in tests.
                throw new Error('unreachable');
            case "DidFailLoad" /* Protocol.Preload.PrerenderFinalStatus.DidFailLoad */:
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Unknown');
            case "Stop" /* Protocol.Preload.PrerenderFinalStatus.Stop */:
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Unknown');
            case "SslCertificateError" /* Protocol.Preload.PrerenderFinalStatus.SslCertificateError */:
                return i18nString(UIStrings.prerenderFinalStatusSslCertificateError);
            case "LoginAuthRequested" /* Protocol.Preload.PrerenderFinalStatus.LoginAuthRequested */:
                return i18nString(UIStrings.prerenderFinalStatusLoginAuthRequested);
            case "UaChangeRequiresReload" /* Protocol.Preload.PrerenderFinalStatus.UaChangeRequiresReload */:
                return i18nString(UIStrings.prerenderFinalStatusUaChangeRequiresReload);
            case "BlockedByClient" /* Protocol.Preload.PrerenderFinalStatus.BlockedByClient */:
                return i18nString(UIStrings.prerenderFinalStatusBlockedByClient);
            case "AudioOutputDeviceRequested" /* Protocol.Preload.PrerenderFinalStatus.AudioOutputDeviceRequested */:
                return i18nString(UIStrings.prerenderFinalStatusAudioOutputDeviceRequested);
            case "MixedContent" /* Protocol.Preload.PrerenderFinalStatus.MixedContent */:
                return i18nString(UIStrings.prerenderFinalStatusMixedContent);
            case "TriggerBackgrounded" /* Protocol.Preload.PrerenderFinalStatus.TriggerBackgrounded */:
                return i18nString(UIStrings.prerenderFinalStatusTriggerBackgrounded);
            case "EmbedderTriggeredAndCrossOriginRedirected" /* Protocol.Preload.PrerenderFinalStatus.EmbedderTriggeredAndCrossOriginRedirected */:
                // Not used.
                return i18n.i18n.lockedString('Internal error');
            case "MemoryLimitExceeded" /* Protocol.Preload.PrerenderFinalStatus.MemoryLimitExceeded */:
                return i18nString(UIStrings.prerenderFinalStatusMemoryLimitExceeded);
            case "FailToGetMemoryUsage" /* Protocol.Preload.PrerenderFinalStatus.FailToGetMemoryUsage */:
                return i18nString(UIStrings.prerenderFinalStatusFailToGetMemoryUsage);
            case "DataSaverEnabled" /* Protocol.Preload.PrerenderFinalStatus.DataSaverEnabled */:
                return i18nString(UIStrings.prerenderFinalStatusDataSaverEnabled);
            case "HasEffectiveUrl" /* Protocol.Preload.PrerenderFinalStatus.HasEffectiveUrl */:
                return i18nString(UIStrings.prerenderFinalStatusHasEffectiveUrl);
            case "ActivatedBeforeStarted" /* Protocol.Preload.PrerenderFinalStatus.ActivatedBeforeStarted */:
                // Status for debugging.
                return i18n.i18n.lockedString('Internal error');
            case "InactivePageRestriction" /* Protocol.Preload.PrerenderFinalStatus.InactivePageRestriction */:
                // This looks internal error.
                //
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Internal error');
            case "StartFailed" /* Protocol.Preload.PrerenderFinalStatus.StartFailed */:
                // This looks internal error.
                //
                // TODO(https://crbug.com/1410709): Fill it.
                return i18n.i18n.lockedString('Internal error');
            case "TimeoutBackgrounded" /* Protocol.Preload.PrerenderFinalStatus.TimeoutBackgrounded */:
                return i18nString(UIStrings.prerenderFinalStatusTimeoutBackgrounded);
            case "CrossSiteRedirectInInitialNavigation" /* Protocol.Preload.PrerenderFinalStatus.CrossSiteRedirectInInitialNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusCrossSiteRedirectInInitialNavigation);
            case "CrossSiteNavigationInInitialNavigation" /* Protocol.Preload.PrerenderFinalStatus.CrossSiteNavigationInInitialNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusCrossSiteNavigationInInitialNavigation);
            case "SameSiteCrossOriginRedirectNotOptInInInitialNavigation" /* Protocol.Preload.PrerenderFinalStatus.SameSiteCrossOriginRedirectNotOptInInInitialNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusSameSiteCrossOriginRedirectNotOptInInInitialNavigation);
            case "SameSiteCrossOriginNavigationNotOptInInInitialNavigation" /* Protocol.Preload.PrerenderFinalStatus.SameSiteCrossOriginNavigationNotOptInInInitialNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusSameSiteCrossOriginNavigationNotOptInInInitialNavigation);
            case "ActivationNavigationParameterMismatch" /* Protocol.Preload.PrerenderFinalStatus.ActivationNavigationParameterMismatch */:
                return i18nString(UIStrings.prerenderFinalStatusActivationNavigationParameterMismatch);
            case "ActivatedInBackground" /* Protocol.Preload.PrerenderFinalStatus.ActivatedInBackground */:
                // Status for debugging.
                return i18n.i18n.lockedString('Internal error');
            case "EmbedderHostDisallowed" /* Protocol.Preload.PrerenderFinalStatus.EmbedderHostDisallowed */:
                // Chrome as embedder doesn't use this.
                throw new Error('unreachable');
            case "ActivationNavigationDestroyedBeforeSuccess" /* Protocol.Preload.PrerenderFinalStatus.ActivationNavigationDestroyedBeforeSuccess */:
                // Should not occur in DevTools because tab is alive?
                return i18n.i18n.lockedString('Internal error');
            case "TabClosedByUserGesture" /* Protocol.Preload.PrerenderFinalStatus.TabClosedByUserGesture */:
                // Should not occur in DevTools because tab is alive.
                throw new Error('unreachable');
            case "TabClosedWithoutUserGesture" /* Protocol.Preload.PrerenderFinalStatus.TabClosedWithoutUserGesture */:
                // Should not occur in DevTools because tab is alive.
                throw new Error('unreachable');
            case "PrimaryMainFrameRendererProcessCrashed" /* Protocol.Preload.PrerenderFinalStatus.PrimaryMainFrameRendererProcessCrashed */:
                return i18nString(UIStrings.prerenderFinalStatusPrimaryMainFrameRendererProcessCrashed);
            case "PrimaryMainFrameRendererProcessKilled" /* Protocol.Preload.PrerenderFinalStatus.PrimaryMainFrameRendererProcessKilled */:
                return i18nString(UIStrings.prerenderFinalStatusPrimaryMainFrameRendererProcessKilled);
            case "ActivationFramePolicyNotCompatible" /* Protocol.Preload.PrerenderFinalStatus.ActivationFramePolicyNotCompatible */:
                return i18nString(UIStrings.prerenderFinalStatusActivationFramePolicyNotCompatible);
            case "PreloadingDisabled" /* Protocol.Preload.PrerenderFinalStatus.PreloadingDisabled */:
                return i18nString(UIStrings.prerenderFinalStatusPreloadingDisabled);
            case "BatterySaverEnabled" /* Protocol.Preload.PrerenderFinalStatus.BatterySaverEnabled */:
                return i18nString(UIStrings.prerenderFinalStatusBatterySaverEnabled);
            case "ActivatedDuringMainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.ActivatedDuringMainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusActivatedDuringMainFrameNavigation);
            case "PreloadingUnsupportedByWebContents" /* Protocol.Preload.PrerenderFinalStatus.PreloadingUnsupportedByWebContents */:
                // Chrome as embedder doesn't use this.
                throw new Error('unreachable');
            case "CrossSiteRedirectInMainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.CrossSiteRedirectInMainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusCrossSiteRedirectInMainFrameNavigation);
            case "CrossSiteNavigationInMainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.CrossSiteNavigationInMainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusCrossSiteNavigationInMainFrameNavigation);
            case "SameSiteCrossOriginRedirectNotOptInInMainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.SameSiteCrossOriginRedirectNotOptInInMainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusSameSiteCrossOriginRedirectNotOptInInMainFrameNavigation);
            case "SameSiteCrossOriginNavigationNotOptInInMainFrameNavigation" /* Protocol.Preload.PrerenderFinalStatus.SameSiteCrossOriginNavigationNotOptInInMainFrameNavigation */:
                return i18nString(UIStrings.prerenderFinalStatusSameSiteCrossOriginNavigationNotOptInInMainFrameNavigation);
            case "MemoryPressureOnTrigger" /* Protocol.Preload.PrerenderFinalStatus.MemoryPressureOnTrigger */:
                return i18nString(UIStrings.prerenderFinalStatusMemoryPressureOnTrigger);
            case "MemoryPressureAfterTriggered" /* Protocol.Preload.PrerenderFinalStatus.MemoryPressureAfterTriggered */:
                return i18nString(UIStrings.prerenderFinalStatusMemoryPressureAfterTriggered);
            case "PrerenderingDisabledByDevTools" /* Protocol.Preload.PrerenderFinalStatus.PrerenderingDisabledByDevTools */:
                return i18nString(UIStrings.prerenderFinalStatusPrerenderingDisabledByDevTools);
            case "ResourceLoadBlockedByClient" /* Protocol.Preload.PrerenderFinalStatus.ResourceLoadBlockedByClient */:
                return i18nString(UIStrings.prerenderFinalStatusResourceLoadBlockedByClient);
            default:
                // Note that we use switch and exhaustiveness check to prevent to
                // forget updating these strings, but allow to handle unknown
                // PrerenderFinalStatus at runtime.
                return i18n.i18n.lockedString(`Unknown failure reason: ${prerenderStatus}`);
        }
    }
    // Decoding PrefetchFinalStatus prefetchAttempt to failure description.
    static prefetchAttemptToFailureDescription({ prefetchStatus }) {
        // If you face an error on rolling CDP changes, see
        // https://docs.google.com/document/d/1PnrfowsZMt62PX1EvvTp2Nqs3ji1zrklrAEe1JYbkTk
        switch (prefetchStatus) {
            case null:
                return null;
            // PrefetchNotStarted is mapped to Pending.
            case "PrefetchNotStarted" /* Protocol.Preload.PrefetchStatus.PrefetchNotStarted */:
                return null;
            // PrefetchNotFinishedInTime is mapped to Running.
            case "PrefetchNotFinishedInTime" /* Protocol.Preload.PrefetchStatus.PrefetchNotFinishedInTime */:
                return null;
            // PrefetchResponseUsed is mapped to Success.
            case "PrefetchResponseUsed" /* Protocol.Preload.PrefetchStatus.PrefetchResponseUsed */:
                return null;
            // Holdback related status is expected to be overridden when DevTools is
            // opened.
            case "PrefetchAllowed" /* Protocol.Preload.PrefetchStatus.PrefetchAllowed */:
            case "PrefetchHeldback" /* Protocol.Preload.PrefetchStatus.PrefetchHeldback */:
                return null;
            // TODO(https://crbug.com/1410709): deprecate PrefetchSuccessfulButNotUsed in the protocol.
            case "PrefetchSuccessfulButNotUsed" /* Protocol.Preload.PrefetchStatus.PrefetchSuccessfulButNotUsed */:
                return null;
            case "PrefetchFailedIneligibleRedirect" /* Protocol.Preload.PrefetchStatus.PrefetchFailedIneligibleRedirect */:
                return PrefetchReasonDescription['PrefetchFailedIneligibleRedirect'].name();
            case "PrefetchFailedInvalidRedirect" /* Protocol.Preload.PrefetchStatus.PrefetchFailedInvalidRedirect */:
                return PrefetchReasonDescription['PrefetchFailedInvalidRedirect'].name();
            case "PrefetchFailedMIMENotSupported" /* Protocol.Preload.PrefetchStatus.PrefetchFailedMIMENotSupported */:
                return PrefetchReasonDescription['PrefetchFailedMIMENotSupported'].name();
            case "PrefetchFailedNetError" /* Protocol.Preload.PrefetchStatus.PrefetchFailedNetError */:
                return PrefetchReasonDescription['PrefetchFailedNetError'].name();
            case "PrefetchFailedNon2XX" /* Protocol.Preload.PrefetchStatus.PrefetchFailedNon2XX */:
                return PrefetchReasonDescription['PrefetchFailedNon2XX'].name();
            case "PrefetchFailedPerPageLimitExceeded" /* Protocol.Preload.PrefetchStatus.PrefetchFailedPerPageLimitExceeded */:
                return PrefetchReasonDescription['PrefetchFailedPerPageLimitExceeded'].name();
            case "PrefetchIneligibleRetryAfter" /* Protocol.Preload.PrefetchStatus.PrefetchIneligibleRetryAfter */:
                return PrefetchReasonDescription['PrefetchIneligibleRetryAfter'].name();
            case "PrefetchEvicted" /* Protocol.Preload.PrefetchStatus.PrefetchEvicted */:
                return PrefetchReasonDescription['PrefetchEvicted'].name();
            case "PrefetchIsPrivacyDecoy" /* Protocol.Preload.PrefetchStatus.PrefetchIsPrivacyDecoy */:
                return PrefetchReasonDescription['PrefetchIsPrivacyDecoy'].name();
            case "PrefetchIsStale" /* Protocol.Preload.PrefetchStatus.PrefetchIsStale */:
                return PrefetchReasonDescription['PrefetchIsStale'].name();
            case "PrefetchNotEligibleBrowserContextOffTheRecord" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleBrowserContextOffTheRecord */:
                return PrefetchReasonDescription['PrefetchNotEligibleBrowserContextOffTheRecord'].name();
            case "PrefetchNotEligibleDataSaverEnabled" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleDataSaverEnabled */:
                return PrefetchReasonDescription['PrefetchNotEligibleDataSaverEnabled'].name();
            case "PrefetchNotEligibleExistingProxy" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleExistingProxy */:
                return PrefetchReasonDescription['PrefetchNotEligibleExistingProxy'].name();
            case "PrefetchNotEligibleHostIsNonUnique" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleHostIsNonUnique */:
                return PrefetchReasonDescription['PrefetchNotEligibleHostIsNonUnique'].name();
            case "PrefetchNotEligibleNonDefaultStoragePartition" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleNonDefaultStoragePartition */:
                return PrefetchReasonDescription['PrefetchNotEligibleNonDefaultStoragePartition'].name();
            case "PrefetchNotEligibleSameSiteCrossOriginPrefetchRequiredProxy" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleSameSiteCrossOriginPrefetchRequiredProxy */:
                return PrefetchReasonDescription['PrefetchNotEligibleSameSiteCrossOriginPrefetchRequiredProxy'].name();
            case "PrefetchNotEligibleSchemeIsNotHttps" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleSchemeIsNotHttps */:
                return PrefetchReasonDescription['PrefetchNotEligibleSchemeIsNotHttps'].name();
            case "PrefetchNotEligibleUserHasCookies" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleUserHasCookies */:
                return PrefetchReasonDescription['PrefetchNotEligibleUserHasCookies'].name();
            case "PrefetchNotEligibleUserHasServiceWorker" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleUserHasServiceWorker */:
                return PrefetchReasonDescription['PrefetchNotEligibleUserHasServiceWorker'].name();
            case "PrefetchNotUsedCookiesChanged" /* Protocol.Preload.PrefetchStatus.PrefetchNotUsedCookiesChanged */:
                return PrefetchReasonDescription['PrefetchNotUsedCookiesChanged'].name();
            case "PrefetchProxyNotAvailable" /* Protocol.Preload.PrefetchStatus.PrefetchProxyNotAvailable */:
                return PrefetchReasonDescription['PrefetchProxyNotAvailable'].name();
            case "PrefetchNotUsedProbeFailed" /* Protocol.Preload.PrefetchStatus.PrefetchNotUsedProbeFailed */:
                return PrefetchReasonDescription['PrefetchNotUsedProbeFailed'].name();
            case "PrefetchNotEligibleBatterySaverEnabled" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligibleBatterySaverEnabled */:
                return PrefetchReasonDescription['PrefetchNotEligibleBatterySaverEnabled'].name();
            case "PrefetchNotEligiblePreloadingDisabled" /* Protocol.Preload.PrefetchStatus.PrefetchNotEligiblePreloadingDisabled */:
                return PrefetchReasonDescription['PrefetchNotEligiblePreloadingDisabled'].name();
            default:
                // Note that we use switch and exhaustiveness check to prevent to
                // forget updating these strings, but allow to handle unknown
                // PrefetchStatus at runtime.
                return i18n.i18n.lockedString(`Unknown failure reason: ${prefetchStatus}`);
        }
    }
}
const coordinator = Coordinator.RenderCoordinator.RenderCoordinator.instance();
export class PreloadingDetailsReportView extends LegacyWrapper.LegacyWrapper.WrappableComponent {
    static litTagName = LitHtml.literal `devtools-resources-preloading-details-report-view`;
    #shadow = this.attachShadow({ mode: 'open' });
    #data = null;
    connectedCallback() {
        this.#shadow.adoptedStyleSheets = [preloadingDetailsReportViewStyles];
    }
    set data(data) {
        this.#data = data;
        void this.#render();
    }
    async #render() {
        await coordinator.write('PreloadingDetailsReportView render', () => {
            if (this.#data === null) {
                // Disabled until https://crbug.com/1079231 is fixed.
                // clang-format off
                LitHtml.render(LitHtml.html `
          <div class="preloading-noselected">
            <div>
              <p>${i18nString(UIStrings.selectAnElementForMoreDetails)}</p>
            </div>
          </div>
        `, this.#shadow, { host: this });
                // clang-format on
                return;
            }
            const action = PreloadingUIUtils.action(this.#data.preloadingAttempt);
            const detailedStatus = PreloadingUIUtils.detailedStatus(this.#data.preloadingAttempt);
            // Disabled until https://crbug.com/1079231 is fixed.
            // clang-format off
            LitHtml.render(LitHtml.html `
        <${ReportView.ReportView.Report.litTagName} .data=${{ reportTitle: 'Preloading Attempt' }}>
          <${ReportView.ReportView.ReportSectionHeader.litTagName}>${i18nString(UIStrings.detailsDetailedInformation)}</${ReportView.ReportView.ReportSectionHeader.litTagName}>

          <${ReportView.ReportView.ReportKey.litTagName}>${i18n.i18n.lockedString('URL')}</${ReportView.ReportView.ReportKey.litTagName}>
          <${ReportView.ReportView.ReportValue.litTagName}>
            <div class="text-ellipsis" title=${this.#data.preloadingAttempt.key.url}>${this.#data.preloadingAttempt.key.url}</div>
          </${ReportView.ReportView.ReportValue.litTagName}>

          <${ReportView.ReportView.ReportKey.litTagName}>${i18nString(UIStrings.detailsAction)}</${ReportView.ReportView.ReportKey.litTagName}>
          <${ReportView.ReportView.ReportValue.litTagName}>
            <div class="text-ellipsis" title="">
              ${action}
            </div>
          </${ReportView.ReportView.ReportValue.litTagName}>

          <${ReportView.ReportView.ReportKey.litTagName}>${i18nString(UIStrings.detailsStatus)}</${ReportView.ReportView.ReportKey.litTagName}>
          <${ReportView.ReportView.ReportValue.litTagName}>
            ${detailedStatus}
          </${ReportView.ReportView.ReportValue.litTagName}>

          ${this.#maybePrefetchFailureReason()}
          ${this.#maybePrerenderFailureReason()}

          ${this.#data.ruleSets.map(ruleSet => this.#renderRuleSet(ruleSet))}
        </${ReportView.ReportView.Report.litTagName}>
      `, this.#shadow, { host: this });
            // clang-format on
        });
    }
    #maybePrefetchFailureReason() {
        assertNotNullOrUndefined(this.#data);
        const attempt = this.#data.preloadingAttempt;
        if (attempt.action !== "Prefetch" /* Protocol.Preload.SpeculationAction.Prefetch */) {
            return LitHtml.nothing;
        }
        const failureDescription = PreloadingUIUtils.prefetchAttemptToFailureDescription(attempt);
        if (failureDescription === null) {
            return LitHtml.nothing;
        }
        return LitHtml.html `
        <${ReportView.ReportView.ReportKey.litTagName}>${i18nString(UIStrings.detailsFailureReason)}</${ReportView.ReportView.ReportKey.litTagName}>
        <${ReportView.ReportView.ReportValue.litTagName}>
          ${failureDescription}
        </${ReportView.ReportView.ReportValue.litTagName}>
    `;
    }
    #maybePrerenderFailureReason() {
        assertNotNullOrUndefined(this.#data);
        const attempt = this.#data.preloadingAttempt;
        if (attempt.action !== "Prerender" /* Protocol.Preload.SpeculationAction.Prerender */) {
            return LitHtml.nothing;
        }
        const failureReason = PreloadingUIUtils.failureReason(attempt);
        if (failureReason === null) {
            return LitHtml.nothing;
        }
        return LitHtml.html `
        <${ReportView.ReportView.ReportKey.litTagName}>${i18nString(UIStrings.detailsFailureReason)}</${ReportView.ReportView.ReportKey.litTagName}>
        <${ReportView.ReportView.ReportValue.litTagName}>
          ${failureReason}
        </${ReportView.ReportView.ReportValue.litTagName}>
    `;
    }
    #renderRuleSet(ruleSet) {
        // We can assume `sourceText` is a valid JSON because this triggered the preloading attempt.
        const json = JSON.stringify(JSON.parse(ruleSet.sourceText));
        // Disabled until https://crbug.com/1079231 is fixed.
        // clang-format off
        return LitHtml.html `
          <${ReportView.ReportView.ReportKey.litTagName}>${i18nString(UIStrings.detailsRuleSet)}</${ReportView.ReportView.ReportKey.litTagName}>
          <${ReportView.ReportView.ReportValue.litTagName}>
            <div class="text-ellipsis" title="">
              ${json}
            </div>
          </${ReportView.ReportView.ReportValue.litTagName}>
    `;
        // clang-format on
    }
}
ComponentHelpers.CustomElements.defineComponent('devtools-resources-preloading-details-report-view', PreloadingDetailsReportView);
//# sourceMappingURL=PreloadingDetailsReportView.js.map