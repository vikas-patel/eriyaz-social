package com.eriyaz.social.model;

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
    private String postId;

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
}
