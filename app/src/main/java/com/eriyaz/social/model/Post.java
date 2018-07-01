/*
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.eriyaz.social.model;

import android.os.Parcelable;

import com.eriyaz.social.enums.BoughtFeedbackStatus;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.utils.FormatterUtil;
import com.google.firebase.database.ServerValue;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kristina on 10/28/16.
 */

public class Post implements Serializable, LazyLoading {

    private String id;
    private String title;
    private String description;
    private long createdDate;
    private String imagePath;
    private String imageTitle;
    private String authorId;
    private long commentsCount;
    private long likesCount;
    private long ratingsCount;
    private float averageRating;
    private long watchersCount;
    private boolean hasComplain;
    private boolean removed;
    private ItemType itemType;
    private long audioDuration;
    private boolean longRecording;
    private BoughtFeedbackStatus boughtFeedbackStatus;
    private boolean anonymous;
    private String avatarImageUrl;
    private String nickName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Post post = (Post) o;

        return id.equals(post.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Post() {
        this.createdDate = new Date().getTime();

        itemType = ItemType.ITEM;
    }

    public Post(ItemType itemType) {
        this.itemType = itemType;
        setId(itemType.toString());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public long getWatchersCount() {
        return watchersCount;
    }

    public void setWatchersCount(long watchersCount) {
        this.watchersCount = watchersCount;
    }

    public boolean isHasComplain() {
        return hasComplain;
    }

    public void setHasComplain(boolean hasComplain) {
        this.hasComplain = hasComplain;
    }

    public long getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(long ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public long getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(long audioDuration) {
        this.audioDuration = audioDuration;
    }

    public boolean isLongRecording() {
        return longRecording;
    }

    public void setLongRecording(boolean longRecording) {
        this.longRecording = longRecording;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getAvatarImageUrl() {
        return avatarImageUrl;
    }

    public void setAvatarImageUrl(String avatarImageUrl) {
        this.avatarImageUrl = avatarImageUrl;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("title", title);
        result.put("description", description);
        result.put("createdDate", ServerValue.TIMESTAMP);
        result.put("imagePath", imagePath);
        result.put("imageTitle", imageTitle);
        result.put("authorId", authorId);
        result.put("commentsCount", commentsCount);
        result.put("likesCount", likesCount);
        result.put("ratingsCount", ratingsCount);
        result.put("averageRating", averageRating);
        result.put("audioDuration", audioDuration);
        result.put("watchersCount", watchersCount);
        result.put("hasComplain", hasComplain);
        result.put("longRecording", longRecording);
        result.put("removed", removed);
        result.put("avatarImageUrl", avatarImageUrl);
        result.put("anonymous", anonymous);
        result.put("nickName", nickName);
        result.put("createdDateText", FormatterUtil.getFirebaseDateFormat().format(new Date(createdDate)));

        return result;
    }

    @Override
    public ItemType getItemType() {
        return itemType;
    }

    @Override
    public void setItemType(ItemType itemType) {

    }

    public BoughtFeedbackStatus getBoughtFeedbackStatus() {
        return boughtFeedbackStatus;
    }

    public void setBoughtFeedbackStatus(BoughtFeedbackStatus boughtFeedbackStatus) {
        this.boughtFeedbackStatus = boughtFeedbackStatus;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", createdDate=" + createdDate +
                ", imagePath='" + imagePath + '\'' +
                ", imageTitle='" + imageTitle + '\'' +
                ", authorId='" + authorId + '\'' +
                ", commentsCount=" + commentsCount +
                ", likesCount=" + likesCount +
                ", ratingsCount=" + ratingsCount +
                ", averageRating=" + averageRating +
                ", watchersCount=" + watchersCount +
                ", hasComplain=" + hasComplain +
                ", audioDuration=" + audioDuration +
                ", itemType=" + itemType +
                '}';
    }
}
