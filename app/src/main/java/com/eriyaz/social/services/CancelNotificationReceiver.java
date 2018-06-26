package com.eriyaz.social.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.utils.LogUtil;

/**
 * Created by vikas on 25/6/18.
 */

public class CancelNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String postId = intent.getStringExtra(PostDetailsActivity.POST_ID_EXTRA_KEY);
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationData:"+postId, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}