/*
 *
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.eriyaz.social.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.managers.CommentManager;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.utils.LogUtil;

/**
 * Created by alexey on 12.05.17.
 */

public class CommentDialog extends DialogFragment {
    public static final String TAG = CommentDialog.class.getSimpleName();
    public static final int NEW_COMMENT_REQUEST = 33;
    private CommentManager commentManager;

    private String postId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        postId = (String) getArguments().get(PostDetailsActivity.POST_ID_EXTRA_KEY);
        super.onCreate(savedInstanceState);
        commentManager = CommentManager.getInstance(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_edit_comment, null);

        final EditText editCommentEditText = view.findViewById(R.id.editCommentEditText);
        editCommentEditText.setHint(R.string.comment_not_ok_hint);

        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        builder.setView(view)
                .setTitle(R.string.title_not_ok_comment)
                .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        notifyToTarget(Activity.RESULT_CANCELED);
                    }
                })
                .setPositiveButton(R.string.button_title_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newCommentText = editCommentEditText.getText().toString();
                        if (newCommentText.length() > 0) {
                            commentManager.createOrUpdateComment(newCommentText, postId, new OnTaskCompleteListener() {
                                @Override
                                public void onTaskComplete(boolean success) {

                                }
                            });
                            notifyToTarget(Activity.RESULT_OK);
                        } else {
                            notifyToTarget(Activity.RESULT_CANCELED);
                        }
                    }
                });

        return builder.create();
    }

    private void notifyToTarget(int code) {
        Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            targetFragment.onActivityResult(getTargetRequestCode(), code, null);
        } else {
//            PostDetailsActivity activity = (PostDetailsActivity) getActivity();
//            activity.onCommentDialogResult(getTargetRequestCode(), code, null);
        }
    }
}
