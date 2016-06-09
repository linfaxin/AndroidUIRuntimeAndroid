### AndroidUIRuntime-Android

Android's runtime for [AndroidUI-WebApp](https://github.com/linfaxin/AndroidUI-WebApp)

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