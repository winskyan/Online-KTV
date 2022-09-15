package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 猜测是将 {@link User} 与 {@link AgoraRoom} 关联的类
 * 标识该用户所在的房间、streamId、角色
 * <p>
 * {@link #roomId User#userId} 与 {@link #userId}相同 《==》 {@link AgoraMember.Role#Owner}
 * 否则 role = {@link AgoraMember.Role#Listener}
 * 用户主动上麦 《==》role = {@link AgoraMember.Role#Speaker}
 */
public class AgoraMember implements Parcelable {
    public static final String TABLE_NAME = "MEMBER_KTV";

    public static final String COLUMN_ROOMID = "roomId";
    public static final String COLUMN_STREAMID = "streamId";
    public static final String COLUMN_USERID = "userId";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_ISAUDIOMUTED = "isMuted";
    public static final String COLUMN_ISSELFAUDIOMUTED = "isSelfMuted";

    public enum Role {
        Listener(0), Owner(1), Speaker(2);
        private int value;

        Role(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static AgoraMember.Role parse(int value) {
            if (value == 0) {
                return AgoraMember.Role.Listener;
            } else if (value == 1) {
                return AgoraMember.Role.Owner;
            } else if (value == 2) {
                return AgoraMember.Role.Speaker;
            }
            return AgoraMember.Role.Listener;
        }

    }

    private String id;
    private AgoraRoom roomId;
    private String userId;
    private Long streamId = 0L;
    private Role role = Role.Listener;
    private int isMuted = 0;
    private int isSelfMuted = 0;

    private User user;

    public AgoraMember() {

    }

    protected AgoraMember(Parcel in) {
        id = in.readString();
        roomId = in.readParcelable(AgoraRoom.class.getClassLoader());
        userId = in.readString();
        if (in.readByte() == 0) {
            streamId = null;
        } else {
            streamId = in.readLong();
        }
        isMuted = in.readInt();
        isSelfMuted = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeParcelable(roomId, flags);
        dest.writeString(userId);
        if (streamId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(streamId);
        }
        dest.writeInt(isMuted);
        dest.writeInt(isSelfMuted);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AgoraMember> CREATOR = new Creator<AgoraMember>() {
        @Override
        public AgoraMember createFromParcel(Parcel in) {
            return new AgoraMember(in);
        }

        @Override
        public AgoraMember[] newArray(int size) {
            return new AgoraMember[size];
        }
    };

    public AgoraRoom getRoomId() {
        return roomId;
    }

    public void setRoomId(AgoraRoom roomId) {
        this.roomId = roomId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getIsMuted() {
        return isMuted;
    }

    public void setIsMuted(int isMuted) {
        this.isMuted = isMuted;
    }

    public int getIsSelfMuted() {
        return isSelfMuted;
    }

    public void setIsSelfMuted(int isSelfMuted) {
        this.isSelfMuted = isSelfMuted;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgoraMember that = (AgoraMember) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "AgoraMember{" +
                "id='" + id + '\'' +
                ", room=" + roomId +
                ", userId='" + userId + '\'' +
                ", streamId=" + streamId +
                ", role=" + role +
                ", isAudioMuted=" + isMuted +
                ", isSelfAudioMuted=" + isSelfMuted +
                '}';
    }
}
