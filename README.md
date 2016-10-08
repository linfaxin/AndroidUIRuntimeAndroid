### Unstable Warn

This library was in the experiments.

After Android5, the webview's performance is good, no need to use this library.

If you wan to improve Android4's performance, it's better to pack a webkit core in your app (like xwalk);

### AndroidUIRuntime-Android

Android's runtime for [AndroidUIX](https://github.com/linfaxin/AndroidUIX)

Runtime sample apk: [Download](app/runtime_sample_v1.0.apk?raw=true)


### Support for app

Import the 'runtimeLib' module to your app.

Call when app init:
```java
    RuntimeInit.init(context);
```

Call when webView init:
```java
    RuntimeInit.initWebView(webView);
```




### Support for cordova plugin

```shell
    cordova plugin add androidui-webapp-runtime-cordova
```
