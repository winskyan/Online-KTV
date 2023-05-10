package io.agora.ktv.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.palette.graphics.Palette;

import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;


import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvLayoutLrcControlViewBinding;
import io.agora.lyrics_view.LrcView;
import io.agora.lyrics_view.PitchView;

/**
 * 歌词控制View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/16
 */
public class LrcControlView extends FrameLayout implements View.OnClickListener {

    protected KtvLayoutLrcControlViewBinding mDataBinding;

    public LrcView getLrcView() {
        return mDataBinding.ilActive.lrcView;
    }

    public PitchView getPitchView() {
        return mDataBinding.ilActive.pitchView;
    }

    public RangeSeekBar getSongSlider() {
        return mDataBinding.ilActive.songProgress;
    }


    public enum Role {
        Singer, Listener
    }

    private Role mRole = Role.Listener;
    private MemberMusicModel mMusic;
    private OnLrcActionListener mOnLrcActionListener;
    private OnPitchViewSingScoreListener mOnPitchViewSingScoreListener;

    private OnSpeedItemSelectedListener mOnSpeedItemSelectedListener;

    public LrcControlView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.ktv_layout_lrc_control_view, this, true);

        mDataBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        initListener();

        String[] speedArrayValue = context.getResources().getStringArray(R.array.ktv_song_play_speed_array);
        ArrayAdapter<String> speedAdapter = new ArrayAdapter<String>(context, R.layout.ktv_speed_spinner, speedArrayValue);
        speedAdapter.setDropDownViewResource(R.layout.ktv_spinner_dropdown_stytle);
        mDataBinding.ilActive.speed.setAdapter(speedAdapter);
        mDataBinding.ilActive.speed.setSelection(1);

        mDataBinding.ilActive.speed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (null != mOnSpeedItemSelectedListener) {
                    try {
                        mOnSpeedItemSelectedListener.onSpeedItemSelected((int) (Float.parseFloat(speedArrayValue[position]) * 100));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initListener() {
        mDataBinding.ilActive.switchOriginal.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicMenu.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicStart.setOnClickListener(this);
        mDataBinding.ilActive.ivChangeSong.setOnClickListener(this);
    }

    public void setOnLrcClickListener(OnLrcActionListener mOnLrcActionListener) {
        this.mOnLrcActionListener = mOnLrcActionListener;
        mDataBinding.ilActive.lrcView.setSeekListener(this.mOnLrcActionListener);
    }

    public void setOnPitchViewSingScoreListener(OnPitchViewSingScoreListener onPitchViewSingScoreListener) {
        this.mOnPitchViewSingScoreListener = onPitchViewSingScoreListener;
        mDataBinding.ilActive.pitchView.setSingScoreListener(this.mOnPitchViewSingScoreListener);
    }

    public void setSongProgressSliderListener(OnRangeChangedListener onChangeListener) {
        mDataBinding.ilActive.songProgress.setOnRangeChangedListener(onChangeListener);
    }

    public void setSpeedItemSelectedListener(OnSpeedItemSelectedListener onSpeedItemSelectedListener) {
        this.mOnSpeedItemSelectedListener = onSpeedItemSelectedListener;
    }

    public void onPrepareStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        if (this.mRole == Role.Singer) {
            mDataBinding.ilActive.lrcView.setEnableDrag(true);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mDataBinding.ilActive.lrcView.setEnableDrag(false);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }
    }

    public void onPlayStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_pause);
    }

    public void onPauseStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_play);
    }

    public void onIdleStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.clActive.setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);
    }

    public void setRole(@NonNull Role mRole) {
        this.mRole = mRole;

        if (this.mRole == Role.Singer) {
            mDataBinding.ilActive.lrcView.setEnableDrag(true);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mDataBinding.ilActive.lrcView.setEnableDrag(false);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }
    }

    public void setMusic(@NonNull MemberMusicModel mMusic) {
        mDataBinding.ilActive.lrcView.reset();
        //mDataBinding.ilActive.pitchView.reset();
        mDataBinding.ilActive.pitchView.setLrcData(null);

        this.mMusic = mMusic;
        mDataBinding.tvMusicName.setText(this.mMusic.getName());
    }

    public void setLrcViewBackground(@DrawableRes int resId) {
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), resId);
        Palette.from(mBitmap).generate(palette -> {
            if (palette == null) {
                return;
            }

            int defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_highligh);
            mDataBinding.ilActive.lrcView.setCurrentColor(palette.getLightVibrantColor(defaultColor));

            defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_nomal);
            mDataBinding.ilActive.lrcView.setNormalColor(palette.getLightMutedColor(defaultColor));
        });
        mDataBinding.clActive.setBackgroundResource(resId);
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.ilActive.switchOriginal) {
            mOnLrcActionListener.onSwitchOriginalClick();
        } else if (v == mDataBinding.ilActive.ivMusicMenu) {
            mOnLrcActionListener.onMenuClick();
        } else if (v == mDataBinding.ilActive.ivMusicStart) {
            mOnLrcActionListener.onPlayClick();
        } else if (v == mDataBinding.ilActive.ivChangeSong) {
            mOnLrcActionListener.onChangeMusicClick();
        }
    }

    public void setSwitchOriginalChecked(boolean checked) {
        mDataBinding.ilActive.switchOriginal.setChecked(checked);
    }

    public interface OnLrcActionListener extends LrcView.OnLyricsSeekListener {
        void onSwitchOriginalClick();

        void onMenuClick();

        void onPlayClick();

        void onChangeMusicClick();
    }

    public interface OnPitchViewSingScoreListener extends PitchView.OnSingScoreListener {

    }

    public interface OnSpeedItemSelectedListener {
        void onSpeedItemSelected(int speed);
    }
}
