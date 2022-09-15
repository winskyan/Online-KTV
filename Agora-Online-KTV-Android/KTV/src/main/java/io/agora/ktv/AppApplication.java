package io.agora.ktv;

import android.util.Log;

import io.agora.rtc2.RtcEngine;


public class AppApplication extends AgoraApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("APP", "SDK Version: " + RtcEngine.getSdkVersion());
    }
}