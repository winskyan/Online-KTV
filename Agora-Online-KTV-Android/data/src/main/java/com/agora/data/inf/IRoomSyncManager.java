package com.agora.data.inf;

import com.agora.data.model.AgoraRoom;

import java.util.List;

import io.reactivex.Observable;

public interface IRoomSyncManager {
    Observable<AgoraRoom> createRoom(AgoraRoom room);

    Observable<List<AgoraRoom>> getRooms();
}