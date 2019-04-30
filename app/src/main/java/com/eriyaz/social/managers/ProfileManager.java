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

package com.eriyaz.social.managers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnProfileListChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Point;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.model.RequestFeedback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;
import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.enums.UploadImagePrefix;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.managers.listeners.OnProfileCreatedListener;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.PreferencesUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Kristina on 10/28/16.
 */

public class ProfileManager extends FirebaseListenersManager {

    private static final String TAG = ProfileManager.class.getSimpleName();
    private static ProfileManager instance;

    private Context context;
    private DatabaseHelper databaseHelper;


    public static ProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager(context);
        }

        return instance;
    }

    private ProfileManager(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    public Profile buildProfile(FirebaseUser firebaseUser, String largeAvatarURL) {
        Profile profile = new Profile(firebaseUser.getUid());
        profile.setEmail(firebaseUser.getEmail());
        profile.setPhone(firebaseUser.getPhoneNumber());
        profile.setUsername(firebaseUser.getDisplayName());
        if (largeAvatarURL == null && firebaseUser.getPhotoUrl() != null) {
            largeAvatarURL = firebaseUser.getPhotoUrl().toString();
        }
        profile.setPhotoUrl(largeAvatarURL);
        return profile;
    }

    public void isProfileExist(String id, final OnObjectExistListener<Profile> onObjectExistListener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child("profiles").child(id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isExist = dataSnapshot.exists();
                if (isExist) {
                    Profile profile = dataSnapshot.getValue(Profile.class);
                    if (profile.getId() == null || profile.getId().isEmpty()) isExist = false;
                }
                onObjectExistListener.onDataChanged(isExist);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void createOrUpdateProfile(final Profile profile, Uri imageUri, final OnProfileCreatedListener onProfileCreatedListener) {
        if (imageUri == null) {
            databaseHelper.createOrUpdateProfile(profile, onProfileCreatedListener);
            return;
        }

        String imageTitle = ImageUtil.generateImageTitle(UploadImagePrefix.PROFILE, profile.getId());
        UploadTask uploadTask = databaseHelper.uploadImage(imageUri, imageTitle);

        if (uploadTask != null) {
            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult().getDownloadUrl();
                        LogUtil.logDebug(TAG, "successful upload image, image url: " + String.valueOf(downloadUrl));

                        profile.setPhotoUrl(downloadUrl.toString());
                        databaseHelper.createOrUpdateProfile(profile, onProfileCreatedListener);

                    } else {
                        onProfileCreatedListener.onProfileCreated(false);
                        LogUtil.logDebug(TAG, "fail to upload image");
                    }

                }
            });
        } else {
            onProfileCreatedListener.onProfileCreated(false);
            LogUtil.logDebug(TAG, "fail to upload image");
        }
    }

    public void getProfileValue(Context activityContext, String id, final OnObjectChangedListener<Profile> listener) {
        ValueEventListener valueEventListener = databaseHelper.getProfile(id, listener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void onNewPointAddedListener(Context activityContext, String id, final OnObjectChangedListener<Point> listener) {
        ChildEventListener childEventListener = databaseHelper.onNewPointAddedListener(id, listener);
        addListenerToChildMap(activityContext, childEventListener);
    }

    public void createOrUpdateMessage(Message message, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().createMessage(message, onTaskCompleteListener);
    }

    public void sendNotification(Notification notification, String userID) {
        ApplicationHelper.getDatabaseHelper().sendNotification(notification, userID);
    }

    public void sendRequestNotification(RequestFeedback notification, String userID) {
        ApplicationHelper.getDatabaseHelper().sendRequestNotification(notification, userID);
    }
    public void blockUser(String blockUserId, String reason, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().blockUser(blockUserId, reason, onTaskCompleteListener);
    }

    public void resetUnseenNotificationCount() {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.resetUnseenNotificationCount();
    }

    public void markRatingViewed(String postId, Rating rating) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.markRatingViewed(postId, rating);
    }

    public void markNotificationRead(Notification notification) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.markNotificationRead(notification);
    }

    public void decrementUserPoints(String userId) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.decrementUserPoints(userId);
    }

    public void getProfileSingleValue(String id, final OnObjectChangedListener<Profile> listener) {
        databaseHelper.getProfileSingleValue(id, listener);
    }

    public void getNotificationsList(String userId, OnObjectChangedListener<ItemListResult> onObjectChangedListener, long date) {
        ApplicationHelper.getDatabaseHelper().getNotificationsList(userId, onObjectChangedListener, date);
    }

    public void getMessagesList(Context activityContext, String userId, OnDataChangedListener<Message> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getMessagesList(userId, onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void getSavedRecordings(Context activityContext, OnDataChangedListener<RecordingItem> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getSavedRecordings(onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void removeSavedRecording(String itemId) {
        ApplicationHelper.getDatabaseHelper().removeSavedRecording(itemId);
    }

    public void removeMessage(String messageId, final String userId, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.removeMessage(messageId, userId, onTaskCompleteListener);
    }

    public void getProfilesByRank(OnProfileListChangedListener<Profile> onDataChangedListener, int rank, String queryParameter) {
        ApplicationHelper.getDatabaseHelper().getProfilesByRank(onDataChangedListener, rank, queryParameter);
    }

    public ProfileStatus checkProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return ProfileStatus.NOT_AUTHORIZED;
        } else if (!PreferencesUtil.isProfileCreated(context)){
            return ProfileStatus.NO_PROFILE;
        } else {
            return ProfileStatus.PROFILE_CREATED;
        }
    }
}
