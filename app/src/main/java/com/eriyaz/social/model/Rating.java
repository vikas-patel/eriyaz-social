package com.eriyaz.social.model;

import com.eriyaz.social.enums.ItemType;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by vikas on 18/12/17.
 */

public class Rating implements Serializable {
    public static final String RATING_ID_EXTRA_KEY = "RATING.RATING_ID_EXTRA_KEY";
    private String id;
    private String authorId;
    private long createdDate;
    private float rating;
    private int normalizedRating;
    private String detailedText;
    // only for rated post
    private String postId;
    private boolean viewedByPostAuthor;
    private boolean isRemoved;
    private ItemType itemType;

    public Rating(ItemType itemType) {
        this.itemType = itemType;
        setId(itemType.toString());
    }

    public Rating() {
        // Default constructor required for calls to DataSnapshot.getValue(Rating.class)
        this.createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public Rating(String authorId, int rating) {
        this.authorId = authorId;

        this.rating = rating;
        this.createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public void reinit() {
        this.id = null;
        this.authorId = null;
        this.rating = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getNormalizedRating() {
        return normalizedRating;
    }

    public void setNormalizedRating(int normalizedRating) {
        this.normalizedRating = normalizedRating;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public boolean isViewedByPostAuthor() {
        return viewedByPostAuthor;
    }

    public void setViewedByPostAuthor(boolean viewedByPostAuthor) {
        this.viewedByPostAuthor = viewedByPostAuthor;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public String getDetailedText() {
        return detailedText;
    }

    public void setDetailedText(String detailedText) {
        this.detailedText = detailedText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rating rating1 = (Rating) o;

        if (Float.compare(rating1.rating, rating) != 0) return false;
        return id.equals(rating1.id);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (rating != +0.0f ? Float.floatToIntBits(rating) : 0);
        return result;
    }

    public ItemType getItemType() {
        if (itemType == null) {
            return ItemType.ITEM;
        } else {
            return itemType;
        }
    }
}
