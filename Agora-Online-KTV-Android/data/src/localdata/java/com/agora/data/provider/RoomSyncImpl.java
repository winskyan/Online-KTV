package com.agora.data.provider;

import android.content.Context;

import com.agora.data.model.AgoraRoom;
import com.agora.data.inf.IRoomSyncManager;

import java.util.List;

import io.reactivex.Observable;

public class RoomSyncImpl implements IRoomSyncManager {

    public RoomSyncImpl(Context mContext) {
    }

    @Override
    public Observable<AgoraRoom> createRoom(AgoraRoom room) {
        //目前无现实，暂保留接口
        return null;
    }

    @Override
    public Observable<List<AgoraRoom>> getRooms() {
        return Observable.just(ExampleData.exampleRooms);
    }
}

