package io.agora.ktv.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.agora.baselibrary.base.BaseError;

import com.agora.data.provider.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.observer.DataObserver;
import com.agora.data.provider.DataRepository;
import com.agora.data.manager.RoomManager;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtils;
import io.agora.ktv.BuildConfig;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomListAdapter;
import io.agora.ktv.databinding.KtvActivityRoomListBinding;
import io.agora.ktv.widget.SpaceItemDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends DataBindBaseActivity<KtvActivityRoomListBinding> implements View.OnClickListener,
        OnItemClickListener<AgoraRoom>, EasyPermissions.PermissionCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private static final int TAG_PERMISSTION_REQUESTCODE = 1000;
    private static final String[] PERMISSTION = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    private RoomListAdapter mAdapter;

    private AgoraRoom mJoinRoom;

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.ktv_activity_room_list;
    }

    @Override
    protected void iniView() {
        mAdapter = new RoomListAdapter(null, this);
        mDataBinding.list.setLayoutManager(new GridLayoutManager(this, 2));
        mDataBinding.list.setAdapter(mAdapter);
        mDataBinding.list.addItemDecoration(new SpaceItemDecoration(this));
    }

    @Override
    protected void iniListener() {
        mDataBinding.swipeRefreshLayout.setOnRefreshListener(this);
        mDataBinding.btCrateRoom.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
        UserManager.Instance().setupDataRepository(DataRepository.Instance(this));

        showEmptyStatus();

        mDataBinding.swipeRefreshLayout.post(this::login);
    }

    private void login() {
        mDataBinding.swipeRefreshLayout.setRefreshing(true);
        UserManager.Instance()
                .loginIn()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<User>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull User user) {
                        loadRooms();
                    }
                });
    }

    private void loadRooms() {
        RoomManager.Instance()
                .getRooms()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new Observer<List<AgoraRoom>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<AgoraRoom> agoraRooms) {
                        mAdapter.setDatas(agoraRooms);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);
                        ToastUtils.toastShort(RoomListActivity.this, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);

                        if (mAdapter.getItemCount() <= 0) {
                            showEmptyStatus();
                        } else {
                            showDataStatus();
                        }
                    }
                });
    }

    private void showEmptyStatus() {
        mDataBinding.llEmpty.setVisibility(View.VISIBLE);
    }

    private void showDataStatus() {
        mDataBinding.llEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (!UserManager.Instance().isLogin()) {
            login();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (null != mJoinRoom) {
            joinRoom(mJoinRoom);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        ToastUtils.toastLong(getApplicationContext(), "you have denied permissions request");
    }

    @Override
    public void onItemClick(@NonNull AgoraRoom data, View view, int position, long id) {
        if (!checkAppId(getApplicationContext())) {
            return;
        }

        mJoinRoom = ExampleData.exampleRooms.get(position);
        if (!EasyPermissions.hasPermissions(this, PERMISSTION)) {
            EasyPermissions.requestPermissions(this, getString(R.string.ktv_error_permisstion),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
            return;
        }

        joinRoom(mJoinRoom);
    }

    private void joinRoom(AgoraRoom room) {
        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra(RoomActivity.TAG_ROOM, room);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mJoinRoom = null;
    }

    @Override
    public void onRefresh() {
        loadRooms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAppId(getApplicationContext());
    }

    private boolean checkAppId(Context context) {
        if (TextUtils.isEmpty(BuildConfig.APP_ID)) {
            ToastUtils.toastLong(context, "please check app id in local.properties first!");
            return false;
        }

        if (TextUtils.isEmpty(BuildConfig.APP_CERTIFICATE)) {
            ToastUtils.toastLong(context, "please check app certificate in local.properties first!");
            return false;
        }
        return true;
    }
}
