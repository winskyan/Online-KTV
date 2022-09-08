package io.agora.ktv.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 1.起始函数{@link MultipleMusicPlayer#prepare}。
 * 2.陪唱点击按钮"加入合唱"后，触发申请 ，然后触发{@link MultipleMusicPlayer#onMemberApplyJoinChorus}，主唱把第一个人设置成陪唱。
 * 3.有陪唱加入后，会收到回调{@link MultipleMusicPlayer#onMemberJoinedChorus}，开始下载资源。
 * 4.{@link MultipleMusicPlayer#joinChannelEX}之后，修改状态成Ready，当所有唱歌的人都Ready后，会触发{@link MultipleMusicPlayer#onMemberChorusReady}
 */
public class MultipleMusicPlayer extends BaseMusicPlayer {

    private static final long PLAY_WAIT = 1000L;

    private final SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {
        @Override
        public void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
            super.onMemberApplyJoinChorus(music);
            MultipleMusicPlayer.this.onMemberApplyJoinChorus(music);
        }

        @Override
        public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
            super.onMemberJoinedChorus(music);
            MultipleMusicPlayer.this.onMemberJoinedChorus(music);
        }

        @Override
        public void onMemberChorusReady(@NonNull MemberMusicModel music) {
            super.onMemberChorusReady(music);
            MultipleMusicPlayer.this.onMemberChorusReady(music);
        }
    };

    public MultipleMusicPlayer(Context mContext, int role, IMediaPlayer mPlayer) {
        super(mContext, role, mPlayer);
        RoomManager.Instance(mContext).getRtcEngine().setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_STANDARD);
        RoomManager.Instance(mContext).addRoomEventCallback(mRoomEventCallback);
        this.mPlayer.adjustPlayoutVolume(80);
        this.selectAudioTrack(1);
    }

    @Override
    public void destroy() {
        super.destroy();

        leaveChannelEX();
        stopNetTestTask();
        RoomManager.Instance(mContext).removeRoomEventCallback(mRoomEventCallback);
    }

    private boolean mRunNetTask = false;
    private Thread mNetTestThread;

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        super.onJoinChannelSuccess(channel, uid, elapsed);
    }

    private void startNetTestTask() {
        mRunNetTask = true;
        mNetTestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mRunNetTask) {
                    sendTestDelay();

                    try {
                        Thread.sleep(10 * 1000L);
                    } catch (InterruptedException exp) {
                        break;
                    }
                }
            }
        });
        mNetTestThread.setName("Thread-NetTest");
        mNetTestThread.start();
    }

    private void stopNetTestTask() {
        mRunNetTask = false;
        if (mNetTestThread != null) {
            mNetTestThread.interrupt();
            mNetTestThread = null;
        }
    }

    private MemberMusicModel musicModelReady;

    @Override
    public void prepare(@NonNull MemberMusicModel music) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        AgoraRoom mRoom = RoomManager.Instance(mContext).getRoom();
        if (mRoom == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            if (music.getUserStatus() == MemberMusicModel.UserStatus.Ready) {
                onMemberJoinedChorus(music);
            }
        } else if (ObjectsCompat.equals(music.getUser1Id(), mUser.getObjectId())) {
            onMemberJoinedChorus(music);
        } else {
            if (!TextUtils.isEmpty(music.getUserId()) && music.getUserStatus() == MemberMusicModel.UserStatus.Ready
                    && !TextUtils.isEmpty(music.getUser1Id()) && music.getUser1Status() == MemberMusicModel.UserStatus.Ready) {
                onMemberChorusReady(music);
            } else if (!TextUtils.isEmpty(music.getUser1Id()) && music.getUser1Status() == MemberMusicModel.UserStatus.Idle) {
                onMemberJoinedChorus(music);
            } else if (!TextUtils.isEmpty(music.getApplyUser1Id())) {
                onMemberApplyJoinChorus(music);
            }
        }
    }

    private RtcConnection mRtcConnection;

    private String channelName = null;

    private void joinChannelEX() {
        mLogger.d("joinChannelEX() called");
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        AgoraRoom mRoom = RoomManager.Instance(mContext).getRoom();
        assert mRoom != null;
        channelName = mRoom.getId();

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = mRole;
        options.publishMicrophoneTrack = false;
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        if (ObjectsCompat.equals(musicModelReady.getUserId(), mUser.getObjectId())) {
            options.publishMediaPlayerAudioTrack = true;
            options.enableAudioRecordingOrPlayout = false;
        } else if (ObjectsCompat.equals(musicModelReady.getUser1Id(), mUser.getObjectId())) {
            options.publishMediaPlayerAudioTrack = false;
            options.enableAudioRecordingOrPlayout = false;
        }

        int uid = (int)(Math.random() * (Integer.MAX_VALUE / 2));
        if (ObjectsCompat.equals(mUser.getObjectId(), musicModelReady.getUserId())) {
            if (musicModelReady.getUserbgId() != null) {
                uid = musicModelReady.getUserbgId().intValue();
            }
        } else if (ObjectsCompat.equals(mUser.getObjectId(), musicModelReady.getUser1Id())) {
            if (musicModelReady.getUser1bgId() != null) {
                uid = musicModelReady.getUser1bgId().intValue();
            }
        }

        mRtcConnection = new RtcConnection();
        mRtcConnection.channelId = channelName;
        mRtcConnection.localUid = uid;
        RoomManager.Instance(mContext).getRtcEngine().joinChannelEx("", mRtcConnection, options, new IRtcEngineEventHandler() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                super.onJoinChannelSuccess(channel, uid, elapsed);
                mLogger.d("onJoinChannelSuccessEX() called with: channel = [%s], uid = [%s], elapsed = [%s]", channel, uid, elapsed);
                MultipleMusicPlayer.this.onJoinChannelExSuccess(uid);
            }

            @Override
            public void onLeaveChannel(RtcStats stats) {
                super.onLeaveChannel(stats);
                mLogger.d("onLeaveChannelEX() called with: stats = [%s]", stats);
            }
        });
    }

    private void leaveChannelEX() {
        if (mRtcConnection == null) {
            return;
        }

        mLogger.d("leaveChannelEX() called");
        RoomManager.Instance(mContext).getRtcEngine().muteAllRemoteAudioStreams(false);
        if (!TextUtils.isEmpty(channelName)) {
            RoomManager.Instance(mContext).getRtcEngine().leaveChannelEx(mRtcConnection);
            mRtcConnection = null;
        }
    }

    private int mUid;

    private void onJoinChannelExSuccess(int uid) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        AgoraRoom mRoom = RoomManager.Instance(mContext).getRoom();
        if (mRoom == null) {
            return;
        }

        this.mUid = uid;
        Long streamId = uid & 0xffffffffL;
        if (ObjectsCompat.equals(musicModelReady.getUserId(), mUser.getObjectId())) {
            HashMap<String, Object> maps = new HashMap<>();
            maps.put(MemberMusicModel.COLUMN_USERSTATUS, MemberMusicModel.UserStatus.Ready.value);
            maps.put(MemberMusicModel.COLUMN_USERBGID, streamId);

            SyncManager.Instance()
                    .getRoom(mRoom.getId())
                    .collection(MemberMusicModel.TABLE_NAME)
                    .document(musicModelReady.getId())
                    .update(maps, new SyncManager.DataItemCallback() {
                        @Override
                        public void onSuccess(AgoraObject result) {

                        }

                        @Override
                        public void onFail(AgoraException exception) {

                        }
                    });
        }
    }

    @Override
    protected void onMusicOpenCompleted() {
        mLogger.i("onMusicOpenCompleted() called");
        mStatus = Status.Opened;

        startDisplayLrc();
        mHandler.obtainMessage(ACTION_ON_MUSIC_OPENCOMPLETED, mPlayer.getDuration()).sendToTarget();
    }

    private volatile boolean isApplyJoinChorus = false;

    private void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (!ObjectsCompat.equals(mUser.getObjectId(), music.getUserId())) {
            return;
        }

        AgoraRoom mRoom = RoomManager.Instance(mContext).getRoom();
        if (mRoom == null) {
            return;
        }

        if (isApplyJoinChorus) {
            return;
        }

        isApplyJoinChorus = true;
        HashMap<String, Object> maps = new HashMap<>();
        maps.put(MemberMusicModel.COLUMN_USER1ID, music.getApplyUser1Id());
        maps.put(MemberMusicModel.COLUMN_APPLYUSERID, "");

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .document(music.getId())
                .update(maps, new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {
                        isApplyJoinChorus = false;
                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        isApplyJoinChorus = false;
                    }
                });
    }

    private void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mUser.getObjectId(), music.getUserId())
                || ObjectsCompat.equals(mUser.getObjectId(), music.getUser1Id())) {
            onPrepareResource();
            ResourceManager.Instance(mContext)
                    .download(music, false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<MemberMusicModel>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@NonNull MemberMusicModel musicModel) {
                            onResourceReady(musicModel);
                            musicModelReady = musicModel;

                            open(musicModelReady);

                            if (ObjectsCompat.equals(mUser.getObjectId(), music.getUserId())) {
                                joinChannelEX();
                            } else if (ObjectsCompat.equals(mUser.getObjectId(), music.getUser1Id())) {
                                startNetTestTask();

                                AgoraRoom room = RoomManager.Instance(mContext).getRoom();
                                if (room == null) {
                                    return;
                                }

                                HashMap<String, Object> maps = new HashMap<>();
                                maps.put(MemberMusicModel.COLUMN_USER1STATUS, MemberMusicModel.UserStatus.Ready.value);
                                maps.put(MemberMusicModel.COLUMN_USER1BGID, mUid);

                                SyncManager.Instance()
                                        .getRoom(room.getId())
                                        .collection(MemberMusicModel.TABLE_NAME)
                                        .document(musicModelReady.getId())
                                        .update(maps, new SyncManager.DataItemCallback() {
                                            @Override
                                            public void onSuccess(AgoraObject result) {

                                            }

                                            @Override
                                            public void onFail(AgoraException exception) {

                                            }
                                        });
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        }
    }

    /**
     * 主唱逻辑：
     * 1.{@link MultipleMusicPlayer#sendStartPlay}通知陪唱的人，要开始了。
     * 2.陪唱人会收到{@link MultipleMusicPlayer#onReceivedStatusPlay}。
     * 3.为了保证所有人同时播放音乐，做延迟wait。
     * <p>
     * 陪唱逻辑:{@link MultipleMusicPlayer#onReceivedStatusPlay}
     *
     * @param music
     */
    private void onMemberChorusReady(@NonNull MemberMusicModel music) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        AgoraMember mMine = RoomManager.Instance(mContext).getMine();
        if (mMine == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            //唱歌人，主唱，joinChannel 需要屏蔽的uid
            RoomManager.Instance(mContext).getRtcEngine().muteRemoteAudioStream(music.getUserbgId().intValue(), true);
        } else if (ObjectsCompat.equals(music.getUser1Id(), mUser.getObjectId())) {
            //唱歌人，陪唱人，joinChannel 需要屏蔽的uid
            RoomManager.Instance(mContext).getRtcEngine().muteRemoteAudioStream(music.getUserbgId().intValue(), true);
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())
                || ObjectsCompat.equals(music.getUser1Id(), mUser.getObjectId())) {
            music.setFileMusic(musicModelReady.getFileMusic());
            music.setFileLrc(musicModelReady.getFileLrc());

            if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
                sendStartPlay();
                try {
                    synchronized (this) {
                        wait(PLAY_WAIT);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                play();
            }
        } else {
            onPrepareResource();

            ResourceManager.Instance(mContext)
                    .download(music, true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<MemberMusicModel>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@NonNull MemberMusicModel musicModel) {
                            onResourceReady(musicModel);

                            onMusicPlaingByListener();
                            playByListener(music);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        }
    }

    @Override
    protected void onReceivedStatusPlay(int uid) {
        super.onReceivedStatusPlay(uid);

        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            if (mStatus == Status.Started) {
                return;
            }

            try {
                synchronized (this) {
                    long waitTime = PLAY_WAIT - netRtt;
                    mLogger.d("onReceivedStatusPlay() called with: waitTime = [%s]", waitTime);
                    if (waitTime > 0) {
                        wait(waitTime);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            play();
        }
    }

    @Override
    protected void onReceivedStatusPause(int uid) {
        super.onReceivedStatusPause(uid);

        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {

        } else if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            pause();
        }
    }

    @Override
    protected void onReceivedSetLrcTime(int uid, long position) {
        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {

        } else if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            if (mStatus == Status.Paused) {
                resume();
            }
      } else {
            super.onReceivedSetLrcTime(uid, position);
        }
    }

    @Override
    protected void onReceivedTestDelay(int uid, long time) {
        super.onReceivedTestDelay(uid, time);

        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            sendReplyTestDelay(time);
        }
    }

    private long offsetTS = 0;
    private long netRtt = 0;
    private long delayWithBrod = 0;

    @Override
    protected void onReceivedReplyTestDelay(int uid, long testDelayTime, long time, long position) {
        super.onReceivedReplyTestDelay(uid, testDelayTime, time, position);
        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            long localTs = System.currentTimeMillis();
            netRtt = (localTs - testDelayTime) / 2;
            delayWithBrod = position + netRtt;

            long localPos = mPlayer.getPlayPosition();
            long diff = localPos - delayWithBrod;
            if (Math.abs(diff) > 40) {
                mLogger.d("xn123 seek= [%s], remotePos = [%s], localPos = [%s]", delayWithBrod, position, localPos);
                seek(delayWithBrod);
            }
        }
    }

    @Override
    protected void onReceivedOrigleChanged(int uid, int mode) {
        super.onReceivedOrigleChanged(uid, mode);
        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUser1Id(), mUser.getObjectId())) {
            selectAudioTrack(mode);
        }
    }

    @Override
    public void switchRole(int role) {
        mLogger.d("switchRole() called with: role = [%s]", role);
        mRole = role;

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        options.clientRoleType = role;
        options.publishMediaPlayerAudioTrack = false;
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            options.publishMicrophoneTrack = true;
        } else {
            options.publishMicrophoneTrack = false;
        }

        RoomManager.Instance(mContext).getRtcEngine().updateChannelMediaOptions(options);
    }

    @Override
    protected void startPublish() {
        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            super.startPublish();
        }
    }

    @Override
    public void togglePlay() {
        if (mStatus == Status.Started) {
            sendPause();
        } else if (mStatus == Status.Paused) {

        }

        super.togglePlay();
    }

    public void sendReplyTestDelay(long receiveTime) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "replyTestDelay");
        msg.put("testDelayTime", String.valueOf(receiveTime));
        msg.put("time", String.valueOf(System.currentTimeMillis()));
        msg.put("position", mPlayer.getPlayPosition());
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendReplyTestDelay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendTestDelay() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "testDelay");
        msg.put("time", String.valueOf(System.currentTimeMillis()));
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendTestDelay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendStartPlay() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("time", 0);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendStartPlay() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    public void sendPause() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "setLrcTime");
        msg.put("time", -1);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendPause() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }

    @Override
    public void selectAudioTrack(int i) {
        super.selectAudioTrack(i);

        MemberMusicModel mMemberMusicModel = RoomManager.Instance(mContext).getMusicModel();
        if (mMemberMusicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(mMemberMusicModel.getUserId(), mUser.getObjectId())) {
            sendTrackMode(i);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mRtcConnection == null) {
            return;
        }
        if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
            RoomManager.Instance(mContext).getRtcEngine().muteAllRemoteAudioStreams(false);
            if (!TextUtils.isEmpty(channelName)) {
                RoomManager.Instance(mContext).getRtcEngine().leaveChannelEx(mRtcConnection);
                mRtcConnection = null;
            }
        }
    }

    public void sendTrackMode(int mode) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", "TrackMode");
        msg.put("mode", mode);
        JSONObject jsonMsg = new JSONObject(msg);
        int streamId = RoomManager.Instance(mContext).getStreamId();
        int ret = RoomManager.Instance(mContext).getRtcEngine().sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            mLogger.e("sendTrackMode() sendStreamMessage called returned: ret = [%s]", ret);
        }
    }
}
