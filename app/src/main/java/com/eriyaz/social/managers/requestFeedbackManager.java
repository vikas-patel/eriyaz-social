package com.eriyaz.social.managers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eriyaz.social.R;
import com.eriyaz.social.dialogs.songListDialog;
import com.eriyaz.social.dialogs.warningDialog;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.Post;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class requestFeedbackManager implements OnDataChangedListener {

    private DatabaseReference databaseReference;
    public static List<Post> items = new ArrayList<>();
    private ProgressDialog mProgressDialog;
    DatabaseHelper databaseHelper;
    Context context1;
    String userID1;
    String currentUserName1;
    Long currentUserPoints1;
    String currentUserId1;


    protected List<Post> postList = new LinkedList<>();


    public requestFeedbackManager(Context context, String currentUserId, String userID, String extraKeyValue, String currentUserName, Long currentUserPoints) {
        context1=context;
        userID1=userID;
        currentUserName1=currentUserName;
        currentUserPoints1=currentUserPoints;
        currentUserId1=currentUserId;
        DialogFragment newFragment = warningDialog.newInstance(currentUserId, userID, extraKeyValue, currentUserName, currentUserPoints,"","");
        newFragment.show(((Activity) context).getFragmentManager(), "dialog");
    }

    public requestFeedbackManager(Context context, String currentUserId, String userID, String currentUserName, Long currentUserPoints) {
        context1=context;
        userID1=userID;
        currentUserName1=currentUserName;
        currentUserPoints1=currentUserPoints;
        currentUserId1=currentUserId;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage("Loading..");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        databaseHelper=new DatabaseHelper(context1);
        Log.e("check", "requestFeedbackManager: "+currentUserId);
        databaseHelper.getPostListByUser(this,currentUserId1);


        /*databaseReference = FirebaseDatabase.getInstance().getReference("posts");
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

                mProgressDialog.dismiss();

                if (items.size() != 0) {
                    DialogFragment newFragment = songListDialog.newInstance(currentUserId, userID, currentUserName, currentUserPoints);
                    newFragment.show(((Activity) context).getFragmentManager(), "dialog");

                } else
                    showDialog(context, R.string.error_request_no_post);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/

    }

    private void showDialog(Context context, int messageId){

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Sorry");
        dialog.setCancelable(false);
        dialog.setMessage(messageId);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onListChanged(List list) {
        items = list;
        mProgressDialog.dismiss();
        if (list.size() != 0) {
            DialogFragment newFragment = songListDialog.newInstance(currentUserId1, userID1, currentUserName1, currentUserPoints1);
            newFragment.show(((Activity) context1).getFragmentManager(), "dialog");

        } else
            showDialog(context1, R.string.error_request_no_post);

    }

}
