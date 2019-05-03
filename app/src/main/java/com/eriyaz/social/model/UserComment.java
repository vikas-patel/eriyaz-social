package com.eriyaz.social.model;

import com.eriyaz.social.enums.ItemType;

public class UserComment {

    private String id;
//    private String commentId;
    private String text;
//    private String authorId;
    private String postId;
    private String audioPath;
    private String audioTitle;
    private long createdDate;
    private int reputationPoints;
    private int likesCount;
//    private String postTitle;
    private ItemType itemType;

    public UserComment(ItemType itemType) {
        this.itemType = itemType;
        setId(itemType.toString());
    }
    public UserComment() {}

    public ItemType getItemType() {
        if (itemType == null) {
            return ItemType.ITEM;
        } else {
            return itemType;
        }
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

//    public String getCommentId() {
//        return commentId;
//    }
//
//    public void setCommentId(String commentId) {
//        this.commentId = commentId;
//    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

//    public String getAuthorId() {
//        return authorId;
//    }
//
//    public void setAuthorId(String authorId) {
//        this.authorId = authorId;
//    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getAudioTitle() {
        return audioTitle;
    }

    public void setAudioTitle(String audioTitle) {
        this.audioTitle = audioTitle;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public int getReputationPoints() {
        return reputationPoints;
    }

    public void setReputationPoints(int reputationPoints) {
        this.reputationPoints = reputationPoints;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

//    public String getPostTitle() {
//        return postTitle;
//    }
//
//    public void setPostTitle(String postTitle) {
//        this.postTitle = postTitle;
//    }
}
