/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.eriyaz.social;

import android.support.annotation.NonNull;

import com.eriyaz.social.managers.DatabaseHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.List;

public class Application extends android.app.Application {

    public static final String TAG = Application.class.getSimpleName();
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private List<String> playPostList = new ArrayList<>();
    private List<String> blockedByList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationHelper.initDatabaseHelper(this);
        DatabaseHelper.getInstance(this).subscribeToNewPosts();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfig();
    }

    public void setBlockedByList(List aBlockedList) {
        blockedByList = aBlockedList;
    }

    public boolean isBlocked(String authorId) {
        if (blockedByList != null && blockedByList.contains(authorId)) return true;
        return false;
    }

    public void addPlayedPost(String postId) {
        playPostList.add(postId);
    }

    public boolean isPostPlayed(String postId) {
        return playPostList.contains(postId);
    }

    private void fetchRemoteConfig() {
        mFirebaseRemoteConfig.fetch()
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        // After config data is successfully fetched, it must be activated before newly fetched
                        // values are returned.
                        mFirebaseRemoteConfig.activateFetched();
                    } else {
                        // don't do anything
                    }
                }
            });
    }
}
