# cordova-plugin-crosswalk-webview-v3

This is a fork of original [cordova-plugin-crosswalk-webview](https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview) library, which aims to provide compatibility with latest cordova versions.

Since there is still a lot of android devices with legacy webview (before 7.0), crosswalk-webview project still makes sense for android. 

For detailed information about crosswalk, please visit the homepage of original library. 

### IMPORTANT NOTICES

- Crosswalk does not work for Android 10+ devices. I recommend you split the app in 2 different projects, one with crosswalk and one without. The one without crosswalk must support minimum android 7 (sdk level 26).

Config.xml must have:

    <platform name="android">
        <preference name="android-minSdkVersion" value="26" />
    </platform>

platforms/android/gradle.properties must have:

    cdvMinSdkVersion=26


### Install

* Add this plugin

```
$ cordova plugin add cordova-plugin-crosswalk-webview-v3
```

* Build
```
$ cordova build android
```
The build script will automatically fetch the Crosswalk WebView libraries from Crosswalk project download site (https://download.01.org/crosswalk/releases/crosswalk/android/maven2/) and build for both X86 and ARM architectures by default.


To build Crosswalk-enabled 32-bit apks for release:

    $ cordova build --release

It will generate following apks:

```
platforms/android/app/build/outputs/apk/armv7/release/app-armv7-release-unsigned.apk
platforms/android/app/build/outputs/apk/x86/release/app-x86-release-unsigned.apk
```

Google changed some policies at 2019 August, and now every app in the market requires 64-bit apks. To do that:

    $ cordova build --release --xwalk64bit

It will generate following apks:

```
platforms/android/app/build/outputs/apk/arm64/release/app-arm64-release-unsigned.apk
platforms/android/app/build/outputs/apk/x86_64/release/app-x86_64-release-unsigned.apk
```

The above apks will be build for each architecture separately only if multiple akps are configured as below in config.xml:

```
<preference name="xwalkMultipleApk" value="true" />
```

If you don't need to support older devices with 32bit architectures, you should only build for 64-bit, sign and upload them to play store.
However, if there are still older devices running your app, you must build and sign all 4 of them and upload each to play store.

Check this gist to build all of them in one bash script: (<https://gist.github.com/ardabeyazoglu/ff505d06bd576b966ad7f1c932f7c6ed>)

### Release Notes

#### 3.0.4 (October 25, 2020)
* Replaced unsupported gradle function in merge task (fixes [#12](https://github.com/ardabeyazoglu/cordova-plugin-crosswalk-webview-v3/issues/12))

#### 3.0.3 (February 18, 2020)
* Changed 64bit product flavors to arm64 only, with a versionCode*10 + 4.

#### 3.0.2 (November 10, 2019)
* Added compatibility with cordova 9
* Fixed version code calculation for 64bit builds (aligned them with 32bit build codes)

#### 2.4.0 (January 18, 2018)
* Keep compatibility with cordova-android 7.0 project structure

#### 2.3.0 (January 21, 2017)
* Uses the latest Crosswalk 23 stable version by default

#### 2.2.0 (November 4, 2016)
* Uses the latest Crosswalk 22 stable version by default
* Keep compatible for Cordova-android 6.0 with evaluating Javascript bridge
* This version requires cordova-android 6.0.0 or newer

#### 2.1.0 (September 9, 2016)
* Uses the latest Crosswalk 21 stable version by default

#### 2.0.0 (August 17, 2016)
* Uses the latest Crosswalk 20 stable version by default
* Discontinue support for Android 4.0 (ICS) in Crosswalk starting with version 20

#### 1.8.0 (June 30, 2016)
* Uses the latest Crosswalk 19 stable version by default

#### 1.7.0 (May 4, 2016)
* Uses the latest Crosswalk 18 stable version by default
* Support to use [Crosswalk Lite](https://crosswalk-project.org/documentation/crosswalk_lite.html), It's possible to specify lite value with the variable of XWALK_MODE at install plugin time.
* [Cordova screenshot plugin](https://github.com/gitawego/cordova-screenshot.git) can capture the visible content of web page with Crosswalk library.
* Doesn't work with Crosswalk 17 and earlier

#### 1.6.0 (March 11, 2016)
* Uses the latest Crosswalk 17 stable version by default
* Support to [package apps for 64-bit devices](https://crosswalk-project.org/documentation/android/android_64bit.html), it's possible to specify 64-bit targets using the `--xwalk64bit` option in the build command:

        cordova build android --xwalk64bit

#### 1.5.0 (January 18, 2016)
* Uses the latest Crosswalk 16 stable version by default
* The message of xwalk's ready can be listened

#### 1.4.0 (November 5, 2015)
* Uses the latest Crosswalk 15 stable version by default
* Support User Agent and Background Color configuration preferences
* Compatible with the newest Cordova version 5.3.4

#### 1.3.0 (August 28, 2015)
* Crosswalk variables can be configured as an option via CLI
* Support for [Crosswalk's shared mode](https://crosswalk-project.org/documentation/shared_mode.html) via the XWALK_MODE install variable or xwalkMode preference
* Uses the latest Crosswalk 14 stable version by default
* The ANIMATABLE_XWALK_VIEW preference is false by default
* Doesn't work with Crosswalk 14.43.343.17 and earlier

#### 1.2.0 (April 22, 2015)
* Made Crosswalk command-line configurable via `<preference name="xwalkCommandLine" value="..." />`
* Disabled pull-down-to-refresh by default

#### 1.1.0 (April 21, 2015)
* Based on Crosswalk v13
* Made Crosswalk version configurable via `<preference name="xwalkVersion" value="..." />`

#### 1.0.0 (Mar 25, 2015)
* Initial release
* Based on Crosswalk v11
