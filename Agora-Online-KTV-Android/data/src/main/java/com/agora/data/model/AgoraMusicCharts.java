package com.agora.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class AgoraMusicCharts implements Parcelable {
    public String name;
    public int type;

    public AgoraMusicCharts() {

    }

    protected AgoraMusicCharts(Parcel in) {
        name = in.readString();
        type = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AgoraMusicCharts> CREATOR = new Creator<AgoraMusicCharts>() {
        @Override
        public AgoraMusicCharts createFromParcel(Parcel in) {
            return new AgoraMusicCharts(in);
        }

        @Override
        public AgoraMusicCharts[] newArray(int size) {
            return new AgoraMusicCharts[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return "AgoraMusicCharts{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
