# 前提条件

开始前，请确保你的开发环境满足如下条件：

- Android Studio 4.0.0 或以上版本。
- Android 4.1 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用

## 注册Agora

### 1. 注册AppId

前往 [Agora官网](https://console.agora.io/)
注册项目，生产appId，请参考[开始使用 Agora 平台](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms)

### 2. 替换appId相关参数

在local.properties中配置对应的参数：

```mk
#app id
APP_ID=xxxxx
#rtc token
RTC_TOKEN=
#app certificate
APP_CERTIFICATE=xxxxx
```

注：

（1）APP_ID需要开通MCC（MusicContentCenter音乐内容中心）服务能力；

（2）如果启用了token模式，需要配置RTC_TOKEN值;

（3）为了测试方便，防止RTM TOKEN过期，APP_CERTIFICATE值仅仅为了动态生成RTM TOKEN使用;


## 运行示例项目

1. 开启 Android 设备的开发者选项，通过 USB 连接线将 Android 设备接入电脑。
2. 在 Android Studio 中，点击 Sync Project with Gradle Files 按钮，同步项目。
3. 在 Android Studio 左下角侧边栏中，点击 Build Variants 选择对应的平台。
4. 点击 Run app 按钮。运行一段时间后，应用就安装到 Android 设备上了。
5. 打开应用，即可使用。

# 参考资料

1.[客户端实现方案](https://docs.agora.io/cn/online-ktv/chorus_client_android?platform=Android)

2.[API参考](https://docs.agora.io/cn/online-ktv/ktv_api_android?platform=Android)