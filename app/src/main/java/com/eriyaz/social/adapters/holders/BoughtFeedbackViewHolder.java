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

package com.eriyaz.social.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.BoughtFeedbackAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;

/**
 * Created by alexey on 10.05.17.
 */

public class BoughtFeedbackViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatarImageView;
    private final TextView dateTextView;
    private final TextView paymentStatusTextView;
    private Button resolveButton;
    private final ProfileManager profileManager;
    private BoughtFeedbackAdapter.Callback callback;
    private Context context;

    public BoughtFeedbackViewHolder(View itemView, final BoughtFeedbackAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        paymentStatusTextView = (TextView) itemView.findViewById(R.id.paymentStatusTextView);
        resolveButton = itemView.findViewById(R.id.toggle_resolved);
    }

    public void bindData(BoughtFeedback boughtFeedback) {
        final String authorId = boughtFeedback.getAuthorId();
        final String postId = boughtFeedback.getPostId();
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(avatarImageView));

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, boughtFeedback.getCreatedDate());
        dateTextView.setText(date);
        paymentStatusTextView.setText(boughtFeedback.getPaymentStatus());

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(authorId);
            }
        });

        if (boughtFeedback.isResolved()) {
            resolveButton.setText("RESOLVED");
        } else {
            resolveButton.setText("UNRESOLVED");
        }

        resolveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.toggleResolveClick(postId);
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onPostClick(postId);
            }
        });
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ImageView avatarImageView) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (obj.getPhotoUrl() != null) {
                    Glide.with(context)
                            .load(obj.getPhotoUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .crossFade()
                            .error(R.drawable.ic_stub)
                            .into(avatarImageView);
                }
            }
        };
    }
}
