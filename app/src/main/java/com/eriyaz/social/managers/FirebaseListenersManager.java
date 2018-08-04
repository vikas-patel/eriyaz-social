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

import com.eriyaz.social.ApplicationHelper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alexey on 19.12.16.
 */

public class FirebaseListenersManager {
    Map<Context, List<ValueEventListener>> activeListeners = new HashMap<>();
    Map<Context, List<ChildEventListener>> activeChildListeners = new HashMap<>();

    void addListenerToMap(Context context, ValueEventListener valueEventListener) {
        if (activeListeners.containsKey(context)) {
            activeListeners.get(context).add(valueEventListener);
        } else {
            List<ValueEventListener> valueEventListeners = new ArrayList<>();
            valueEventListeners.add(valueEventListener);
            activeListeners.put(context, valueEventListeners);
        }
    }

    void addListenerToChildMap(Context context, ChildEventListener childEventListener) {
        if (activeChildListeners.containsKey(context)) {
            activeChildListeners.get(context).add(childEventListener);
        } else {
            List<ChildEventListener> childEventListeners = new ArrayList<>();
            childEventListeners.add(childEventListener);
            activeChildListeners.put(context, childEventListeners);
        }
    }

    public void closeListeners(Context context) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (activeListeners.containsKey(context)) {
            for (ValueEventListener listener : activeListeners.get(context)) {
                databaseHelper.closeListener(listener);
            }
            activeListeners.remove(context);
        }
    }

    public void closeChildListeners(Context context) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (activeChildListeners.containsKey(context)) {
            for (ChildEventListener listener : activeChildListeners.get(context)) {
                databaseHelper.closeChildListener(listener);
            }
            activeChildListeners.remove(context);
        }
    }

    public boolean hasActiveListeners(Context context) {
        if (activeListeners.containsKey(context)) {
            return true;
        }
        return false;
    }
}
