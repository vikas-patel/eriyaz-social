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
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.adapters.AvtarAdapter;
import com.eriyaz.social.managers.CommentManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Avatar;
import com.eriyaz.social.model.Notification;

import java.util.List;

/**
 * Created by alexey on 12.05.17.
 */

public class AvatarDialog extends DialogFragment {
    public static final String TAG = AvatarDialog.class.getSimpleName();
    public static final String AVATAR_IMAGE_URL_EXTRA_KEY = "AvatarDialog.AVATAR_IMAGE_URL_EXTRA_KEY";
    private GridView gridView;
    private ProgressBar progressBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_avatar_grid, null);
        gridView = view.findViewById(R.id.gridView);
        progressBar = view.findViewById(R.id.progressBar);
        ApplicationHelper.getDatabaseHelper().getAvatarList(createOnAvatarsChangedDataListener());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle("Select Avatar..");
        return builder.create();
    }

    private void notifyToTarget(String imageUrl) {
        Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            Intent intent = getActivity().getIntent();
            intent.putExtra(AVATAR_IMAGE_URL_EXTRA_KEY, imageUrl);
            targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }
    }

    private OnDataChangedListener<Avatar> createOnAvatarsChangedDataListener() {

        return new OnDataChangedListener<Avatar>() {
            @Override
            public void onListChanged(List<Avatar> list) {
                AvtarAdapter gridAdapter = new AvtarAdapter(getActivity());
                gridAdapter.setList(list);
                gridAdapter.setCallback(new AvtarAdapter.Callback() {
                    @Override
                    public void onItemClick(final Avatar avatar, final View view) {
                        notifyToTarget(avatar.getImageUrl());
                        dismiss();
                    }
                });
                gridView.setAdapter(gridAdapter);
                progressBar.setVisibility(View.GONE);
            }
        };
    }
}
