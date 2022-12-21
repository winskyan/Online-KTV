package io.agora.ktv.manager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraMusicCharts;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;

import io.agora.ktv.bean.MemberMusicModel;
import io.agora.musiccontentcenter.MusicChartInfo;

@MainThread
public interface RoomEventCallback {

    default void onRoomInfoChanged(@NonNull AgoraRoom room) {

    }

    /**
     * 房间被关闭
     *
     * @param fromUser true-是我主动关闭，false-被动关闭，比如房主退出房间
     */
    default void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser) {

    }

    /**
     * 用户加入房间
     */
    default void onMemberJoin(@NonNull AgoraMember member) {

    }

    /**
     * 用户离开房间，不包括房主
     */
    default void onMemberLeave(@NonNull AgoraMember member) {

    }

    /**
     * 房间角色变化回调，角色变化指：观众和说话人变化
     */
    default void onRoleChanged(@NonNull AgoraMember member) {

    }

    /**
     * Audio变化回调，这里变化是指：开麦和禁麦
     *
     * @param isMine 是否是我主动触发的回调，true-我主动触发，false-不是我触发，被动触发。
     */
    default void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {

    }

    default void onRoomError(int error, String msg) {

    }

    default void onMusicAdd(@NonNull MemberMusicModel music) {

    }

    default void onMusicDelete(@NonNull MemberMusicModel music) {

    }

    default void onMusicChanged(@NonNull MemberMusicModel music) {

    }

    default void onMusicEmpty() {

    }

    default void onMusicProgress(long total, long cur) {

    }

    default void onMusicChartsResult(String requestId, AgoraMusicCharts[] musicCharts) {

    }

    default void onMusicCollectionResult(String requestId, MusicModel[] musics) {

    }

    default void onMusicPreLoadEvent(long songCode, int percent, int status, String msg, String lyricUrl) {

    }

    default void onLyricResult(String requestId, String lyricUrl) {

    }

}
