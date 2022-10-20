/*
* This hook adds all the needed config to implement a Cordova plugin with Swift.
*
*  - It adds a Bridging header importing Cordova/CDV.h if it's not already
*    the case. Else it concats all the bridging headers in one single file.
*
*    /!\ Please be sure not naming your bridging header file 'Bridging-Header.h'
*    else it won't be supported.
*
*  - It puts the ios deployment target to 7.0 in case your project would have a
*    lesser one.
*
*  - It updates the ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES build setting to YES.
*
*  - It updates the SWIFT_VERSION to 4.0.
*/

const fs = require('fs');
const path = require('path');
const xcode = require('xcode');
const childProcess = require('child_process');
const semver = require('semver');
const glob = require('glob');

module.exports = context => {
  const projectRoot = context.opts.projectRoot;

  // This script has to be executed depending on the command line arguments, not
  // on the hook execution cycle.
  if ((context.hook === 'after_platform_add' && context.cmdLine.includes('platform add')) ||
    (context.hook === 'after_prepare' && context.cmdLine.includes('prepare')) ||
    (context.hook === 'after_plugin_add' && context.cmdLine.includes('plugin add'))) {
    getPlatformVersionsFromFileSystem(context, projectRoot).then(platformVersions => {
      const IOS_MIN_DEPLOYMENT_TARGET = '7.0';
      const platformPath = path.join(projectRoot, 'platforms', 'ios');
      const config = getConfigParser(context, path.join(projectRoot, 'config.xml'));

      let bridgingHeaderPath;
      let bridgingHeaderContent;
      let projectName;
      let projectPath;
      let pluginsPath;
      let iosPlatformVersion;
      let pbxprojPath;
      let xcodeProject;

      const COMMENT_KEY = /_comment$/;
      let buildConfigs;
      let buildConfig;
      let configName;

      platformVersions.forEach((platformVersion) => {
        if (platformVersion.platform === 'ios') {
          iosPlatformVersion = platformVersion.version;
        }
      });

      if (!iosPlatformVersion) {
        return;
      }

      projectName = config.name();
      projectPath = path.join(platformPath, projectName);
      pbxprojPath = path.join(platformPath, projectName + '.xcodeproj', 'project.pbxproj');
      xcodeProject = xcode.project(pbxprojPath);
      pluginsPath = path.join(projectPath, 'Plugins');

      xcodeProject.parseSync();

      bridgingHeaderPath = getBridgingHeaderPath(projectPath, iosPlatformVersion);

      try {
        fs.statSync(bridgingHeaderPath);
      } catch (err) {
        // If the bridging header doesn't exist, we create it with the minimum
        // Cordova/CDV.h import.
        bridgingHeaderContent = ['//',
          '//  Use this file to import your target\'s public headers that you would like to expose to Swift.',
          '//',
          '#import <Cordova/CDV.h>'];
        fs.writeFileSync(bridgingHeaderPath, bridgingHeaderContent.join('\n'), { encoding: 'utf-8', flag: 'w' });
        xcodeProject.addHeaderFile('Bridging-Header.h');
      }

      buildConfigs = xcodeProject.pbxXCBuildConfigurationSection();

      const bridgingHeaderProperty = '"$(PROJECT_DIR)/$(PROJECT_NAME)' + bridgingHeaderPath.split(projectPath)[1] + '"';

      for (configName in buildConfigs) {
        if (!COMMENT_KEY.test(configName)) {
          buildConfig = buildConfigs[configName];
          if (xcodeProject.getBuildProperty('SWIFT_OBJC_BRIDGING_HEADER', buildConfig.name) !== bridgingHeaderProperty) {
            xcodeProject.updateBuildProperty('SWIFT_OBJC_BRIDGING_HEADER', bridgingHeaderProperty, buildConfig.name);
            console.log('Update IOS build setting SWIFT_OBJC_BRIDGING_HEADER to:', bridgingHeaderProperty, 'for build configuration', buildConfig.name);
          }
        }
      }

      // Look for any bridging header defined in the plugin
      glob('**/*Bridging-Header*.h', { cwd: pluginsPath }, (error, files) => {
        const bridgingHeader = path.basename(bridgingHeaderPath);
        const headers = files.map((filePath) => path.basename(filePath));

        // if other bridging headers are found, they are imported in the
        // one already configured in the project.
        let content = fs.readFileSync(bridgingHeaderPath, 'utf-8');

        if (error) throw new Error(error);

        headers.forEach((header) => {
          if (header !== bridgingHeader && !~content.indexOf(header)) {
            if (content.charAt(content.length - 1) !== '\n') {
              content += '\n';
            }
            content += '#import "' + header + '"\n';
            console.log('Importing', header, 'into', bridgingHeaderPath);
          }
        });
        fs.writeFileSync(bridgingHeaderPath, content, 'utf-8');

        for (configName in buildConfigs) {
          if (!COMMENT_KEY.test(configName)) {
            buildConfig = buildConfigs[configName];
            if (parseFloat(xcodeProject.getBuildProperty('IPHONEOS_DEPLOYMENT_TARGET', buildConfig.name)) < parseFloat(IOS_MIN_DEPLOYMENT_TARGET)) {
              xcodeProject.updateBuildProperty('IPHONEOS_DEPLOYMENT_TARGET', IOS_MIN_DEPLOYMENT_TARGET, buildConfig.name);
              console.log('Update IOS project deployment target to:', IOS_MIN_DEPLOYMENT_TARGET, 'for build configuration', buildConfig.name);
            }

            if (xcodeProject.getBuildProperty('ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES', buildConfig.name) !== 'YES') {
              xcodeProject.updateBuildProperty('ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES', 'YES', buildConfig.name);
              console.log('Update IOS build setting ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES to: YES', 'for build configuration', buildConfig.name);
            }

            if (xcodeProject.getBuildProperty('LD_RUNPATH_SEARCH_PATHS', buildConfig.name) !== '"@executable_path/Frameworks"') {
              xcodeProject.updateBuildProperty('LD_RUNPATH_SEARCH_PATHS', '"@executable_path/Frameworks"', buildConfig.name);
              console.log('Update IOS build setting LD_RUNPATH_SEARCH_PATHS to: @executable_path/Frameworks', 'for build configuration', buildConfig.name);
            }

            if (typeof xcodeProject.getBuildProperty('SWIFT_VERSION', buildConfig.name) === 'undefined') {
              if (config.getPreference('UseLegacySwiftLanguageVersion', 'ios')) {
                xcodeProject.updateBuildProperty('SWIFT_VERSION', '2.3', buildConfig.name);
                console.log('Use legacy Swift language version', buildConfig.name);
              } else if (config.getPreference('UseSwiftLanguageVersion', 'ios')) {
                const swiftVersion = config.getPreference('UseSwiftLanguageVersion', 'ios');
                xcodeProject.updateBuildProperty('SWIFT_VERSION', swiftVersion, buildConfig.name);
                console.log('Use Swift language version', swiftVersion);
              } else {
                xcodeProject.updateBuildProperty('SWIFT_VERSION', '4.0', buildConfig.name);
                console.log('Update SWIFT version to 4.0', buildConfig.name);
              }
            }

            if (buildConfig.name === 'Debug') {
              if (xcodeProject.getBuildProperty('SWIFT_OPTIMIZATION_LEVEL', buildConfig.name) !== '"-Onone"') {
                xcodeProject.updateBuildProperty('SWIFT_OPTIMIZATION_LEVEL', '"-Onone"', buildConfig.name);
                console.log('Update IOS build setting SWIFT_OPTIMIZATION_LEVEL to: -Onone', 'for build configuration', buildConfig.name);
              }
            }
          }
        }

        fs.writeFileSync(pbxprojPath, xcodeProject.writeSync());
      });
    });
  }
};

const getConfigParser = (context, configPath) => {
  let ConfigParser;

  if (semver.lt(context.opts.cordova.version, '5.4.0')) {
    ConfigParser = context.requireCordovaModule('cordova-lib/src/ConfigParser/ConfigParser');
  } else {
    ConfigParser = context.requireCordovaModule('cordova-common/src/ConfigParser/ConfigParser');
  }

  return new ConfigParser(configPath);
};

const getBridgingHeaderPath = (projectPath, iosPlatformVersion) => {
  let bridgingHeaderPath;
  if (semver.lt(iosPlatformVersion, '4.0.0')) {
    bridgingHeaderPath = path.posix.join(projectPath, 'Plugins', 'Bridging-Header.h');
  } else {
    bridgingHeaderPath = path.posix.join(projectPath, 'Bridging-Header.h');
  }

  return bridgingHeaderPath;
};

const getPlatformVersionsFromFileSystem = (context, projectRoot) => {
  const cordovaUtil = context.requireCordovaModule('cordova-lib/src/cordova/util');
  const platformsOnFs = cordovaUtil.listPlatforms(projectRoot);
  const platformVersions = platformsOnFs.map(platform => {
    const script = path.join(projectRoot, 'platforms', platform, 'cordova', 'version');
    return new Promise((resolve, reject) => {
      childProcess.exec('"' + script + '"', {}, (error, stdout, _) => {
        if (error) {
          reject(error);
          return;
        }
        resolve(stdout.trim());
      });
    }).then(result => {
      const version = result.replace(/\r?\n|\r/g, '');
      return { platform, version };
    }, (error) => {
      console.log(error);
      process.exit(1);
    });
  });

  return Promise.all(platformVersions);
};
