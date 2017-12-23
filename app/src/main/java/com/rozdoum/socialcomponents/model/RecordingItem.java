package com.rozdoum.socialcomponents.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Daniel on 12/30/2014.
 */
public class RecordingItem implements Parcelable {
    private String mName; // file name
    private String mFilePath; //file path
    private long mLength; // length of recording in seconds
    private long mTime; // date/time of the recording

    public RecordingItem()
    {
    }

    public RecordingItem(String mName, String mFilePath, long mLength) {
        this.mName = mName;
        this.mFilePath = mFilePath;
        this.mLength = mLength;
        this.mTime = System.currentTimeMillis();
    }

    public RecordingItem(Parcel in) {
        mName = in.readString();
        mFilePath = in.readString();
        mLength = in.readInt();
        mTime = in.readLong();
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

    public void setLength(int length) {
        mLength = length;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
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
        dest.writeLong(mTime);
        dest.writeString(mFilePath);
        dest.writeString(mName);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}