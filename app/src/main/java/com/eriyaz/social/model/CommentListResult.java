package com.eriyaz.social.model;

import java.util.ArrayList;
import java.util.List;

public class CommentListResult {

    private boolean isMoreDataAvailable;
    private List<UserComment> items = new ArrayList<>();
    private long lastItemCreatedDate;

    public boolean isMoreDataAvailable() {
        return isMoreDataAvailable;
    }

    public void setMoreDataAvailable(boolean moreDataAvailable) {
        isMoreDataAvailable = moreDataAvailable;
    }

    public List<UserComment> getItems() {
        return items;
    }

    public void setItems(List<UserComment> items) {
        this.items = items;
    }

    public long getLastItemCreatedDate() {
        return lastItemCreatedDate;
    }

    public void setLastItemCreatedDate(long lastItemCreatedDate) {
        this.lastItemCreatedDate = lastItemCreatedDate;
    }
}
