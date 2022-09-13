package io.agora.ktv.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraMusicCharts;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.agora.ktv.bean.MemberMusicModel;
import io.agora.musiccontentcenter.MusicChartInfo;

/**
 * 主要将房间内事件切换到主线程，然后丢给界面。
 */
public class MainThreadDispatch implements RoomEventCallback {
    private Logger.Builder mLogger = XLog.tag("MainThreadDispatch");

    private static final int ON_MEMBER_JOIN = 1;
    private static final int ON_MEMBER_LEAVE = ON_MEMBER_JOIN + 1;
    private static final int ON_ROLE_CHANGED = ON_MEMBER_LEAVE + 1;
    private static final int ON_AUDIO_CHANGED = ON_ROLE_CHANGED + 1;
    private static final int ON_ROOM_ERROR = ON_AUDIO_CHANGED + 1;
    private static final int ON_ROOM_CLOSED = ON_ROOM_ERROR + 1;
    private static final int ON_MUSIC_ADD = ON_ROOM_CLOSED + 1;
    private static final int ON_MUSIC_DELETE = ON_MUSIC_ADD + 1;
    private static final int ON_MUSIC_CHANGED = ON_MUSIC_DELETE + 1;
    private static final int ON_MUSIC_EMPTY = ON_MUSIC_CHANGED + 1;
    private static final int ON_MUSIC_PROGRESS = ON_MUSIC_EMPTY + 1;
    private static final int ON_ROOM_INFO_CHANGED = ON_MUSIC_PROGRESS + 1;
    private static final int ON_MUSIC_CHARTS_RESULT = ON_ROOM_INFO_CHANGED + 1;
    private static final int ON_MUSIC_COLLECTION_RESULT = ON_MUSIC_CHARTS_RESULT + 1;
    private static final int ON_MUSIC_PRELOAD_EVENT = ON_MUSIC_COLLECTION_RESULT + 1;
    private static final int ON_LYRIC_RESULT = ON_MUSIC_PRELOAD_EVENT + 1;

    private final List<RoomEventCallback> eventCallbacks = new ArrayList<>();

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.eventCallbacks.add(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.eventCallbacks.remove(callback);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == ON_MEMBER_JOIN) {
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberJoin((AgoraMember) msg.obj);
                }
            } else if (msg.what == ON_MEMBER_LEAVE) {
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMemberLeave((AgoraMember) msg.obj);
                }
            } else if (msg.what == ON_ROLE_CHANGED) {
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoleChanged((AgoraMember) msg.obj);
                }
            } else if (msg.what == ON_AUDIO_CHANGED) {
                Bundle bundle = msg.getData();
                boolean isMine = bundle.getBoolean("isMine");
                AgoraMember member = bundle.getParcelable("member");

                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onAudioStatusChanged(isMine, member);
                }
            } else if (msg.what == ON_ROOM_ERROR) {
                Bundle bundle = msg.getData();
                int error = bundle.getInt("error");
                String msgError = bundle.getString("msg");

                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomError(error, msgError);
                }
            } else if (msg.what == ON_ROOM_CLOSED) {
                Bundle bundle = msg.getData();
                AgoraRoom room = bundle.getParcelable("room");
                boolean fromUser = bundle.getBoolean("fromUser");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomClosed(room, fromUser);
                }
            } else if (msg.what == ON_MUSIC_ADD) {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicAdd(data);
                }
            } else if (msg.what == ON_MUSIC_DELETE) {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicDelete(data);
                }
            } else if (msg.what == ON_MUSIC_EMPTY) {
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicEmpty();
                }
            } else if (msg.what == ON_MUSIC_CHANGED) {
                MemberMusicModel data = (MemberMusicModel) msg.obj;
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicChanged(data);
                }
            } else if (msg.what == ON_MUSIC_PROGRESS) {
                Bundle bundle = msg.getData();
                long total = bundle.getLong("total");
                long cur = bundle.getLong("cur");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicProgress(total, cur);
                }
            } else if (msg.what == ON_ROOM_INFO_CHANGED) {
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onRoomInfoChanged((AgoraRoom) msg.obj);
                }
            } else if (msg.what == ON_MUSIC_CHARTS_RESULT) {
                Bundle bundle = msg.getData();
                String requestId = bundle.getString("requestId");
                AgoraMusicCharts[] musicCharts = (AgoraMusicCharts[]) bundle.getParcelableArray("musicCharts");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicChartsResult(requestId, musicCharts);
                }
            } else if (msg.what == ON_MUSIC_COLLECTION_RESULT) {
                Bundle bundle = msg.getData();
                String requestId = bundle.getString("requestId");
                MusicModel[] musics = (MusicModel[]) bundle.getParcelableArray("musics");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicCollectionResult(requestId, musics);
                }
            } else if (msg.what == ON_MUSIC_PRELOAD_EVENT) {
                Bundle bundle = msg.getData();
                long songCode = bundle.getLong("songCode");
                String lyricUrl = bundle.getString("lyricUrl");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onMusicPreLoadEvent(songCode, lyricUrl);
                }
            } else if (msg.what == ON_LYRIC_RESULT) {
                Bundle bundle = msg.getData();
                String requestId = bundle.getString("requestId");
                String lyricUrl = bundle.getString("lyricUrl");
                for (RoomEventCallback callback : eventCallbacks) {
                    callback.onLyricResult(requestId, lyricUrl);
                }
            }
            return false;
        }
    });

    @Override
    public void onRoomInfoChanged(@NonNull AgoraRoom room) {
        mLogger.d("onRoomInfoChanged() called with: room = [%s]", room);
        mHandler.obtainMessage(ON_ROOM_INFO_CHANGED, room).sendToTarget();
    }

    @Override
    public void onRoomClosed(@NonNull AgoraRoom room, boolean fromUser) {
        mLogger.d("onRoomClosed() called with: room = [%s], fromUser = [%s]", room, fromUser);
        Bundle bundle = new Bundle();
        bundle.putParcelable("room", room);
        bundle.putBoolean("fromUser", fromUser);

        Message message = mHandler.obtainMessage(ON_ROOM_CLOSED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMemberJoin(@NonNull AgoraMember member) {
        mLogger.d("onMemberJoin() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_MEMBER_JOIN, member).sendToTarget();
    }

    @Override
    public void onMemberLeave(@NonNull AgoraMember member) {
        mLogger.d("onMemberLeave() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_MEMBER_LEAVE, member).sendToTarget();
    }

    @Override
    public void onRoleChanged(@NonNull AgoraMember member) {
        mLogger.d("onRoleChanged() called with: member = [%s]", member);
        mHandler.obtainMessage(ON_ROLE_CHANGED, member).sendToTarget();
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.d("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMine", isMine);
        bundle.putParcelable("member", member);

        Message message = mHandler.obtainMessage(ON_AUDIO_CHANGED);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onRoomError(int error, String msg) {
        mLogger.d("onRoomError() called with: error = [%s], msg = [%s]", error, msg);
        Bundle bundle = new Bundle();
        bundle.putInt("error", error);
        bundle.putString("msg", msg);

        Message message = mHandler.obtainMessage(ON_ROOM_ERROR);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMusicAdd(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicAdd() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_ADD, music).sendToTarget();
    }

    @Override
    public void onMusicDelete(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicDelete() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_DELETE, music).sendToTarget();
    }

    @Override
    public void onMusicChanged(@NonNull MemberMusicModel music) {
        mLogger.d("onMusicChanged() called with: music = [%s]", music);
        mHandler.obtainMessage(ON_MUSIC_CHANGED, music).sendToTarget();
    }

    @Override
    public void onMusicEmpty() {
        mLogger.d("onMusicEmpty() called");
        mHandler.obtainMessage(ON_MUSIC_EMPTY).sendToTarget();
    }

    @Override
    public void onMusicProgress(long total, long cur) {
        mLogger.d("onMusicProgress() called with: total = [%s], cur = [%s]", total, cur);
        Bundle bundle = new Bundle();
        bundle.putLong("total", total);
        bundle.putLong("cur", cur);

        Message message = mHandler.obtainMessage(ON_MUSIC_PROGRESS);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMusicChartsResult(String requestId, AgoraMusicCharts[] musicCharts) {
        mLogger.d("onMusicChartsResult() called with: requestId = [%s],musicCharts=[%s]", requestId, Arrays.toString(musicCharts));

        Bundle bundle = new Bundle();
        bundle.putString("requestId", requestId);
        bundle.putParcelableArray("musicCharts", musicCharts);

        Message message = mHandler.obtainMessage(ON_MUSIC_CHARTS_RESULT);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMusicCollectionResult(String requestId, MusicModel[] musics) {
        mLogger.d("onMusicCollectionResult() called with: requestId = [%s],musics=[%s]", requestId, Arrays.toString(musics));

        Bundle bundle = new Bundle();
        bundle.putString("requestId", requestId);
        bundle.putParcelableArray("musics", musics);

        Message message = mHandler.obtainMessage(ON_MUSIC_COLLECTION_RESULT);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onMusicPreLoadEvent(long songCode, String lyricUrl) {
        mLogger.d("onMusicPreLoadEvent() called with: songCode = [%s],lyricUrl=[%s]", songCode, lyricUrl);

        Bundle bundle = new Bundle();
        bundle.putLong("songCode", songCode);
        bundle.putString("lyricUrl", lyricUrl);

        Message message = mHandler.obtainMessage(ON_MUSIC_PRELOAD_EVENT);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void onLyricResult(String requestId, String lyricUrl) {
        mLogger.d("onLyricResult() called with: requestId = [%s],lyricUrl=[%s]", requestId, lyricUrl);

        Bundle bundle = new Bundle();
        bundle.putString("requestId", requestId);
        bundle.putString("lyricUrl", lyricUrl);

        Message message = mHandler.obtainMessage(ON_LYRIC_RESULT);
        message.setData(bundle);
        message.sendToTarget();
    }
}
