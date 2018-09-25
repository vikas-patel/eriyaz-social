/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.eriyaz.social.adapters.holders;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.model.LikeUser;
import com.eriyaz.social.utils.ImageUtil;

/**
 * Created by Alexey on 03.05.18.
 */

public class UserViewHolder extends RecyclerView.ViewHolder {
    public static final String TAG = UserViewHolder.class.getSimpleName();

    private Context context;
    private ImageView photoImageView;
    private TextView nameTextView;
    private Button messageButton;

    private Activity activity;

    public UserViewHolder(View view, final Callback callback, Activity activity) {
        super(view);
        this.context = view.getContext();
        this.activity = activity;

        nameTextView = view.findViewById(R.id.nameTextView);
        photoImageView = view.findViewById(R.id.photoImageView);
        messageButton = view.findViewById(R.id.messageButton);

        view.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (callback != null && position != RecyclerView.NO_POSITION) {
                callback.onItemClick(getAdapterPosition(), v);
            }
        });

        messageButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onMessageButtonClick(getAdapterPosition());
            }
        });
    }

    public void bindData(LikeUser user) {
        fillInProfileFields(user);
    }

    protected void fillInProfileFields(LikeUser user) {
        nameTextView.setText(user.getUsername());

        if (user.getPhotoUrl() != null) {
            ImageUtil.loadImage(activity, user.getPhotoUrl(), photoImageView);
        }
    }

    public interface Callback {
        void onItemClick(int position, View view);

        void onMessageButtonClick(int position);
    }

}