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

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.enums.UploadImagePrefix;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.managers.listeners.OnProfileCreatedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Point;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.PreferencesUtil;
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

/**
 * Created by Kristina on 10/28/16.
 */

public class BlockUserManager extends FirebaseListenersManager {

    private static final String TAG = BlockUserManager.class.getSimpleName();
    private static BlockUserManager instance;

    private Context context;
    private DatabaseHelper databaseHelper;


    public static BlockUserManager getInstance(Context context) {
        if (instance == null) {
            instance = new BlockUserManager(context);
        }

        return instance;
    }

    private BlockUserManager(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    public void getBlockedByList(Context activityContext, String userId, OnDataChangedListener<String> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getBlockedByList(userId, onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }
}
