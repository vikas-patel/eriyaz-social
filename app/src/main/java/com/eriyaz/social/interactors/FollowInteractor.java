/*
 *
 * Copyright 2018 Rozdoum
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
 *
 */

package com.eriyaz.social.interactors;

import android.app.Activity;
import android.content.Context;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.FollowingPost;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Alexey on 05.06.18.
 */

public class FollowInteractor {

    private static final String TAG = FollowInteractor.class.getSimpleName();
    private static FollowInteractor instance;

    private DatabaseHelper databaseHelper;
    private Context context;

    public static FollowInteractor getInstance(Context context) {
        if (instance == null) {
            instance = new FollowInteractor(context);
        }

        return instance;
    }

    private FollowInteractor(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    public void getFollowingPosts(String userId, OnDataChangedListener<FollowingPost> listener) {
        getFollowersRef(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<FollowingPost> list = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FollowingPost followingPost = snapshot.getValue(FollowingPost.class);
                            list.add(followingPost);
                        }

                        Collections.reverse(list);

                        listener.onListChanged(list);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        LogUtil.logDebug(TAG, "getBookmarkPosts, onCancelled");
                    }
                });
    }

    private DatabaseReference getFollowersRef(String userId) {
        return databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.BOOKMARK_POSTS_DB_KEY)
                .child(userId);
    }

    private Task<Void> addFollowPost(String userId, String postId) {
        FollowingPost followPost = new FollowingPost();
        followPost.setPostId(postId);
        return getFollowersRef(userId)
                .child(postId)
                .setValue(followPost);
    }

    public void followPost(Activity activity, String userId, String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        addFollowPost(userId, postId)
                .addOnCompleteListener(activity, task -> {
                    onTaskCompleteListener.onTaskComplete(task.isSuccessful());
                });
    }
}
