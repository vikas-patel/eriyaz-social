package com.eriyaz.social.model;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by vikas on 14/7/18.
 */

public class Flag implements Serializable{
    private String id;
    private String postId;
    private String ratingId;
    private String commentId;
    private String reason;
    private String flaggedUser;
    private String flaggedBy;
    private long createdDate;

    public Flag(String postId, String ratingId, String commentId, String reason, String flaggedUser, String flaggedBy) {
        this.postId = postId;
        this.ratingId = ratingId;
        this.commentId = commentId;
        this.reason = reason;
        this.flaggedUser = flaggedUser;
        this.flaggedBy = flaggedBy;
        createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getRatingId() {
        return ratingId;
    }

    public void setRatingId(String ratingId) {
        this.ratingId = ratingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFlaggedUser() {
        return flaggedUser;
    }

    public void setFlaggedUser(String flaggedUser) {
        this.flaggedUser = flaggedUser;
    }

    public String getFlaggedBy() {
        return flaggedBy;
    }

    public void setFlaggedBy(String flaggedBy) {
        this.flaggedBy = flaggedBy;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }
}
