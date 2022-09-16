package com.agora.data.provider;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.inf.IDataRepository;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DataRepositoryImpl implements IDataRepository {

    @Override
    public Observable<User> login(@NonNull User user) {
        //目前无现实，暂保留接口
        return null;
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        //目前无现实，暂保留接口
        return null;
    }

    @Override
    public Observable<User> getUser(@NonNull String userId) {
        //目前无现实，暂保留接口
        return null;
    }

    @Override
    public Observable<List<MusicModel>> getMusics(@Nullable String searchKey) {
        return Observable.just(ExampleData.exampleSongs);
    }

    @Override
    public Observable<MusicModel> getMusic(@NonNull String musicId) {
        MusicModel musicModel = null;

        for (MusicModel exampleSong : ExampleData.exampleSongs) {
            if (exampleSong.getMusicId().equals(musicId)) {
                musicModel = exampleSong;
                break;
            }
        }

        assert musicModel != null;
        return Observable.just(musicModel);
    }

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    public Completable download(@NonNull File file, @NonNull String url) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@NonNull CompletableEmitter emitter) throws Exception {
                Log.d("down", file.getName() + ", url: " + url);

                if (file.isDirectory()) {
                    emitter.onError(new Throwable("file is a Directory"));
                    return;
                }

                Request request = new Request.Builder().url(url).build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        emitter.onError(e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        ResponseBody body = response.body();
                        if (body == null) {
                            emitter.onError(new Throwable("body is empty"));
                            return;
                        }

                        long total = body.contentLength();

                        if (file.exists() && file.length() == total) {
                            emitter.onComplete();
                            return;
                        }

                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len = 0;
                        FileOutputStream fos = null;
                        try {
                            is = body.byteStream();
                            fos = new FileOutputStream(file);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                Log.d("down", file.getName() + ", progress: " + progress);
                            }
                            fos.flush();
                            // 下载完成
                            Log.d("down", file.getName() + " onComplete");
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        });
    }
}