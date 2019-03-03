package com.eriyaz.social.model;

public class feedbackDetails {

    public Long createdDate;
    public String message, fromUserId, action, extraKey, extraKeyValue, itemType, id;
    public Boolean openPlayStore, fromSystem, read;

    public feedbackDetails() {
    }

    public feedbackDetails(Long createdDate, String message, String fromUserId, String action, String extraKey, String extraKeyValue, String id) {
        this.createdDate = createdDate;
        this.message = message;
        this.fromUserId = fromUserId;
        this.action = action;
        this.extraKey = extraKey;
        this.extraKeyValue = extraKeyValue;
        this.openPlayStore = false;
        this.itemType = "ITEM";
        this.fromSystem = false;
        this.id = id;
        this.read = false;
    }
}
