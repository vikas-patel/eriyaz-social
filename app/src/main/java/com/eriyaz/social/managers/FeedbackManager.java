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

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Message;
import com.google.firebase.database.ValueEventListener;

public class FeedbackManager extends FirebaseListenersManager {

    private static final String TAG = FeedbackManager.class.getSimpleName();
    private static FeedbackManager instance;

    private Context context;

    public static FeedbackManager getInstance(Context context) {
        if (instance == null) {
            instance = new FeedbackManager(context);
        }

        return instance;
    }

    private FeedbackManager(Context context) {
        this.context = context;
    }

    public void createFeedback(Message feedback, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().createFeedback(feedback, onTaskCompleteListener);
    }

    public void getFeedbackList(Context activityContext, OnDataChangedListener<Message> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getFeedbackList(onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void removeFeedback(String feedbackId, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.removeFeedback(feedbackId, onTaskCompleteListener);
    }
}
