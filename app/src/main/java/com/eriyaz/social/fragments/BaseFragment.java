package com.eriyaz.social.fragments;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.utils.Analytics;

/**
 * Created by vikas on 16/7/18.
 */

public class BaseFragment extends Fragment {
    protected Analytics analytics;
    @Override
    public void onResume() {
        super.onResume();
        analytics = new Analytics(getContext());
        analytics.logActivity(this);
    }

    public void showWarningDialog(String message) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        builder.setMessage(message);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }
}