package com.eriyaz.social.model;

import com.eriyaz.social.enums.ItemType;

/**
 * Created by vikas on 12/2/18.
 */

public class Notification {
    private String id;
    private String fromUserId;
    private String message;
    private long createdDate;
    private String action;
    private String extraKey;
    private String extraKeyValue;
    private boolean read;
    private boolean openPlayStore;
    private boolean fromSystem;
    private ItemType itemType;
    private boolean forCommentNotification;

    public Notification() {
    }

    public Notification(ItemType itemType) {
        this.itemType = itemType;
        setId(itemType.toString());
    }

    public Notification(String fromUserId, String message, String action, String extraKey, String extraKeyValue, boolean read, boolean openPlayStore, boolean fromSystem, ItemType itemType) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.message = message;
        this.createdDate = createdDate;
        this.action = action;
        this.extraKey = extraKey;
        this.extraKeyValue = extraKeyValue;
        this.read = read;
        this.openPlayStore = openPlayStore;
        this.fromSystem = fromSystem;
        this.itemType = itemType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExtraKey() {
        return extraKey;
    }

    public void setExtraKey(String extraKey) {
        this.extraKey = extraKey;
    }

    public String getExtraKeyValue() {
        return extraKeyValue;
    }

    public void setExtraKeyValue(String extraKeyValue) {
        this.extraKeyValue = extraKeyValue;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isOpenPlayStore() {
        return openPlayStore;
    }

    public void setOpenPlayStore(boolean openPlayStore) {
        this.openPlayStore = openPlayStore;
    }

    public boolean isFromSystem() {
        return fromSystem;
    }

    public void setFromSystem(boolean fromSystem) {
        this.fromSystem = fromSystem;
    }

    public boolean isForCommentNotification() {
        return forCommentNotification;
    }

    public void setForCommentNotification(boolean forCommentNotification) {
        this.forCommentNotification = forCommentNotification;
    }

    public ItemType getItemType() {
        if (itemType == null) {
            return ItemType.ITEM;
        } else {
            return itemType;
        }
    }
}
