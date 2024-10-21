// Copyright 2023 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
// IMPORTANT: this file is auto generated. Please do not edit this file.
/* istanbul ignore file */
const styles = new CSSStyleSheet();
styles.replaceSync(
`/*
 * Copyright (C) 2013 Google Inc. All rights reserved.
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

.screencast {
  overflow: hidden;
}

.screencast-navigation {
  flex-direction: row;
  display: flex;
  flex: 24px 0 0;
  position: relative;
  padding-left: 1px;
  border-bottom: 1px solid var(--color-details-hairline);
  background-origin: padding-box;
  background-clip: padding-box;
}

.screencast-navigation button {
  border-radius: 2px;
  background-color: transparent;
  background-image:
    image-set(
      var(--image-file-navigationControls) 1x,
      var(--image-file-navigationControls_2x) 2x
    );
  background-clip: content-box;
  background-origin: content-box;
  background-repeat: no-repeat;
  border: 1px solid transparent;
  height: 23px;
  padding: 2px;
  width: 23px;
}

.screencast-navigation button:hover,
.screencast-navigation button:focus {
  border-color: var(--legacy-accent-color-hover);
}

.screencast-navigation button:active {
  border-color: var(--color-background-elevation-2);
}

.screencast-navigation button[disabled] {
  opacity: 50%;
}

.screencast-navigation button.back {
  background-position-x: -1px;
}

.screencast-navigation button.forward {
  background-position-x: -18px;
}

.screencast-navigation button.reload {
  background-position-x: -37px;
}

.screencast-navigation input {
  flex: 1;
  margin: 2px;
  max-height: 19px;
}

.screencast-navigation .progress {
  --override-progress-background-color: rgb(66 129 235);

  background-color: var(--override-progress-background-color);
  height: 3px;
  left: 0;
  position: absolute;
  top: 100%;  /* Align with the bottom edge of the parent. */
  width: 0;
  z-index: 2;  /* Above .screencast-glasspane. */
}

.-theme-with-dark-background .screencast-navigation .progress,
:host-context(.-theme-with-dark-background) .screencast-navigation .progress {
  --override-progress-background-color: rgb(20 83 189);
}

.screencast-viewport {
  display: flex;
  border: 1px solid var(--color-details-hairline);
  border-radius: 20px;
  flex: none;
  padding: 20px;
  margin: 10px;
  background-color: var(--color-background-elevation-2);
}

.screencast-canvas-container {
  flex: auto;
  display: flex;
  border: 1px solid var(--color-details-hairline);
  position: relative;
  cursor: image-set(var(--image-file-touchCursor) 1x, var(--image-file-touchCursor_2x) 2x), default;
}

.screencast canvas {
  flex: auto;
  position: relative;
}

.screencast-element-title {
  position: absolute;
  z-index: 10;
}

.screencast-tag-name {
  color: var(--color-token-tag);
}

.screencast-attribute {
  color: var(--color-token-attribute);
}

.screencast-dimension {
  --override-dimension-color: hsl(0deg 0% 45%);
  /* Keep this in sync with tool_highlight.css (.dimensions) */
  color: var(--override-dimension-color);
}

.screencast-glasspane {
  background-color: var(--color-background-opacity-80);
  font-size: 30px;
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: center;
}

/*# sourceURL=screencastView.css */
`);
export default styles;
