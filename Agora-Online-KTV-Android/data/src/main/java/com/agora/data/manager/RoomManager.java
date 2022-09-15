package com.agora.data.manager;

import android.content.Context;

import com.agora.data.model.AgoraRoom;
import com.agora.data.provider.RoomSyncImpl;
import com.agora.data.inf.IRoomSyncManager;

import java.util.List;

import io.reactivex.Observable;

/**
 * 房间状态同步
 */
public final class RoomManager implements IRoomSyncManager {

    private volatile static RoomManager instance;

    private RoomManager() {
    }

    public static RoomManager Instance() {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null)
                    instance = new RoomManager();
            }
        }
        return instance;
    }

    private IRoomSyncManager mIRoomSyncManager;

    public void init(Context mContext) {
        mIRoomSyncManager = new RoomSyncImpl(mContext);
    }

    @Override
    public Observable<AgoraRoom> createRoom(AgoraRoom room) {
        return mIRoomSyncManager.createRoom(room);
    }

    @Override
    public Observable<List<AgoraRoom>> getRooms() {
        return mIRoomSyncManager.getRooms();
    }
}
