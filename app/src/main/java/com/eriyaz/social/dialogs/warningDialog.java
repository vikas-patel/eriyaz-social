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
import android.widget.Toast;

import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.model.Notification;

public class warningDialog extends DialogFragment {

    public static String notificationMessage = " has requested you to give feedback on his song.";

    public static warningDialog newInstance(String currentUserId, String userID, String extraKeyValue, String currentUserName, Long currentUserPoints) {
        warningDialog frag = new warningDialog();
        Bundle args = new Bundle();
        args.putString("currentUserId", currentUserId);
        args.putString("currentUserName", currentUserName);
        args.putLong("currentUserPoints", currentUserPoints);
        args.putString("userID", userID);
        args.putString("extraKeyValue", extraKeyValue);
        frag.setArguments(args);
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

                            String message = currentUserName + notificationMessage;
                            String action = "com.eriyaz.social.activities.PostDetailsActivity";
                            String extraKey = "PostDetailsActivity.POST_ID_EXTRA_KEY";

                            Notification notification = new Notification(currentUserId, message, action, extraKey, extraKeyValue, false, false, false, ItemType.ITEM);

                            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                                profileManager.sendNotification(notification, userID);
                                profileManager.decrementUserPoints(currentUserId);

                                Toast.makeText(activity, "Request sent", Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(activity, "Request failed", Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(activity, "You don't have enough points to send the feedback request", Toast.LENGTH_SHORT).show();

                    }
                }).setNegativeButton("No", null);

        Dialog warningDialog = warningMessageBuilder.create();
        warningDialog.setCanceledOnTouchOutside(false);
        warningDialog.show();
        return warningDialog;
    }
}
