[![npm version](https://badge.fury.io/js/cordova-plugin-add-swift-support.svg)](https://badge.fury.io/js/cordova-plugin-add-swift-support) [![Build Status](https://travis-ci.org/akofman/cordova-plugin-add-swift-support.svg?branch=master)](https://travis-ci.org/akofman/cordova-plugin-add-swift-support) [![npm](https://img.shields.io/npm/dm/cordova-plugin-add-swift-support.svg)]()

# cordova-plugin-add-swift-support

![swift-128x128](https://cloud.githubusercontent.com/assets/579922/15999501/79196b48-3146-11e6-836e-061a7ef53571.png)

This [Cordova plugin](https://www.npmjs.com/package/cordova-plugin-add-swift-support) adds the Swift support to your iOS project.

## Installation

You can add this plugin directly to your project:

`cordova plugin add cordova-plugin-add-swift-support --save`

Or add it as a dependency into your own plugin:

`<dependency id="cordova-plugin-add-swift-support" version="2.0.2"/>`

By default, the Swift 4 support is added but the legacy version (2.3) can still be configured as a preference, inside the project's `config.xml`, within the `<platform name="ios">` section:

`<preference name="UseLegacySwiftLanguageVersion" value="true" />`

Or it is possible to specify the version as following, inside the project's `config.xml`, within the `<platform name="ios">` section:

`<preference name="UseSwiftLanguageVersion" value="5" />`

If needed, add a prefixed Bridging-Header file in your plugin in order to import frameworks (MyPlugin-Bridging-Header.h for instance).
As an example you can have a look at this [plugin](https://github.com/akofman/cordova-plugin-permissionScope).

If the `cordova-plugin-add-swift-support` plugin is already installed to your project, then you can add your own Swift plugin as usual, its prefixed Bridging-Header will be automatically found and merged.

## Contributing

The src folder contains ECMAScript 2015 source files, the minimum Node.js version is `6` (Boron).

## License

MIT
