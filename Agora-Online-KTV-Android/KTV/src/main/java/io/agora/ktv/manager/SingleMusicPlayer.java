package io.agora.ktv.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.agora.data.manager.UserManager;
import com.agora.data.model.User;

import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SingleMusicPlayer extends BaseMusicPlayer {

    public SingleMusicPlayer(Context mContext, int role, IMediaPlayer mPlayer) {
        super(mContext, role, mPlayer);
        //RoomManager.Instance(mContext).getRtcEngine().setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO);
    }

    @Override
    public void switchRole(int role) {
        mLogger.d("switchRole() called with: role = [%s]", role);
        mRole = role;

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
        options.clientRoleType = role;
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            options.publishMicrophoneTrack = true;
            options.publishMediaPlayerAudioTrack = true;
        } else {
            options.publishMicrophoneTrack = false;
            options.publishMediaPlayerAudioTrack = false;
        }
        RoomManager.Instance(mContext).getRtcEngine().updateChannelMediaOptions(options);
    }

    @Override
    public void prepare(@NonNull MemberMusicModel music) {
        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        onPrepareResource();

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
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

                            open(musicModel);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        } else {
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
                            playByListener(musicModel);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        }
    }
}
