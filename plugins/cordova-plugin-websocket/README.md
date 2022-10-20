# cordova-plugin-websocket [![GitHub version](https://badge.fury.io/gh/knowledgecode%2FWebSocket-for-Android.svg)](http://badge.fury.io/gh/knowledgecode%2FWebSocket-for-Android)
This is a Cordova plugin, which is being developed based on [Jetty 8](https://github.com/eclipse/jetty.project/tree/jetty-8), makes WebSocket (RFC6455) available on Android.  

## Requirements
 - Android 2.3 or later (recommended 4.1 or later)  
 - `cordova-android@3.6.0` or later, or compatible framework  
 - `cordova-plugin-whitelist` or `cordova-plugin-legacy-whitelist` if using `cordova-android@4.0.0` and later  

The plugin for Cordova 2.x can be found [here](https://github.com/knowledgecode/WebSocket-for-Android/tree/2.x). However it is no longer maintained.  

## Supported Features
| version         | WS protocol | WSS protocol | text message | binary message |
|:---------------:|:-----------:|:------------:|:------------:|:--------------:|
| 2.3.x (API 10)  | ✓          | ✓           | ✓           |                |
| 4.0.x (API 14)  | ✓          | ✓           | ✓           | ✓             |
| 4.0.x (API 15)  | ✓          | ✓           | ✓           | ✓             |
| 4.1.x (API 16)  | ✓          | ✓           | ✓           | ✓             |
| 4.2.x (API 17)  | ✓          | ✓           | ✓           | ✓             |
| 4.3.x (API 18)  | ✓          | ✓           | ✓           | ✓             |
| 4.4.x and later | -           | -            | -            | -              |

#### Notes
 - Since Android 4.4.x (KitKat) and later support WebSocket, this plugin is **NOT** used on there by default.  
 - WSS protocol is only supported TLS. SSL is not supported.  
 - Android 3.x (Honeycomb) are not supported (might work but is not tested).  
 - In `cordova-android@4.0.0` and later, this plugin can be used together with [Crosswalk](https://crosswalk-project.org/). In this case also it is not used on there by default since that supports WebSocket.  
 - In order to support Android 5.x (Lollipop) and later, would be better to build with `cordova-android@3.7.1` or later.  

## Installation
Use Cordova Command-Line Interface (CLI). At first check your CLI version:
```sh
$ cordova --version
5.0.0
```
If using 5.0.0 and later, you can install it via npm:
```sh
$ cordova plugin add cordova-plugin-websocket
```
If using other old versions, you can install it via GitHub:
```sh
$ cordova plugin add https://github.com/knowledgecode/WebSocket-for-Android.git
```

#### Setting a Content-Security-Policy (CSP)
`cordova-android@4.0.0` and later support SCP. In order to permit WebSocket access using `cordova-plugin-whitelist`, append `connect-src` directive in `index.html`:
```html
connect-src ws://example.com wss://example.com
```
For example:
```html
<head>
  <meta http-equiv="Content-Security-Policy" content="default-src 'self' data: gap: https://ssl.gstatic.com 'unsafe-eval'; style-src 'self' 'unsafe-inline'; media-src *; connect-src ws://example.com wss://example.com">
```

## Upgrading from previous versions
Remove and then reinstall:
```sh
$ cordova plugin rm cordova-plugin-websocket
$ cordova plugin add cordova-plugin-websocket
```

#### Caveats
- When install this plugin, it adds `INTERNET` permission to `platforms/android/AndroidManifest.xml`. If remove this plugin, the permission is also removed at the same time even if it is required for other plugins.  
- It has not supported `cordova-android@3.5.x` and earlier since v0.12.0. Please make sure Android platform version is more than that:
```sh
$ cordova platform
Installed platforms: android 4.1.1
```

## Usage
### *WebSocket(url[, protocols])*
The WebSocket(url, protocols) constructor takes one or two arguments. The first argument, url, specifies the URL to which to connect. The second, protocols, is either a string or an array of strings.  
A simple code is as follows:  
```javascript
document.addEventListener('deviceready', function () {
    var ws = new WebSocket('ws://echo.websocket.org');

    ws.onopen = function () {
        console.log('open');
        this.send('hello');         // transmit "hello" after connecting
    };

    ws.onmessage = function (event) {
        console.log(event.data);    // will be "hello"
        this.close();
    };

    ws.onerror = function () {
        console.log('error occurred!');
    };

    ws.onclose = function (event) {
        console.log('close code=' + event.code);
    };
}, false);
```
#### Options
This plugin has the following options. All these parameters are optional. Of course these don't affect built-in WebSocket.  

| key                  | type    | default value       | supported version        |
|:---------------------|:--------|:--------------------|:-------------------------|
| origin               | string  | file:// (usually)   | >=v0.3.0                 |
| maxConnectTime       | number  | 75000               | >=v0.4.0                 |
| maxTextMessageSize   | number  | -1                  | >=v0.4.0 (except v0.8.x) |
| maxBinaryMessageSize | number  | -1                  | >=v0.4.0 (except v0.8.x) |
| override             | boolean | false               | >=v0.8.0                 |
| agent                | string  | (depends on device) | >=v0.9.0                 |
| perMessageDeflate    | boolean | true                | >=v0.10.0                |

`origin` is a value to set a request header field. Default value is usually `file://`. This is the same value as when using built-in WebSocket.  

`maxConnectTime` is time to wait for connection. A unit is millisecond.  

`maxTextMessageSize` and `maxBinaryMessageSize` are receivable maximum size from a server. Default value is -1 (unlimited. depends on heap size of devices). A unit is byte.  

`override` is a flag to force WebView to use this plugin even if it supports WebSocket. However in most cases it will be slower than built-in WebSocket.  

`agent` is user-agent to set a request header field. Default value depends on devices. This is the same value as when using built-in WebSocket.  

`perMessageDeflate` is a flag whether to use permessage-deflate extension. Default value is true. Sends data with compression if a server also supports permessage-deflate. However if mainly sending compressed binary like JPEG images, recommended to set to false.  

If change these parameters, need to do before creating a instance:  
```javascript
WebSocket.pluginOptions = {
    origin: 'http://example.com',
    maxConnectTime: 5000,
    override: true
};

var ws = new WebSocket('ws://echo.websocket.org');
```
### *send(data)*
Transmits data to the server over the WebSocket connection. The data takes a string, a blob, or an arraybuffer.  

#### Notes
An upper limit of the message size depends on heap size of devices. It would be better to consider a way to split the message if it is quite large.  
### *close([code[, reason]])*
Closes the WebSocket connection or connection attempt, if any.  

### For debug
This plugin has been available logging for debug since v0.12.0. The logging level can be adjusted with `config.xml` that is in your project directory:  
```xml
<platform name="android">
    <preference name="LogLevel" value="DEBUG" />
</platform>
```
If don't specify this parameter, default level is `DEBUG`. So at first you may be surprised to see many debug logs on logcat. To stop logs except errors, change the level to `ERROR`.  

## Change Log
See [CHANGELOG.md](https://github.com/knowledgecode/WebSocket-for-Android/blob/master/CHANGELOG.md).

## License
This plugin is available under the terms of the Apache License Version 2.0.
