package com.eriyaz.social.fragments;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.eriyaz.social.utils.Analytics;

/**
 * Created by vikas on 16/7/18.
 */

public class BaseDialogFragment extends DialogFragment {
    protected Analytics analytics;
    @Override
    public void onResume() {
        super.onResume();
        analytics = new Analytics(getContext());
        analytics.logActivity(this);
    }
}