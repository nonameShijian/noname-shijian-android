本项目中使用[WebViewUpgrade](https://github.com/JonaNorman/WebViewUpgrade)项目的代码升级Webview内核

# 项目说明
将公共的Api和升级Webview内核操作封装到NoameCore模块中，使所有App可以共用相同功能

# 克隆本项目
git clone --recursive https://github.com/nonameShijian/noname-shijian-android.git

# 更新NonameCore模块
git pull
git submodule update

# 创建安卓项目
先按教程全局安装cordova环境(本项目用的是cordova12)

然后安装项目依赖

```
npm i cordova@12 -g
npm i
```

创建安卓项目: 
```
cordova platform add android@13
```

在platforms\android\settings.gradle中加入以下代码
```
include ":NonameCore"
```

在platforms\android\app\src\main\res\xml\config.xml中的edit-config标签前面加入以下代码
```xml
<feature name="FinishImport">
    <param name="android-package" value="com.noname.shijian.FinishImport"/>
</feature>
```

在platforms\android\repositories.gradle
和
在platforms\android\app\repositories.gradle
`改为`:
```gradle
ext.repos = {
    google()
    mavenCentral()
    jcenter()
    maven { url "https://oss.jfrog.org/libs-snapshot" }
    maven { url 'https://jitpack.io' }
    maven { url 'https://maven.aliyun.com/repository/public/' }
}
```

在platforms\android\app\build.gradle的android块上面添加:
```gradle
def generateTime() {
    return new Date().format("yyyy-MM-dd")
}
android { ... }
```
在platforms\android\app\build.gradle的android块中添加:
```gradle
android.applicationVariants.all {
    variant ->
        variant.outputs.all {
            if (buildType.name == 'release') {
                outputFileName = "无名杀诗笺版(安卓)v${variant.versionName}(${generateTime()}).ApK"
            }
        }
}

aaptOptions {
    // 表示不让aapt压缩的文件后缀
    noCompress "apk"
}
```

在platforms\android\app\build.gradle的dependencies块的SUB-PROJECT DEPENDENCIES END注释后加入:
```gradle
dependencies {
    ...
    // SUB-PROJECT DEPENDENCIES END

    // 要添加的如下
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    implementation fileTree(dir: 'src/main/libs', include: '*.jar')
    implementation 'com.alibaba:fastjson:1.1.55.android'
    implementation 'androidx.palette:palette:1.0.0'
    // devtools需要websocket
    implementation 'org.java-websocket:Java-WebSocket:1.5.6'
    implementation(project(path: ":NonameCore"))
}
```

在platforms\android\cordova-plugin-local-notification的
唯一子文件的dependencies块，将compile改成implementation
(已经是implementation的就不用改了)

由于不太了解Cordova的版本号设置，如果需要，在platforms\android\app\src\main\AndroidManifest.xml中修改指定的版本号(versionCode)

最后将platforms\android\app\src\main\java\com\noname\shijian\MainActivity.java的以下代码注释掉以解除签名验证
```
CheckUtils.check(this, Executors.newFixedThreadPool(5));
```

然后打开`最新版`Android Studio进行安卓开发

# 说明
最近有一伙人号称是《无名杀十周年》的开发团队，宣称《无名杀十周年》“全新无名杀，比旧版拥有更多武将，兼容更多扩展”，实际上: 

- 《无名杀十周年》（原《无名杀清瑶版》）由无名杀v1.9.124修改而来，属于无名杀（libccy/noname）的一种**Fork**，并非“全新无名杀”；且《无名杀十周年》开发团队**公然违反GPL-3.0协议**，详情请看[这里](https://github.com/github/dmca/blob/master/2023/09/2023-09-20-noname.md)、[这里](https://tieba.baidu.com/p/8623890806)以及[这里](https://tieba.baidu.com/p/8624582238)。
- 《无名杀十周年》至今没有更新神典韦等新机制武将，且删除了部分无名杀的原创武将，导致《无名杀十周年》的武将数量远远不及无名杀前几个版本的武将数量；不仅如此，《无名杀十周年》自分裂后的部分武将源码依然来自无名杀和其他开发者开源的代码。
- 《无名杀十周年》兼容扩展的方式是不更新本体数据，从而导致《无名杀十周年》仍然在用1.9.124版本的代码，无法兼容使用1.10以后功能的扩展；而且《无名杀十周年》开发团队在使用**大量**GPL-3.0开源的代码后对生成产物进行了**混淆加密**，在**违反开源精神**的同时，也导致扩展稳定性极具下降，更容易出问题。

《无名杀十周年》就是彻头彻尾的骗局，《无名杀十周年》的开发团队更是一群拿无名杀吸血的骗子，虽然号称“不忘初心”，却公然对最有资格论述无名杀创作初心的无名杀创始人进行侮辱谩骂，直接违背其制定的规则和开源精神，恶劣程度远超当初在多个无名杀社群“自立”的水叶之流。

先秦介子推曾言：“窃人之财，犹谓之盗，况贪天之功以为己力乎。”无名杀社区发展至今，正是因为有大量的开源代码进行参考，才能不断推陈出新。试想每个扩展开发者在成为一个扩展开发者之前，谁敢说没有大量参考社区内的源码？每个作品凝聚的都是大家的心血，而不是仅仅归属于个别人。我们相信：开放、共享、多元才是无名杀的初心，绝不是封闭、私藏与趋同。

我们在此呼吁无名杀社区正确认识《无名杀十周年》开发团队的一些行为与做法，并希望《无名杀十周年》开发团队能反省迄今以来的所作所为。**自由开源**是无名杀社区的灵魂，希望各方都能够遵循这一精神。