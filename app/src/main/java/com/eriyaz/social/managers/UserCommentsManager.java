package com.eriyaz.social.managers;

import android.content.Context;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.UserComment;
import com.google.firebase.database.ValueEventListener;

public class UserCommentsManager extends FirebaseListenersManager {

    private static final String TAG = UserCommentsManager.class.getSimpleName();
    private static UserCommentsManager instance;

    private Context context;

    public static UserCommentsManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserCommentsManager(context);
        }
        return instance;
    }

    private UserCommentsManager(Context context) {
        this.context = context;
    }

    public void getUserCommentsList(OnObjectChangedListener<ItemListResult> onDataChangedListener, long date, String userId) {
        ApplicationHelper.getDatabaseHelper().getUserCommentsList(onDataChangedListener, date, userId);
    }
}
