package io.agora.ktv;

import androidx.multidex.MultiDexApplication;

import com.agora.data.manager.RoomManager;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;

public class AgoraApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.init(LogLevel.ALL);

        RoomManager.Instance().init(this);
    }
}
