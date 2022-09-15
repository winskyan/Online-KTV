package io.agora.ktv.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMusicCharts;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongsAdapter;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.manager.RoomEventCallback;
import io.agora.ktv.manager.RtcManager;
import io.agora.ktv.widget.SpaceItemDecoration;
import io.agora.musiccontentcenter.IAgoraMusicContentCenter;

/**
 * 歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongsFragment extends DataBindBaseFragment<KtvFragmentSongListBinding> implements OnItemClickListener<MusicModel> {
    private final Logger.Builder mLogger = XLog.tag("SongsFragment");

    private static final int MUSIC_PAGE_SIZE = 10;

    private IAgoraMusicContentCenter mMcc;
    private String mMusicChartsRequestId;
    private String mMusicCollectionRequestId;
    private int mMusicChartId = -1;
    private int mCurrentPage = 0;

    private final RoomEventCallback callback = new RoomEventCallback() {
        @Override
        public void onMusicChartsResult(String requestId, AgoraMusicCharts[] musicCharts) {
            if (!TextUtils.isEmpty(mMusicChartsRequestId) && mMusicChartsRequestId.equals(requestId)) {
                //加载第一个排行榜
                if (musicCharts.length > 0) {
                    loadMusicsByChartId(musicCharts[0].type);
                }
            }
        }

        @Override
        public void onMusicCollectionResult(String requestId, MusicModel[] musics) {
            if (!TextUtils.isEmpty(mMusicCollectionRequestId) && mMusicCollectionRequestId.equals(requestId)) {
                if (musics.length > 0) {
                    onLoadMusics(Arrays.asList(musics));
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        RtcManager.Instance(getContext()).addRoomEventCallback(callback);
    }

    @Override
    public void onStop() {
        super.onStop();
        RtcManager.Instance(getContext()).removeRoomEventCallback(callback);
    }

    public static SongsFragment newInstance() {
        SongsFragment mFragment = new SongsFragment();
        return mFragment;
    }

    private SongsAdapter mAdapter;

    @Override
    public void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_fragment_song_list;
    }

    @Override
    public void iniView(View view) {

    }

    @Override
    public void iniListener() {
    }

    @Override
    public void iniData() {
        mAdapter = new SongsAdapter(new ArrayList<>(), this);
        mDataBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        mDataBinding.list.addItemDecoration(new SpaceItemDecoration(requireContext()));
        mDataBinding.list.setAdapter(mAdapter);

        mDataBinding.swipeRefreshLayout.setEnabled(false);

        mDataBinding.llEmpty.setVisibility(View.GONE);

        mMcc = RtcManager.Instance(requireContext()).getAgoraMusicContentCenter();
        loadMusics(null);
    }

    private void loadMusics(String searchKey) {
        //onLoadMusics(ExampleData.exampleSongs);
        mMusicChartsRequestId = mMcc.getMusicCharts();

    }

    private void loadMusicsByChartId(int musicChartId) {
        if (-1 == musicChartId) {
            return;
        }
        //默认加载十首歌曲
        mMusicCollectionRequestId = mMcc.getMusicCollectionByMusicChartId(musicChartId, mCurrentPage, MUSIC_PAGE_SIZE);
    }

    private void onLoadMusics(List<MusicModel> list) {
        if (list.isEmpty()) {
            mDataBinding.llEmpty.setVisibility(View.VISIBLE);
        } else {
            mDataBinding.llEmpty.setVisibility(View.GONE);
        }
        mAdapter.setDatas(list);
    }

    @Override
    public void onItemClick(@NonNull MusicModel data, View view, int position, long id) {
        AgoraRoom mRoom = RtcManager.Instance(requireContext()).getRoom();
        if (mRoom == null) {
            return;
        }

        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        MemberMusicModel model = new MemberMusicModel(data);
        model.setRoomId(mRoom);
        model.setUserId(mUser.getObjectId());
        model.setId(data.getMusicId());
        model.setMusicId(data.getMusicId());

        RtcManager.Instance(requireContext()).onMusicChanged(model);
        requireActivity().onBackPressed();
    }

}
