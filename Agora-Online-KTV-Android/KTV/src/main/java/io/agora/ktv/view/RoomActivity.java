package io.agora.ktv.view;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.fragment.app.Fragment;

import com.agora.data.provider.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtils;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomSpeakerAdapter;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
import io.agora.ktv.manager.MusicPlayer;
import io.agora.ktv.manager.MusicResourceManager;
import io.agora.ktv.manager.RoomEventCallback;
import io.agora.ktv.manager.RtcManager;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.view.dialog.WaitingDialog;
import io.agora.ktv.widget.LrcControlView;
import io.agora.lrcview.LrcLoadUtils;
import io.agora.lrcview.bean.LrcData;
import io.agora.rtc2.Constants;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * 房间界面
 *
 * @author chenhengfei@agora.io
 */
public class RoomActivity extends DataBindBaseActivity<KtvActivityRoomBinding> implements View.OnClickListener, OnItemClickListener<AgoraMember> {
    public static final String TAG_ROOM = "room";
    private final Logger.Builder mLogger = XLog.tag("RoomActivity");

    private RoomSpeakerAdapter mRoomSpeakerAdapter;
    private MusicPlayer mMusicPlayer;
    private Deque<MemberMusicModel> mMusicQueue;

    private final MusicPlayer.Callback mMusicCallback = new MusicPlayer.Callback() {

        @Override
        public void onPrepareResource() {
            mDataBinding.lrcControlView.onPrepareStatus();
        }

        @Override
        public void onResourceReady(@NonNull MemberMusicModel music) {
            File lrcFile = music.getFileLrc();
            LrcData data = LrcLoadUtils.parse(lrcFile);
            mDataBinding.lrcControlView.getLrcView().setLrcData(data);
            mDataBinding.lrcControlView.getPitchView().setLrcData(data);
        }

        @Override
        public void onMusicOpening() {
        }

        @Override
        public void onMusicOpenCompleted(long duration) {
            mDataBinding.lrcControlView.getLrcView().setTotalDuration(duration);
        }

        @Override
        public void onMusicOpenError(int error) {

        }

        @Override
        public void onMusicPlaying() {
            mDataBinding.lrcControlView.onPlayStatus();
        }

        @Override
        public void onMusicPause() {
            mDataBinding.lrcControlView.onPauseStatus();
        }

        @Override
        public void onMusicStop() {
            if (!mMusicQueue.isEmpty()) {
                RoomActivity.this.onMusicChanged(mMusicQueue.pop());
            }
        }


        @Override
        public void onMusicCompleted() {
            emptyMusic();
        }

        @Override
        public void onMusicPositionChanged(long position) {
            mDataBinding.lrcControlView.getLrcView().updateTime(position);
            mDataBinding.lrcControlView.getPitchView().updateTime(position);
        }
    };

    private final RoomEventCallback mRoomEventCallback = new RoomEventCallback() {

        @Override
        public void onMemberJoin(@NonNull AgoraMember member) {
            if (mRoomSpeakerAdapter.datas.contains(member)) {
                if (member.getRole() == AgoraMember.Role.Listener)
                    mRoomSpeakerAdapter.deleteItem(member);
            } else {
                mRoomSpeakerAdapter.addItem(member);
            }
        }

        @Override
        public void onMemberLeave(@NonNull AgoraMember member) {
            mRoomSpeakerAdapter.deleteItem(member);
        }

        @Override
        public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {

            AgoraMember mMine = RtcManager.Instance(RoomActivity.this).getMine();
            if (ObjectsCompat.equals(member, mMine)) {
                if (member.getIsSelfMuted() == 1) {
                    mDataBinding.ivMic.setImageResource(R.mipmap.ktv_room_unmic);
                } else {
                    mDataBinding.ivMic.setImageResource(R.mipmap.ktv_room_mic);
                }
            }
        }

        @Override
        public void onMusicChanged(@NonNull MemberMusicModel music) {
            mMusicQueue.offer(music);
            // 先停止 再播放下一曲
            emptyMusic();
        }

        @Override
        public void onMusicEmpty() {
            //远程切歌
            mMusicPlayer.remoteEmptyMusic();
            emptyMusic();
        }

        @Override
        public void onMusicPreLoadEvent(long songCode, String lyricUrl) {
            loadLrc(songCode, lyricUrl);
        }

        @Override
        public void onLyricResult(String requestId, String lyricUrl) {
            if (requestId.equals(mMusicPlayer.getLrcRequestId())) {
                try {
                    loadLrc(Long.parseLong(mMusicPlayer.getMusicModel().getMusicId()), lyricUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.ktv_activity_room;
    }

    @Override
    protected void iniView() {
        mRoomSpeakerAdapter = new RoomSpeakerAdapter(new ArrayList<>(), this);
        mDataBinding.rvSpeakers.setAdapter(mRoomSpeakerAdapter);
    }

    @Override
    protected void iniListener() {
        // 背景图监听
        ExampleData.getBackgroundImage().observe(this, integer -> Glide.with(this)
                .asDrawable()
                .load(ExampleData.exampleBackgrounds.get(integer))
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mDataBinding.root.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                }));

        RtcManager.Instance(this).addRoomEventCallback(mRoomEventCallback);
        mDataBinding.ivLeave.setOnClickListener(this);
        mDataBinding.ivMic.setOnClickListener(this);
        mDataBinding.ivBackgroundPicture.setOnClickListener(this);
        mDataBinding.llChooseSong.setOnClickListener(this);
        mDataBinding.ivChorus.setOnClickListener(this);

        mDataBinding.lrcControlView.setOnLrcClickListener(new LrcControlView.OnLrcActionListener() {
            @Override
            public void onProgressChanged(long time) {
                mMusicPlayer.seek(time);
            }

            @Override
            public void onStartTrackingTouch() {

            }

            @Override
            public void onStopTrackingTouch() {

            }

            @Override
            public void onSwitchOriginalClick() {
                toggleOriginal();
            }

            @Override
            public void onMenuClick() {
                showMusicMenuDialog();
            }

            @Override
            public void onPlayClick() {
                toggleStart();
            }

            @Override
            public void onChangeMusicClick() {
                showChangeMusicDialog();
            }
        });
    }

    @Override
    protected void iniData() {
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            ToastUtils.toastShort(this, "please login in");
            finish();
            return;
        }

        mMusicQueue = new LinkedList<>();

        mDataBinding.lrcControlView.setRole(LrcControlView.Role.Listener);

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);
        mDataBinding.tvName.setText(mRoom.getId());

        showJoinRoomDialog();
        RtcManager.Instance(this)
                .joinRoom(mRoom)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        closeJoinRoomDialog();
                        onJoinRoom();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        closeJoinRoomDialog();
                        ToastUtils.toastShort(RoomActivity.this, R.string.ktv_join_error);
                        doLeave();
                    }
                });
    }

    private WaitingDialog dialogJoinRoom = null;

    private void showJoinRoomDialog() {
        if (dialogJoinRoom != null && dialogJoinRoom.isShowing()) {
            return;
        }

        dialogJoinRoom = new WaitingDialog();
        dialogJoinRoom.show(getSupportFragmentManager(), getString(R.string.ktv_dialog_join_msg), () -> {

        });
    }

    private void closeJoinRoomDialog() {
        if (dialogJoinRoom == null || !dialogJoinRoom.isShowing()) {
            return;
        }

        dialogJoinRoom.dismiss();
    }

    private void onJoinRoom() {
        AgoraRoom mRoom = RtcManager.Instance(this).getRoom();
        assert mRoom != null;

        mDataBinding.lrcControlView.setLrcViewBackground(mRoom.getMVRes());

        mMusicPlayer = new MusicPlayer(getApplicationContext(), RtcManager.Instance(this).getRtcEngine(),
                RtcManager.Instance(this).getAgoraMusicPlayer(),
                RtcManager.Instance(this).getAgoraMusicContentCenter());
        mMusicPlayer.registerPlayerObserver(mMusicCallback);


        showNotOnSeatStatus();
    }

    private CountDownTimer timerOnTrial;

    private void stopOnTrialTimer() {
        if (timerOnTrial != null) {
            timerOnTrial.cancel();
            timerOnTrial = null;
        }
    }

    private void prepareMusic(final MemberMusicModel musicModel) {
        mMusicCallback.onPrepareResource();
        mMusicPlayer.preloadMusic(musicModel);
    }

    private void loadLrc(final long songCode, final String lyricUrl) {
        mLogger.d("loadLrc songCode=[%s], lrcUrl=[%s]", songCode, lyricUrl);
        if (-1 == songCode || TextUtils.isEmpty(lyricUrl)) {
            mLogger.e("loadLrc song code or lyricUrl error");
            return;
        }
        final MemberMusicModel musicModel = mMusicPlayer.getMusicModel();

        if (null != musicModel && musicModel.getMusicId().equals(String.valueOf(songCode))) {
            User user = UserManager.Instance().getUserLiveData().getValue();
            if (user == null) {
                return;
            }

            final boolean onlyLrc = !ObjectsCompat.equals(musicModel.getUserId(), user.getObjectId());
            musicModel.setLrc(lyricUrl);
            MusicResourceManager.Instance(this)
                    .loadLrc(musicModel)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(mLifecycleProvider.bindToLifecycle())
                    .subscribe(new SingleObserver<MemberMusicModel>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                        }

                        @Override
                        public void onSuccess(@NonNull MemberMusicModel musicModel) {
                            mMusicCallback.onResourceReady(musicModel);

                            if (!onlyLrc) {
                                mMusicPlayer.openMusic(songCode);
                            } else {
                                mMusicPlayer.playByListener(musicModel);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtils.toastShort(RoomActivity.this, R.string.ktv_lrc_load_fail);
                        }
                    });
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.ivLeave) {
            AgoraMember member = RtcManager.Instance(this).getMine();
            if (member != null)
                member.setRole(AgoraMember.Role.Listener);
            doLeave();
        } else if (v == mDataBinding.ivMic) {
            toggleMic();
        } else if (v == mDataBinding.ivBackgroundPicture) {
            showBackgroundPicDialog();
        } else if (v == mDataBinding.llChooseSong) {
            showChooseSongDialog();
//        } else if (v == mDataBinding.ivChorus) {
//
        }
    }

    private void showChooseSongDialog() {
        new RoomChooseSongDialog().show(getSupportFragmentManager());
    }

    private void showBackgroundPicDialog() {
        AgoraRoom room = RtcManager.Instance(this).getRoom();
        if (room == null) {
            return;
        }

        new RoomMVDialog().show(getSupportFragmentManager(), Integer.parseInt(room.getMv()));
    }

    private void doLeave() {
        emptyMusic();

        User u = UserManager.Instance().getUserLiveData().getValue();

        // 下线
        if (u != null) {
            mMusicPlayer.sendSyncMember(u.getObjectId(), AgoraMember.Role.Listener, u.getAvatar());
        }
        MemberMusicModel model = RtcManager.Instance(this).getMusicModel();
        if (model != null) {
            mMusicPlayer.sendSyncLrc(model.getMusicId(), 0, 0, false);
        }
        RtcManager.Instance(this)
                .leaveRoom()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
        finish();
    }

    /**
     * 禁用/启用 麦克风
     */
    private void toggleMic() {
        AgoraMember mMine = RtcManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        mDataBinding.ivMic.setEnabled(false);
        boolean newValue = mMine.getIsSelfMuted() == 0;
        RtcManager.Instance(this)
                .toggleSelfAudio(newValue)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.ivMic.setEnabled(true);
                        if (newValue) {
                            RtcManager.Instance(RoomActivity.this).getRtcEngine().adjustRecordingSignalVolume(0);
                        } else {
                            RtcManager.Instance(RoomActivity.this).getRtcEngine().adjustRecordingSignalVolume(100);
                        }

                        mDataBinding.ivMic.setImageResource(newValue ? R.mipmap.ktv_room_unmic : R.mipmap.ktv_room_mic);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    /**
     * 禁用/启用 原唱
     */
    private void toggleOriginal() {
        if (mMusicPlayer == null) {
            return;
        }

        if (mMusicPlayer.hasAccompaniment()) {
            mMusicPlayer.toggleOriginal();
        } else {
            mDataBinding.lrcControlView.setSwitchOriginalChecked(true);
            ToastUtils.toastShort(this, R.string.ktv_error_cut);
        }
    }

    private boolean isEar = false;
    private int volMic = 100;
    private int volMusic = 100;

    private void showMusicMenuDialog() {
        if (mMusicPlayer == null) {
            return;
        }

        new MusicSettingDialog().show(getSupportFragmentManager(), isEar, volMic, volMusic, new MusicSettingDialog.Callback() {
            @Override
            public void onEarChanged(boolean isEar) {
                RoomActivity.this.isEar = isEar;
                RtcManager.Instance(RoomActivity.this).getRtcEngine().enableInEarMonitoring(isEar);
            }

            @Override
            public void onMicVolChanged(int vol) {
                RoomActivity.this.volMic = vol;
                mMusicPlayer.setMicVolume(vol);
            }

            @Override
            public void onMusicVolChanged(int vol) {
                RoomActivity.this.volMusic = vol;
                mMusicPlayer.setMusicVolume(vol);
            }
        });
    }

    private void showChangeMusicDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ktv_room_change_music_title)
                .setMessage(R.string.ktv_room_change_music_msg)
                .setNegativeButton(R.string.ktv_cancel, null)
                .setPositiveButton(R.string.ktv_confirm, (dialog, which) -> emptyMusic())
                .show();
    }

    /**
     * 切歌
     */
    private void emptyMusic() {
        AgoraRoom mRoom = RtcManager.Instance(this).getRoom();
        if (mRoom != null) {
            mMusicPlayer.stop();
        }
        onMusicEmpty();
    }

    private void onMusicEmpty() {
        User user = UserManager.Instance().getUserLiveData().getValue();
        if (user != null) {
            RtcManager roomManager = RtcManager.Instance(this);
            MemberMusicModel model = roomManager.getMusicModel();
            if (model != null && model.getUserId() != null && model.getUserId().equals(user.getObjectId()))
                mMusicPlayer.sendSyncLrc(model.getMusicId(), 0, 0, false);
        }
        mDataBinding.lrcControlView.getLrcView().reset();
        mDataBinding.lrcControlView.onIdleStatus();
    }

    private void toggleStart() {
        if (mMusicPlayer == null) {
            return;
        }

        mMusicPlayer.togglePlay();
    }

    private void showOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.VISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.VISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.VISIBLE);
        mDataBinding.ivChorus.setVisibility(View.INVISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.GONE);
    }

    private void showNotOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.INVISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.INVISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.INVISIBLE);
        mDataBinding.ivChorus.setVisibility(View.INVISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.VISIBLE);
    }

    /**
     * 非空座位点击事件
     */
    @Override
    public void onItemClick(@NonNull AgoraMember data, View view, int position, long id) {
    }

    /**
     * 空座位点击事件
     */
    @Override
    public void onItemClick(View view, int position, long id) {
        requestSeatOn();
    }

    private void requestSeatOn() {
        AgoraMember mMine = RtcManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        if (mMine.getRole() != AgoraMember.Role.Listener) {
            return;
        }
        mMusicPlayer.switchRole(Constants.CLIENT_ROLE_BROADCASTER);
        RtcManager.Instance(RoomActivity.this).getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        mMine.setRole(AgoraMember.Role.Speaker);

        mRoomSpeakerAdapter.addItem(mMine);
        showOnSeatStatus();
        mMusicPlayer.startPublish();
    }

    private void onMusicChanged(MemberMusicModel music) {
        if (music == null) return;
        mDataBinding.lrcControlView.setMusic(music);

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        boolean onlyLrc = !ObjectsCompat.equals(music.getUserId(), mUser.getObjectId());
        if (!onlyLrc) {
            mDataBinding.lrcControlView.setRole(LrcControlView.Role.Singer);
        } else {
            mDataBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        }

        mDataBinding.lrcControlView.onPrepareStatus();

        prepareMusic(music);
    }

    @Override
    protected void onDestroy() {
        closeJoinRoomDialog();
        stopOnTrialTimer();

        RtcManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        if (mMusicPlayer != null) {
            mMusicPlayer.unregisterPlayerObserver();
            mMusicPlayer.destory();
            mMusicPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // dismiss dialog
        Fragment fragment = getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getFragments().size() - 1);
        if (fragment instanceof DataBindBaseDialog)
            ((DataBindBaseDialog<?>) fragment).dismiss();
    }
}
