package com.eriyaz.social.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.eriyaz.social.R;

import java.util.ArrayList;

import static com.eriyaz.social.managers.requestFeedbackManager.items;

public class songListDialog extends DialogFragment {

    public static ArrayList<String> songsList;

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

        songsList = new ArrayList<>();
        songsList.add("Select");
        // ArrayList to Array Conversion
        for (int j = 0; j < items.size(); j++)
            songsList.add(String.valueOf(items.get(j).getTitle()));

        Activity profileActivity = this.getActivity();

        //build the dialog view
        View view = LayoutInflater.from(profileActivity).inflate(R.layout.songs_list_dialog, null, false);
        Spinner spinner = view.findViewById(R.id.songs_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(profileActivity, android.R.layout.simple_list_item_1, songsList);
        spinner.setAdapter(adapter);

        AlertDialog.Builder songListBuilder = new AlertDialog.Builder(profileActivity);

        songListBuilder.setTitle("Select Song")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Request Feedback", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = spinner.getSelectedItemPosition();
                        if(position>0) {
                            String extraKeyValue = items.get(position - 1).getImageTitle().substring(5);
                            DialogFragment newFragment = warningDialog.newInstance(currentUserId, userID, extraKeyValue, currentUserName, currentUserPoints);
                            newFragment.show(((Activity) profileActivity).getFragmentManager(), "dialog");
                        }
                        else{
                            Toast.makeText(profileActivity, "Please select a song", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        Dialog dialog = songListBuilder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}