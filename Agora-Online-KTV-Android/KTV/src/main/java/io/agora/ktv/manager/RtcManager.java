package io.agora.ktv.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.provider.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraMusicCharts;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.util.ToastUtils;
import io.agora.ktv.BuildConfig;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.musiccontentcenter.IAgoraMusicContentCenter;
import io.agora.musiccontentcenter.IAgoraMusicPlayer;
import io.agora.musiccontentcenter.IMusicContentCenterEventHandler;
import io.agora.musiccontentcenter.Music;
import io.agora.musiccontentcenter.MusicChartInfo;
import io.agora.musiccontentcenter.MusicContentCenterConfiguration;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtm.RtmTokenBuilder;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

/**
 * 房间控制
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
@SuppressWarnings("ALL")
public final class RtcManager {
    private final Logger.Builder mLogger = XLog.tag("RoomManager");
    private final Logger.Builder mLoggerRTC = XLog.tag("RTC");

    private volatile static RtcManager instance;

    private Context mContext;
    private final MainThreadDispatch mMainThreadDispatch = new MainThreadDispatch();

//    private final Map<String, AgoraMember> memberHashMap = new ConcurrentHashMap<>();

    private volatile static AgoraRoom mRoom;
    private volatile static AgoraMember owner;
    private volatile static AgoraMember mMine;

    private volatile MemberMusicModel mMusicModel;

    private RtcEngine mRtcEngine;
    private IAgoraMusicContentCenter mMcc;
    private IAgoraMusicPlayer mAgoraMusicPlayer;

    private MusicContentCenterConfiguration mConfig;

    private List<Double> voicePitchList = new ArrayList<>();

    private ConnectivityManager mConnMgr;

    /**
     * 唱歌人的UserId
     */
//    private final List<String> singers = new ArrayList<>();
    private IMusicContentCenterEventHandler mIMccEventHandler = new IMusicContentCenterEventHandler() {
        @Override
        public void onPreLoadEvent(String requestId, long songCode, int percent, String lyricUrl, int status, int errorCode) {
            mLogger.d("onPreLoadEvent " + requestId + "," + songCode + "," + percent + "," + lyricUrl + "," + status + ",errorCode=" + errorCode);
            mMainThreadDispatch.onMusicPreLoadEvent(requestId, songCode, percent, lyricUrl, status, errorCode);
        }

        @Override
        public void onMusicCollectionResult(String requestId, int page, int pageSize, int total, Music[] list, int errorCode) {
            mLogger.d("onMusicCollectionResult " + requestId + "," + errorCode + "," + page + "," + pageSize + "," + list);
            if (0 == errorCode) {
                MusicModel[] musics = new MusicModel[list.length];
                MusicModel music;
                Music musicItem;
                for (int i = 0; i < list.length; i++) {
                    musicItem = list[i];
                    music = new MusicModel();
                    music.setMusicId(String.valueOf(musicItem.getSongCode()));
                    music.setName(musicItem.getName());
                    music.setCreatedAt(musicItem.getReleaseTime());
                    music.setSinger(musicItem.getSinger());
                    music.setPoster(musicItem.getPoster());
                    music.setType(musicItem.getType());
                    musics[i] = music;
                }
                mMainThreadDispatch.onMusicCollectionResult(requestId, musics);
            }
        }

        @Override
        public void onMusicChartsResult(String requestId, MusicChartInfo[] list, int errorCode) {
            mLogger.d("onMusicChartsResult " + requestId + "," + errorCode + "," + list);
            if (0 == errorCode) {
                AgoraMusicCharts[] musicCharts = new AgoraMusicCharts[list.length];
                AgoraMusicCharts agoraMusicCharts;
                for (int i = 0; i < list.length; i++) {
                    agoraMusicCharts = new AgoraMusicCharts();
                    agoraMusicCharts.setName(list[i].name);
                    agoraMusicCharts.setType(list[i].type);
                    musicCharts[i] = agoraMusicCharts;
                }
                mMainThreadDispatch.onMusicChartsResult(requestId, musicCharts);
            }
        }

        @Override
        public void onLyricResult(String requestId, long songCode, String lyricUrl, int errorCode) {
            mLogger.d("onLyricResult " + requestId + "," + lyricUrl + ",errorCode=" + errorCode);
            mMainThreadDispatch.onLyricResult(requestId, songCode, lyricUrl);
        }

        @Override
        public void onSongSimpleInfoResult(String requestId, long songCode, String simpleInfo, int errorCode) {
            mLogger.d("onSongSimpleInfoResult " + requestId + "," + songCode + ",simple Info=" + simpleInfo + ",errorCode=" + errorCode);
        }
    };

    private IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            super.onAudioVolumeIndication(speakers, totalVolume);
            if (null != speakers && speakers.length > 0) {
                voicePitchList.add(speakers[0].voicePitch);
            }
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            mLogger.e("onError() called with: err = [%s]", err);
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            super.onConnectionStateChanged(state, reason);
            mLoggerRTC.d("onConnectionStateChanged() called with: state = [%s], reason = [%s]", state, reason);

            if (state == Constants.CONNECTION_STATE_FAILED) {
                if (emitterJoinRTC != null) {
                    emitterJoinRTC.onError(new Exception("connection_state_failed"));
                    emitterJoinRTC = null;
                }
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            mLoggerRTC.i("onJoinChannelSuccess() called with: channel = [%s], uid = [%s], elapsed = [%s]", channel, uid, elapsed);

            if (emitterJoinRTC != null) {
                emitterJoinRTC.onSuccess(uid);
                emitterJoinRTC = null;
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            mLoggerRTC.i("onUserOffline() " + uid);
            super.onUserOffline(uid, reason);
            User user = new User();
            user.setObjectId(String.valueOf(uid));

            AgoraMember member = new AgoraMember();
            member.setId(user.getObjectId());

            mMainThreadDispatch.onMemberLeave(member);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            mLoggerRTC.i("onLeaveChannel() called with: stats = [%s]", stats);
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            JSONObject jsonMsg;
            try {
                String strMsg = new String(data);
                jsonMsg = new JSONObject(strMsg);

                String cmd = jsonMsg.getString("cmd");

                mLoggerRTC.d(jsonMsg);
                if (cmd == null) return;

                if (cmd.equals("setLrcTime")) {
                    String musicId = jsonMsg.getString("lrcId");
                    if (musicId.isEmpty()) return;
                    String remoteUserId = null;
                    try {
                        remoteUserId = String.valueOf(uid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (remoteUserId == null) return;
                    boolean shouldPlay = jsonMsg.getInt("state") == 1;
                    // 当前不在播放 || 远端更换歌曲 《==》播放远端歌曲
                    if ((mMusicModel == null && shouldPlay) ||
                            (mMusicModel != null && !mMusicModel.getMusicId().equals(musicId) && mMusicModel.getUserId().equals(remoteUserId))
                                    && shouldPlay) {
                        mMusicModel = new MemberMusicModel(musicId);
                        mMusicModel.setId(musicId);
                        mMusicModel.setUserId(remoteUserId);
                        onMusicChanged(mMusicModel);
                        // 远端切歌
                    } else if (jsonMsg.getInt("state") == 0 && (null == mMusicModel || mMusicModel.getUserId().equals(remoteUserId))) {
                        remoteOnMusicEmpty();
                    }
                } else if (cmd.equals("syncMember")) {
                    AgoraMember member = new AgoraMember();
                    User user = new User();
                    user.setAvatar(jsonMsg.getString("avatar"));
                    user.setObjectId(jsonMsg.getString("userId"));

                    member.setId(user.getObjectId());
                    member.setUser(user);
                    member.setRole(AgoraMember.Role.parse(jsonMsg.getInt("role")));
                    mMainThreadDispatch.onMemberJoin(member);
                }
            } catch (JSONException exp) {
                exp.printStackTrace();
            }
        }
    };

    private RtcManager(Context mContext) {
        this.mContext = mContext;

        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        initData();
        iniRTC();
        //initMcc();
        register();
    }

    private void initData() {
    }

    private void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    private void unregister() {
        if (null != mContext) {
            mContext.unregisterReceiver(mNetworkStateReceiver);
        }
    }

    private void iniRTC() {
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = mContext;
        config.mAppId = BuildConfig.APP_ID;
        config.mEventHandler = mIRtcEngineEventHandler;

        try {
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);

            mLoggerRTC.i("SDK version:" + RtcEngine.getSdkVersion());
        } catch (Exception e) {
            e.printStackTrace();
            mLoggerRTC.e("init error", e);
        }
    }

    public void initMcc() {
        if (null == getRtcEngine()) {
            ToastUtils.toastLong(mContext, "please init rtc engine first!");
            return;
        }

        try {
            int ret = getRtcEngine().loadExtensionProvider("agora_drm_loader_extension");
            mMcc = IAgoraMusicContentCenter.create(getRtcEngine());

            //just for test
            //动态生成mcc uid
            long mccUid = (long) (Math.random() * 90000 + 10000);

            //每次初始化生成rtm token，防止token过期
            RtmTokenBuilder token = new RtmTokenBuilder();
            String rtmToken = token.buildToken(BuildConfig.APP_ID, BuildConfig.APP_CERTIFICATE, String.valueOf(mccUid), RtmTokenBuilder.Role.Rtm_User, 0);

            mConfig = new MusicContentCenterConfiguration();
            mConfig.appId = BuildConfig.APP_ID;
            mConfig.mccUid = mccUid;
            mConfig.token = rtmToken;
            mConfig.eventHandler = mIMccEventHandler;
            mMcc.initialize(mConfig);

            mAgoraMusicPlayer = mMcc.createMusicPlayer();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void destroyMcc() {
        if (null != mMcc) {
            mMcc.unregisterEventHandler();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != mMcc) {
                    mMcc.getCaches();
                }
            }
        }).start();
        IAgoraMusicContentCenter.destroy();
        if (null != mAgoraMusicPlayer) {
            mAgoraMusicPlayer.destroy();
            mAgoraMusicPlayer = null;
        }
        mMcc = null;
        mConfig.eventHandler = null;
        mConfig = null;
    }

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    public IAgoraMusicContentCenter getAgoraMusicContentCenter() {
        return mMcc;
    }

    public IAgoraMusicPlayer getAgoraMusicPlayer() {
        return mAgoraMusicPlayer;
    }

    public static RtcManager Instance(Context mContext) {
        if (instance == null) {
            synchronized (RtcManager.class) {
                if (instance == null)
                    instance = new RtcManager(mContext.getApplicationContext());
            }
        }
        return instance;
    }

    public boolean isOwner() {
        return ObjectsCompat.equals(getOwner(), getMine());
    }

    @Nullable
    public AgoraRoom getRoom() {
        return mRoom;
    }

    @Nullable
    public AgoraMember getOwner() {
        return owner;
    }

    @Nullable
    public AgoraMember getMine() {
        return mMine;
    }

//    public boolean isSinger(String userId) {
//        return singers.contains(userId);
//    }

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        mMainThreadDispatch.removeRoomEventCallback(callback);
    }

    private void onMemberRoleChanged(@NonNull AgoraMember member) {
        mLogger.i("onMemberRoleChanged() called with: member = [%s]", member);
        mMainThreadDispatch.onRoleChanged(member);
    }

    private void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
        mLogger.i("onAudioStatusChanged() called with: isMine = [%s], member = [%s]", isMine, member);
        mMainThreadDispatch.onAudioStatusChanged(isMine, member);
    }

    private void remoteOnMusicEmpty() {
        mLogger.i("remoteOnMusicEmpty() called");
        mMainThreadDispatch.onMusicEmpty();
        mMusicModel = null;
    }

    public void localOnMusicEmpty() {
        mLogger.i("localOnMusicEmpty() called");
        mMusicModel = null;
    }


    public void onMusicChanged(MemberMusicModel model) {
        mLogger.i("onMusicChanged() called with: model = [%s]", model);
        mMusicModel = model;
        mMainThreadDispatch.onMusicChanged(mMusicModel);
    }

    private final static Object musicObject = new Object();

    @Nullable
    public MemberMusicModel getMusicModel() {
        return mMusicModel;
    }

    public Completable joinRoom(AgoraRoom room) {
        mRoom = room;

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return Completable.error(new NullPointerException("mUser is empty"));
        }

        mMine = new AgoraMember();
        mMine.setRoomId(mRoom);
        mMine.setId(mUser.getObjectId());
        mMine.setUserId(mUser.getObjectId());
        mMine.setUser(mUser);
        mMine.setRole(AgoraMember.Role.Listener);

        try {
            ExampleData.updateBackgroundImage(Integer.parseInt(mRoom.getMv()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Completable.complete().andThen(joinRTC().doOnSuccess(uid -> {
                    Long streamId = uid & 0xffffffffL;
                    mMine.setStreamId(streamId);
                }).ignoreElement())
                .doOnComplete(() -> onJoinRoom());
    }

    private void onJoinRoom() {
        mMusicModel = null;
    }

    private SingleEmitter<Integer> emitterJoinRTC = null;

    private Single<Integer> joinRTC() {
        return Single.create(emitter -> {
            emitterJoinRTC = emitter;
            getRtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            getRtcEngine().enableAudio();
            getRtcEngine().enableAudioVolumeIndication(30, 10, true);
            getRtcEngine().enableVideo();
            getRtcEngine().enableLocalVideo(false);
            getRtcEngine().setParameters("{\"rtc.audio.opensl.mode\":0}");
            getRtcEngine().setParameters("{\"rtc.audio_fec\":[3,2]}");
            getRtcEngine().setParameters("{\"rtc.audio_resend\":false}");
            if (ObjectsCompat.equals(mMine, owner)) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else if (mMine.getRole() == AgoraMember.Role.Speaker) {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else {
                getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            }

            mLoggerRTC.i("joinRTC() called with: results = [%s]", mRoom);

            int ret = getRtcEngine().joinChannel(BuildConfig.RTC_TOKEN, mRoom.getChannelName(), null, Integer.parseInt(mMine.getId()));
            if (ret != Constants.ERR_OK) {
                mLoggerRTC.e("joinRTC() called error " + ret);
                emitter.onError(new Exception("join rtc room error " + ret));
                emitterJoinRTC = null;
            }
        });
    }

    public Completable leaveRoom() {
        mLogger.i("leaveRoom() called");
        if (mRoom == null) {
            return Completable.complete();
        }

        mLoggerRTC.i("leaveChannel() called");
        getRtcEngine().leaveChannel();

        mRoom = null;
        mMine = null;
        owner = null;
        return Completable.complete();
    }

    public Completable toggleSelfAudio(boolean isMute) {
        int mute = 0;
        if (isMute) mute = 1;
        mMine.setIsSelfMuted(mute);
        return Completable.complete();
    }

    public List<Double> getVoicePitchList() {
        return voicePitchList;
    }

    public void resetVoicePitchList() {
        voicePitchList.clear();
    }

    private BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isNetworkConnected = false;
                //获取WIFI连接的信息
                NetworkInfo wifiNetworkInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                //获取移动数据连接的信息
                NetworkInfo dataNetworkInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                if (wifiNetworkInfo.isConnected() || dataNetworkInfo.isConnected()) {
                    //网络连接
                    isNetworkConnected = true;
                }

                if (isNetworkConnected) {
                    mLogger.i("network connnected");
                } else {
                    mLogger.i("network disconnected");
                }
            }
        }
    };
}
