package com.eriyaz.social.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by vikas on 10/1/18.
 */

public class Analytics {

    private FirebaseAnalytics firebase;
    public static final String RATING = "rating";
    public static final String COMMENT = "comment";
    public static final String POST = "Post";
    public static final String OPEN_AUDIO = "OpenAudio";
    public static final String OPEN_RECORDED_AUDIO = "OpenRecordedAudio";
    public static final String RECORD = "Record";

    public Analytics(Context context) {
        firebase = FirebaseAnalytics.getInstance(context);
    }

    public void logActivity(Activity activity) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, activity.getClass().getSimpleName());
        firebase.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void logOpenAudio() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "OpenAudio");
        firebase.logEvent(OPEN_AUDIO, bundle);
    }

    public void logOpenRecordedAudio() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "OpenRecordedAudio");
        firebase.logEvent(OPEN_RECORDED_AUDIO, bundle);
    }

    public void logRecording() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Record");
        firebase.logEvent(RECORD, bundle);
    }

    public void logRating(String authorId, int rating) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Rating");
        bundle.putString("Author", authorId);
        bundle.putInt("Value", rating);
        firebase.logEvent(RATING, bundle);
    }

    public void logComment(String authorId) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Comment");
        bundle.putString("Author", authorId);
        firebase.logEvent(COMMENT, bundle);
    }

    public void logPost(String authorId) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Post");
        bundle.putString("Author", authorId);
        firebase.logEvent(POST, bundle);
    }
}
