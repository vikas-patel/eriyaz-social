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


import android.support.annotation.NonNull;

import com.eriyaz.social.utils.FormatterUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Feedback implements ListItem {

    private String id;
    private String parentId;
    private String text;
    private String authorId;
    private long createdDate;
    private boolean removed;


    public Feedback() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Feedback(String text) {

        this.text = text;
        this.createdDate = Calendar.getInstance().getTimeInMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("text", text);
        result.put("parentId", parentId);
        result.put("createdDate", createdDate);
        result.put("removed", removed);
        result.put("createdDateText", FormatterUtil.getFirebaseDateFormat().format(new Date(createdDate)));

        return result;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", text='" + text + '\'' +
                ", authorId='" + authorId + '\'' +
                ", createdDate=" + createdDate +
                ", removed=" + removed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feedback feedback = (Feedback) o;

        return id.equals(feedback.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int getListItemType() {
        if (getParentId() != null) {
            return ListItem.TEXT_CHILD;
        } else {
            return ListItem.TEXT;
        }
    }
}
