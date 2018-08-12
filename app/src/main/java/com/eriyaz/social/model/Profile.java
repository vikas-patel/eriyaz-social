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

import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Profile implements Serializable {

    private String id;
    private String username;
    private String email;
    private String photoUrl;
    private long points;
    private int postCount;
    private long lastPostCreatedDate;
    private int unseen;
    private int unseenMessageCount;
    private String registrationToken;
    private boolean admin;
    private String appVersion;

    public Profile() {
        // Default constructor required for calls to DataSnapshot.getValue(Profile.class)
    }

    public Profile(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public int getUnseen() {
        return unseen;
    }

    public void setUnseen(int unseen) {
        this.unseen = unseen;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public long getLastPostCreatedDate() {
        return lastPostCreatedDate;
    }

    public void setLastPostCreatedDate(long lastPostCreatedDate) {
        this.lastPostCreatedDate = lastPostCreatedDate;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("username", username);
        result.put("email", email);
        result.put("photoUrl", photoUrl);
        result.put("points", points);
        result.put("appVersion", appVersion);
        return result;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", points=" + points +
                ", postCount=" + postCount +
                ", unseen=" + unseen +
                ", registrationToken='" + registrationToken + '\'' +
                ", admin=" + admin +
                '}';
    }
}
