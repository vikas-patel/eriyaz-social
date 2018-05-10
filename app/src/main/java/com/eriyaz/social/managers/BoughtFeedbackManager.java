/*
 *
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
 *
 */

package com.eriyaz.social.managers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ValueEventListener;

public class BoughtFeedbackManager extends FirebaseListenersManager {

    private static final String TAG = BoughtFeedbackManager.class.getSimpleName();
    private static BoughtFeedbackManager instance;

    private Context context;

    public static BoughtFeedbackManager getInstance(Context context) {
        if (instance == null) {
            instance = new BoughtFeedbackManager(context);
        }

        return instance;
    }

    private BoughtFeedbackManager(Context context) {
        this.context = context;
    }

    public void createBoughtFeedback(String postId, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().createBoughtFeedback(postId, onTaskCompleteListener);
    }

    public void toggleBoughtFeedback(String postId) {
        ApplicationHelper.getDatabaseHelper().toggleBoughtFeedback(postId);
    }

    public void getBoughtFeedbacksList(Context activityContext, OnDataChangedListener<BoughtFeedback> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getBoughtFeedbacksList(onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }
}
