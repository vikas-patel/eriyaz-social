package com.eriyaz.social.model;

import java.util.Calendar;

/**
 * Created by vikas on 12/2/18.
 */

public class Message {
    private String id;
    private String senderId;
    private String text;
    private long createdDate;

    public Message() {
    }

    public Message(String text) {
        this.text = text;
        this.createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }
}
