package com.eriyaz.social.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.model.Notification;

public class warningDialog extends DialogFragment {

    public static String notificationMessage = " has requested you to give feedback on post ";
    public static String songname;

    public static warningDialog newInstance(String currentUserId, String userID, String extraKeyValue, String currentUserName, Long currentUserPoints , String songname1) {
        warningDialog frag = new warningDialog();
        Bundle args = new Bundle();
        args.putString("currentUserId", currentUserId);
        args.putString("currentUserName", currentUserName);
        args.putLong("currentUserPoints", currentUserPoints);
        args.putString("userID", userID);
        args.putString("extraKeyValue", extraKeyValue);
        frag.setArguments(args);
        songname=songname1;
        return frag;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String currentUserId = getArguments().getString("currentUserId");
        String currentUserName = getArguments().getString("currentUserName");
        Long currentUserPoints = getArguments().getLong("currentUserPoints");
        String userID = getArguments().getString("userID");
        String extraKeyValue = getArguments().getString("extraKeyValue");

        Activity activity = this.getActivity();

        AlertDialog.Builder warningMessageBuilder = new AlertDialog.Builder(activity);

        warningMessageBuilder.setTitle("Warning")
                .setMessage("Your one point will be deducted if you continue. Do you still want to continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int u) {
                        if (currentUserPoints > 0) {
                            ProfileManager profileManager = ProfileManager.getInstance(activity);

                            String message = currentUserName + notificationMessage + songname;
                            String action = "com.eriyaz.social.activities.PostDetailsActivity";
                            String extraKey = "PostDetailsActivity.POST_ID_EXTRA_KEY";

                            Notification notification = new Notification(currentUserId, message, action, extraKey, extraKeyValue, false, false, false, ItemType.ITEM);

                            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                                profileManager.sendNotification(notification, userID);
                                profileManager.decrementUserPoints(currentUserId);

                                showSnackbar("Feedback Request Sent");
                            } else
                                showSnackbar("Request Failed");
                        } else
                            //showSnackbar();
                            showDialog(getActivity(), "You don't have enough points to send the feedback request");
                    }
                }).setNegativeButton("No", null);

        Dialog warningDialog = warningMessageBuilder.create();
        warningDialog.setCanceledOnTouchOutside(false);
        warningDialog.show();
        return warningDialog;
    }

    private void showSnackbar(String message){
        Snackbar.make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showDialog(Context context, String message){
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Sorry");
        dialog.setCancelable(false);
        dialog.setMessage(message);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
