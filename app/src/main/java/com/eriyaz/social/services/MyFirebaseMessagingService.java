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

package com.eriyaz.social.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.enums.PostOrigin;
import com.eriyaz.social.utils.Analytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.MainActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.utils.LogUtil;

/**
 * Created by alexey on 13.04.17.
 */


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    private static int notificationId = 0;
    private static final String POST_ID_KEY = "postId";
    private static final String BODY_KEY = "body";
    private static final String AUTHOR_ID_KEY = "authorId";
    private static final String AUTHOR_NAME_KEY = "authorName";
    private static final String ACTION_TYPE_KEY = "actionType";
    private static final String TITLE_KEY = "title";
    private static final String POST_TITLE_KEY = "postTitle";
    private static final String ICON_KEY = "icon";
    private static final String ACTION_TYPE_NEW_RATING = "new_rating";
    private static final String ACTION_TYPE_NEW_COMMENT = "new_comment";
    private static final String ACTION_TYPE_NEW_POST = "new_post";
    private static final String ACTION_TYPE_OFFICIAL_FEEDBACK = "official_feedback";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData() != null && remoteMessage.getData().get(ACTION_TYPE_KEY) != null) {
            handleRemoteMessage(remoteMessage);
        } else {
            LogUtil.logError(TAG, "onMessageReceived()", new RuntimeException("FCM remoteMessage doesn't contains Action Type"));
        }
    }

    private void handleRemoteMessage(RemoteMessage remoteMessage) {
        String receivedActionType = remoteMessage.getData().get(ACTION_TYPE_KEY);
        Analytics analytics;
        switch (receivedActionType) {
            case ACTION_TYPE_NEW_RATING:
                parseCommentOrLike(Channel.NEW_LIKE, remoteMessage);
                analytics = new Analytics(getApplicationContext());
                analytics.receivedNotification("rating");
                break;
            case ACTION_TYPE_NEW_COMMENT:
                parseCommentOrLike(Channel.NEW_COMMENT, remoteMessage);
                analytics = new Analytics(getApplicationContext());
                analytics.receivedNotification("comment");
                break;
            case ACTION_TYPE_NEW_POST:
                handleNewPostCreatedAction(remoteMessage);
                break;
            case ACTION_TYPE_OFFICIAL_FEEDBACK:
                parseOfficialFeedback(remoteMessage);
                break;
        }
    }

    private void handleNewPostCreatedAction(RemoteMessage remoteMessage) {
        String postAuthorId = remoteMessage.getData().get(AUTHOR_ID_KEY);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        //Send notification for each users except author of post.
        if (firebaseUser != null && !firebaseUser.getUid().equals(postAuthorId)) {
            PostManager.getInstance(this.getApplicationContext()).incrementNewPostsCounter();
        }
    }

    private void parseOfficialFeedback(RemoteMessage remoteMessage) {
        String notificationTitle = remoteMessage.getData().get(TITLE_KEY);
        String notificationBody = remoteMessage.getData().get(BODY_KEY);
        String notificationImageUrl = remoteMessage.getData().get(ICON_KEY);
        String postId = remoteMessage.getData().get(POST_ID_KEY);

        Intent backIntent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, postId);
        intent.putExtra(PostDetailsActivity.POST_ORIGIN_EXTRA_KEY, PostOrigin.PUSH_NOTIFICATION);

        Bitmap bitmap = getBitmapFromUrl(notificationImageUrl);

        sendIndividualNotification(notificationTitle, notificationBody, bitmap, intent, backIntent);

        LogUtil.logDebug(TAG, "Message Notification Body: " + remoteMessage.getData().get(BODY_KEY));
    }

    private void parseCommentOrLike(Channel channel, RemoteMessage remoteMessage) {
        String notificationTitle = remoteMessage.getData().get(TITLE_KEY);
        String notificationImageUrl = remoteMessage.getData().get(ICON_KEY);
        String postId = remoteMessage.getData().get(POST_ID_KEY);
        String postTitle = remoteMessage.getData().get(POST_TITLE_KEY);
        String authorName = remoteMessage.getData().get(AUTHOR_NAME_KEY);

        Intent backIntent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, postId);
        intent.putExtra(PostDetailsActivity.POST_ORIGIN_EXTRA_KEY, PostOrigin.PUSH_NOTIFICATION);
        String receivedActionType = remoteMessage.getData().get(ACTION_TYPE_KEY);

        Bitmap bitmap = getBitmapFromUrl(notificationImageUrl);

        sendNotification(channel, notificationTitle, bitmap, intent, backIntent, postId, receivedActionType, authorName, postTitle);
    }

    public Bitmap getBitmapFromUrl(String imageUrl) {
        try {
            return Glide.with(this)
                    .load(imageUrl)
                    .asBitmap()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(Constants.PushNotification.LARGE_ICONE_SIZE, Constants.PushNotification.LARGE_ICONE_SIZE)
                    .get();

        } catch (Exception e) {
            LogUtil.logError(TAG, "getBitmapfromUrl", e);
            return null;
        }
    }

    private void sendIndividualNotification(String notificationTitle, String notificationBody, Bitmap bitmap, Intent intent, Intent backIntent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent;

        if(backIntent != null) {
            backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent[] intents = new Intent[] {backIntent, intent};
            pendingIntent = PendingIntent.getActivities(this, notificationId++, intents, PendingIntent.FLAG_ONE_SHOT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, notificationId++, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.ic_push_notification_small) //Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setLargeIcon(bitmap)
                .setSound(defaultSoundUri);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId++ /* ID of notification */, notificationBuilder.build());

    }

    private void sendNotification(Channel channel, String notificationTitle, Bitmap bitmap,
                                  Intent intent, Intent backIntent, String postId, String actionType, String authorName, String postTitle) {
        int postIdInt = postId.hashCode();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent onCancelNotificationReceiver = new Intent(this, CancelNotificationReceiver.class);
        onCancelNotificationReceiver.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, postId);
        PendingIntent onCancelNotificationReceiverPendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), postIdInt,
                onCancelNotificationReceiver, 0);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent;

        if(backIntent != null) {
            backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent[] intents = new Intent[] {backIntent, intent};
            pendingIntent = PendingIntent.getActivities(this, postIdInt, intents, PendingIntent.FLAG_ONE_SHOT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, postIdInt, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("NotificationData:"+postId, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (actionType.equalsIgnoreCase(ACTION_TYPE_NEW_RATING)) {
            editor.putString("lastLikedBy", authorName);
            editor.putInt("likeCount", sharedPreferences.getInt("likeCount", 0) + 1);
        } else {
            editor.putString("lastCommentedBy", authorName);
            editor.putInt("commentCount", sharedPreferences.getInt("commentCount", 0) + 1);
        }
        editor.apply();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channel.id)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.ic_push_notification_small) //Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle(notificationTitle)
                .setStyle(getStyleForNotification(postId, authorName, postTitle))
                .setLargeIcon(bitmap)
                .setSound(defaultSoundUri)
                .setDeleteIntent(onCancelNotificationReceiverPendingIntent);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(channel.id, getString(channel.name), importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(ContextCompat.getColor(this, R.color.primary));
            notificationChannel.enableVibration(true);
            notificationBuilder.setChannelId(channel.id);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        manager.notify(postIdInt /* ID of notification */, notificationBuilder.build());
    }

    private NotificationCompat.InboxStyle getStyleForNotification(String postId, String newAuthor, String postTitle) {
        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        SharedPreferences sharedPref = getSharedPreferences("NotificationData:"+postId, 0);
        int likeCount = sharedPref.getInt("likeCount", 0);
        if (likeCount > 0) {
            String authorName = sharedPref.getString("lastLikedBy", "");
            String message;
            if (likeCount == 1) {
                message = authorName + " rated.";
            } else {
                message = String.format(getResources().getQuantityString(R.plurals.push_notification_rated_body, likeCount-1), authorName, likeCount-1);
            }
            inbox.addLine(message);
            inbox.setBigContentTitle("Your post '" + postTitle + "'");
        }
        int commentCount = sharedPref.getInt("commentCount", 0);
        if (commentCount > 0) {
            String authorName = sharedPref.getString("lastCommentedBy", "");
            String message;
            if (commentCount == 1) {
                message = authorName + " commented.";
            } else {
                message = String.format(getResources().getQuantityString(R.plurals.push_notification_commented_body, commentCount-1), authorName, commentCount-1);
            }
            inbox.addLine(message);
            inbox.setBigContentTitle("Your post '" + postTitle + "'");
        }
        return inbox;
    }

    enum Channel {
        NEW_LIKE("new_like_id", R.string.new_like_channel_name),
        NEW_COMMENT("new_comment_id", R.string.new_comment_channel_name);

        String id;
        @StringRes
        int name;

        Channel(String id, @StringRes int name) {
            this.id = id;
            this.name = name;
        }
    }
}
