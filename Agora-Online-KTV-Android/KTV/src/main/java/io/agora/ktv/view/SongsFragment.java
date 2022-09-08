package io.agora.ktv.view;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.data.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.provider.DataRepositroy;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongsAdapter;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.widget.SpaceItemDecoration;
import io.agora.musiccontentcenter.IAgoraMusicContentCenter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongsFragment extends DataBindBaseFragment<KtvFragmentSongListBinding> implements OnItemClickListener<MusicModel> {

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

        loadMusics(null);
    }

    private void loadMusics(String searchKey) {
        onLoadMusics(ExampleData.exampleSongs);
        IAgoraMusicContentCenter mcc = RoomManager.Instance(requireContext()).getAgoraMusicContentCenter();
        String searchResult = mcc.searchMusic(null, 0, 10);
        String charts = mcc.getMusicCharts();
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
        AgoraRoom mRoom = RoomManager.Instance(requireContext()).getRoom();
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

        RoomManager.Instance(requireContext()).onMusicChanged(model);
        requireActivity().onBackPressed();
    }

}
