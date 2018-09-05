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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.model.Flag;

/**
 * Created by alexey on 12.05.17.
 */

public class BlockDialog extends DialogFragment {
    public static final String TAG = BlockDialog.class.getSimpleName();
    public static final String BLOCKED_USER_KEY = "BlockDialog.BLOCKED_USER_KEY";

    private BlockCallback callback;
    private String blockedUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getActivity() instanceof BlockCallback) {
            callback = (BlockCallback) getActivity();
        } else {
            throw new RuntimeException(getActivity().getTitle() + " should implement BlockCallback");
        }
        blockedUser = getArguments().getString(BLOCKED_USER_KEY);

        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_block, null);

        final EditText editCommentEditText = (EditText) view.findViewById(R.id.complainReasonEditText);
        editCommentEditText.setHint(R.string.hint_block_dialog);

        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        builder.setView(view)
                .setTitle(R.string.title_block_dialog)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_title_save, null); //Set to null. We override the onclick
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String reason = editCommentEditText.getText().toString();
                        if (reason != null && !reason.isEmpty()) {
                            callback.onBlock(blockedUser, reason);
                            dialog.dismiss();
                        } else {
                            editCommentEditText.setError("must specify the reason");
                        }
                    }
                });
            }
        });

        return dialog;
    }

    public interface BlockCallback {
        void onBlock(String blockedUser, String reason);
    }
}
