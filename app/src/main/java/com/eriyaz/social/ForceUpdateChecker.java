package com.eriyaz.social;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

/**
 * Created by vikas on 15/4/18.
 */

public class ForceUpdateChecker {

    private static final String TAG = ForceUpdateChecker.class.getSimpleName();

    private static final java.lang.String KEY_UPDATE_SKIP_REMINDER = "is_update_skip_reminder";
    public static final String KEY_UPDATE_COMPULSORY = "is_update_compulsory";
    public static final String KEY_UPDATE_BANNER_PERSISTENCE = "is_update_banner_persistent";
    public static final String KEY_CURRENT_VERSION = "current_version";


    private OnUpdateNeededListener onUpdateNeededListener;
    private Context context;

    public interface OnUpdateNeededListener {
        void onUpdateCompulsory();

        void onUpdateReminder(boolean isPersistent);
    }

    public static Builder with(@NonNull Context context) {
        return new Builder(context);
    }

    public ForceUpdateChecker(@NonNull Context context,
                              OnUpdateNeededListener onUpdateNeededListener) {
        this.context = context;
        this.onUpdateNeededListener = onUpdateNeededListener;
    }

    public void check() {
        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        int currentVersion = (int) remoteConfig.getLong(KEY_CURRENT_VERSION);
        int appVersion = getAppVersionCode(context);
        if (appVersion != 0 && currentVersion > appVersion
                && onUpdateNeededListener != null) {
            if (remoteConfig.getBoolean(KEY_UPDATE_SKIP_REMINDER)) {
                //do nothing
            } else if (remoteConfig.getBoolean(KEY_UPDATE_COMPULSORY)) {
                onUpdateNeededListener.onUpdateCompulsory();
            } else {
                onUpdateNeededListener.onUpdateReminder(remoteConfig.getBoolean(KEY_UPDATE_BANNER_PERSISTENCE));
            }
        }
    }

    private int getAppVersionCode(Context context) {
        int result = 0;

        try {
            result = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionCode;
//            result = result.replaceAll("[a-zA-Z]|-", "");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }

        return result;
    }

    public static class Builder {

        private Context context;
        private OnUpdateNeededListener onUpdateNeededListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder onUpdateNeeded(OnUpdateNeededListener onUpdateNeededListener) {
            this.onUpdateNeededListener = onUpdateNeededListener;
            return this;
        }

        public ForceUpdateChecker build() {
            return new ForceUpdateChecker(context, onUpdateNeededListener);
        }

        public ForceUpdateChecker check() {
            ForceUpdateChecker forceUpdateChecker = build();
            forceUpdateChecker.check();

            return forceUpdateChecker;
        }
    }
}