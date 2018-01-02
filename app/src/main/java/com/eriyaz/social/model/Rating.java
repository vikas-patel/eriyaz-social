package com.eriyaz.social.model;

import java.util.Calendar;

/**
 * Created by vikas on 18/12/17.
 */

public class Rating {
    private String id;
    private String authorId;
    private long createdDate;
    private float rating;

    public Rating() {
        // Default constructor required for calls to DataSnapshot.getValue(Rating.class)
        this.createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public Rating(String authorId, int rating) {
        this.authorId = authorId;

        this.rating = rating;
        this.createdDate = Calendar.getInstance().getTimeInMillis();
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
}
