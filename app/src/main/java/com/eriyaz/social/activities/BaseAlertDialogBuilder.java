package com.eriyaz.social.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.eriyaz.social.utils.Analytics;

/**
 * Created by vikas on 17/7/18.
 */

public class BaseAlertDialogBuilder extends AlertDialog.Builder {
    protected Analytics analytics;

    public BaseAlertDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @Override
    public AlertDialog.Builder setMessage(@Nullable CharSequence message) {
        super.setMessage(message);
        analytics = new Analytics(getContext());
        analytics.logAlertDialog(getContext(), message);
        return this;
    }
}
