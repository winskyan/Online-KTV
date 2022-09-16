# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Android Studio 4.0.0 或以上版本。
- Android 4.1 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用
## 注册Agora
1. 前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId，请参考[开始使用 Agora 平台](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms)
2. 替换appId相关参数
   在local.properties中配置对应的参数
   例如：

```mk
 #rtc app id
 RTC_APP_ID=xxxxxxxx
 #rtc token
 RTC_TOKEN=
 #mcc app id
 MCC_APP_ID=xxxxx
 #mcc uid
 MCC_UID=xxxxx
 #mcc rtm token
 MCC_RTM_TOKEN=xxxxxxx
```
   如果启用了token模式，需要配置 **RTC_TOKEN**值。


3. 下载SDK，请参考 [说明](https://docs.agora.io/cn/Voice/start_call_audio_android?platform=Android#%E9%9B%86%E6%88%90-sdk)


## 运行示例项目
1. 开启 Android 设备的开发者选项，通过 USB 连接线将 Android 设备接入电脑。
2. 在 Android Studio 中，点击 Sync Project with Gradle Files 按钮，同步项目。
3. 在 Android Studio 左下角侧边栏中，点击 Build Variants 选择对应的平台。
4. 点击 Run app 按钮。运行一段时间后，应用就安装到 Android 设备上了。
5. 打开应用，即可使用。


# 参考资料

1.[客户端实现方案](https://docs.agora.io/cn/online-ktv/chorus_client_android?platform=Android)

2.[API参考](https://docs.agora.io/cn/online-ktv/ktv_api_android?platform=Android)