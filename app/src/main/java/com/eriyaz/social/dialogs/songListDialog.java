package com.eriyaz.social.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import static com.eriyaz.social.managers.requestFeedbackManager.items;

public class songListDialog extends DialogFragment {

    public static String str[];

    public static songListDialog newInstance(String currentUserId, String userID, String currentUserName, Long currentUserPoints) {
        songListDialog frag = new songListDialog();
        Bundle args = new Bundle();
        args.putString("currentUserId", currentUserId);
        args.putLong("currentUserPoints", currentUserPoints);
        args.putString("userID", userID);
        args.putString("currentUserName", currentUserName);
        frag.setArguments(args);
        return frag;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String currentUserId = getArguments().getString("currentUserId");
        Long currentUserPoints = getArguments().getLong("currentUserPoints");
        String userID = getArguments().getString("userID");
        String currentUserName = getArguments().getString("currentUserName");

        str = new String[items.size()];
        // ArrayList to Array Conversion
        for (int j = 0; j < items.size(); j++)
            str[j] = String.valueOf(items.get(j).getTitle());

        Activity profileActivity = this.getActivity();
        AlertDialog.Builder songListBuilder = new AlertDialog.Builder(profileActivity);

        songListBuilder.setTitle("Select Song")
                .setItems(str, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String extraKeyValue = items.get(which).getImageTitle().substring(5);
                        DialogFragment newFragment = warningDialog.newInstance(currentUserId, userID, extraKeyValue, currentUserName, currentUserPoints);
                        newFragment.show(((Activity) profileActivity).getFragmentManager(), "dialog");
                    }
                })
                .setNegativeButton("Cancel", null);

        Dialog dialog = songListBuilder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}