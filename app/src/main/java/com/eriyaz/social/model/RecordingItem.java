package com.eriyaz.social.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Daniel on 12/30/2014.
 */
public class RecordingItem implements Parcelable, Serializable {
    private String id;
    private String mName; // file name
    private String mFilePath; //file path
    private long mLength; // length of recording in seconds
    private long time;
    private boolean isServer;

    public RecordingItem()
    {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RecordingItem(String mName, String mFilePath, long mLength) {
        this.mName = mName;
        this.mFilePath = mFilePath;
        this.mLength = mLength;
        time = Calendar.getInstance().getTimeInMillis();
    }

    public RecordingItem(Parcel in) {
        mName = in.readString();
        mFilePath = in.readString();
        mLength = in.readInt();
        time = in.readLong();
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public long getLength() {
        return mLength;
    }

    public void setLength(long length) {
        mLength = length;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public static final Parcelable.Creator<RecordingItem> CREATOR = new Parcelable.Creator<RecordingItem>() {
        public RecordingItem createFromParcel(Parcel in) {
            return new RecordingItem(in);
        }

        public RecordingItem[] newArray(int size) {
            return new RecordingItem[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mLength);
        dest.writeString(mFilePath);
        dest.writeString(mName);
        dest.writeLong(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}