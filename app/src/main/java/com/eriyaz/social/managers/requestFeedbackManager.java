package com.eriyaz.social.managers;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.eriyaz.social.dialogs.songListDialog;
import com.eriyaz.social.dialogs.warningDialog;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class requestFeedbackManager {

    private DatabaseReference databaseReference;
    public static List<Post> items = new ArrayList<>();
    private ProgressDialog mProgressDialog;

    protected List<Post> postList = new LinkedList<>();


    public requestFeedbackManager(Context context, String currentUserId, String userID, String extraKeyValue, String currentUserName, Long currentUserPoints) {
        DialogFragment newFragment = warningDialog.newInstance(currentUserId, userID, extraKeyValue, currentUserName, currentUserPoints);
        newFragment.show(((Activity) context).getFragmentManager(), "dialog");
    }

    public requestFeedbackManager(Context context, String currentUserId, String userID, String currentUserName, Long currentUserPoints) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage("Loading..");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        databaseReference = FirebaseDatabase.getInstance().getReference("posts");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                items.clear();

                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    Post p = d.getValue(Post.class);

                    if (p.getAuthorId().equals(currentUserId))
                        items.add(p);
                }

                // check if list is empty, if yes it show toast message
                if (items.size() != 0) {
                    mProgressDialog.dismiss();

                    DialogFragment newFragment = songListDialog.newInstance(currentUserId, userID, currentUserName, currentUserPoints);
                    newFragment.show(((Activity) context).getFragmentManager(), "dialog");

                } else
                    Toast.makeText(context, "You don't have posted any song.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
