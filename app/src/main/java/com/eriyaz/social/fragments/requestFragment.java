package com.eriyaz.social.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.model.feedbackDetails;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import static com.eriyaz.social.activities.ProfileActivity.currentUserId;
import static com.eriyaz.social.activities.ProfileActivity.items;
import static com.eriyaz.social.activities.ProfileActivity.str;
import static com.eriyaz.social.activities.ProfileActivity.userID;


public class requestFragment extends DialogFragment {

    int mPoints = 0;
    String userName;
    //private ProfileManager profileManager;

    @SuppressLint("NewApi")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity profileActivity = this.getActivity();

        AlertDialog.Builder songListBuilder = new AlertDialog.Builder(profileActivity);
        AlertDialog.Builder warningMessageBuilder = new AlertDialog.Builder(profileActivity);

        ProgressDialog myProgressDialog = new ProgressDialog(profileActivity);

        //profileManager = ProfileManager.getInstance(toastActivity);

        songListBuilder.setTitle("Select Song")
                .setItems(str, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        warningMessageBuilder.setMessage("Your one point will be deducted if you continue. Do you still want to continue?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int u) {
                                        myProgressDialog.setMessage("Sending request..");
                                        myProgressDialog.setCanceledOnTouchOutside(true);
                                        myProgressDialog.setCancelable(false);
                                        myProgressDialog.show();

                                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                                        DatabaseReference databaseReference = database.getReference("user-notifications").child(userID);

                                        DatabaseReference databaseReference2 = database.getReference("profiles").child(currentUserId);
                                        String extraKeyValue = items.get(which).getImageTitle().substring(5);
                                        String childNode = databaseReference.push().getKey();

                                        databaseReference2.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                userName = dataSnapshot.getValue(String.class);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        databaseReference2.child("points").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                mPoints = dataSnapshot.getValue(Integer.class);

                                                if (mPoints > 0) {
                                                    long createdDate = Calendar.getInstance().getTimeInMillis();
                                                    String message =  userName + " has requested you to give feedback on his song.";
                                                    String fromUserId = currentUserId;
                                                    String action = "com.eriyaz.social.activities.PostDetailsActivity";
                                                    String extraKey = "PostDetailsActivity.POST_ID_EXTRA_KEY";

                                                    feedbackDetails f = new feedbackDetails(createdDate, message, fromUserId, action, extraKey, extraKeyValue, childNode);

                                                    databaseReference.child(childNode).setValue(f).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(profileActivity, "Request sent", Toast.LENGTH_SHORT).show();
                                                                databaseReference2.child("points").setValue(--mPoints);
                                                                //profileManager.decrementUserPoints(currentUserId);


                                                            } else
                                                                Toast.makeText(profileActivity, "Error while sending request", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                } else
                                                    Toast.makeText(profileActivity, "You don't have enough points for request.", Toast.LENGTH_SHORT).show();
                                                myProgressDialog.dismiss();

                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                myProgressDialog.dismiss();
                                                // Getting Post failed, log a message
                                            }
                                        });

                                    }
                                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int u) {

                            }
                        });
                        Dialog warningDialog = warningMessageBuilder.create();
                        warningDialog.setCanceledOnTouchOutside(false);
                        warningDialog.show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        Dialog dialog = songListBuilder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}