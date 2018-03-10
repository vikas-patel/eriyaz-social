package com.eriyaz.social.model;

/**
 * Created by vikas on 25/1/18.
 */

public class Point {
    private long creationDate;
    private String type;
    private String action;
    private int value;

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Point{" +
                "creationDate=" + creationDate +
                ", type='" + type + '\'' +
                ", action='" + action + '\'' +
                ", value=" + value +
                '}';
    }
}
