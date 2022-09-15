package io.agora.ktv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;

import java.io.File;
import java.io.Serializable;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/9
 */
public class MemberMusicModel implements Parcelable {
    public enum Type implements Serializable {
        Default, MiGu;
    }

    private String id;
    private String name;
    private String singer;
    private String poster;
    private String userId;
    private AgoraRoom roomId;
    private String musicId;

    private String song;
    private String lrc;

    private File fileMusic;
    private File fileLrc;

    private Type type = Type.MiGu;

    public MemberMusicModel(String musicId) {
        this.musicId = musicId;
    }

    public MemberMusicModel(MusicModel data) {
        this.name = data.getName();
        this.musicId = data.getMusicId();
        this.singer = data.getSinger();
        this.poster = data.getPoster();
    }

    protected MemberMusicModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        roomId = in.readParcelable(AgoraRoom.class.getClassLoader());
        musicId = in.readString();
        song = in.readString();
        singer = in.readString();
        poster = in.readString();
        lrc = in.readString();
        fileMusic = (File) in.readSerializable();
        fileLrc = (File) in.readSerializable();
        type = (Type) in.readSerializable();
        userId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeParcelable(roomId, flags);
        dest.writeString(musicId);
        dest.writeString(song);
        dest.writeString(singer);
        dest.writeString(poster);
        dest.writeString(lrc);
        dest.writeSerializable(fileMusic);
        dest.writeSerializable(fileLrc);
        dest.writeSerializable(type);
        dest.writeString(userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MemberMusicModel> CREATOR = new Creator<MemberMusicModel>() {
        @Override
        public MemberMusicModel createFromParcel(Parcel in) {
            return new MemberMusicModel(in);
        }

        @Override
        public MemberMusicModel[] newArray(int size) {
            return new MemberMusicModel[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AgoraRoom getRoomId() {
        return roomId;
    }

    public void setRoomId(AgoraRoom roomId) {
        this.roomId = roomId;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getLrc() {
        return lrc;
    }

    public void setLrc(String lrc) {
        this.lrc = lrc;
    }

    public File getFileMusic() {
        return fileMusic;
    }

    public void setFileMusic(File fileMusic) {
        this.fileMusic = fileMusic;
    }

    public File getFileLrc() {
        return fileLrc;
    }

    public void setFileLrc(File fileLrc) {
        this.fileLrc = fileLrc;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemberMusicModel that = (MemberMusicModel) o;

        if (id == null || id.isEmpty()) {
            return musicId.equals(that.musicId);
        }

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "MemberMusicModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", singer='" + singer + '\'' +
                ", poster='" + poster + '\'' +
                ", userId='" + userId + '\'' +
                ", roomId=" + roomId +
                ", musicId='" + musicId + '\'' +
                ", song='" + song + '\'' +
                ", lrc='" + lrc + '\'' +
                ", fileMusic=" + fileMusic +
                ", fileLrc=" + fileLrc +
                ", type=" + type +
                '}';
    }
}
