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
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.BoughtFeedbackAdapter;
import com.eriyaz.social.adapters.ProfileAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by alexey on 10.05.17.
 */

public class LeaderBoardViewHolder extends RecyclerView.ViewHolder {

    private final TextView rankTextView;
    private final ImageView avatarImageView;
    private TextView authorNameTextView;
    private TextView pointsTextView, weeklyRankTextView;
    private ProfileAdapter.Callback callback;
    private Context context;

    public LeaderBoardViewHolder(View itemView, final ProfileAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        avatarImageView = itemView.findViewById(R.id.avatarImageView);
        rankTextView = itemView.findViewById(R.id.rankTextView);
        authorNameTextView = itemView.findViewById(R.id.authorNameTextView);
        pointsTextView = itemView.findViewById(R.id.pointsTextView);
        weeklyRankTextView = itemView.findViewById(R.id.weeklyPointsTextView);
    }

    public void bindData(final Profile profile) {
        rankTextView.setText(Integer.toString(profile.getRank()));
        pointsTextView.setText(Long.toString(profile.getReputationPoints()));
        weeklyRankTextView.setText(Long.toString(profile.getWeeklyRank()));
        authorNameTextView.setText(profile.getUsername());
        setProfileImage(profile);
        if (isSelfProfile(profile.getId())) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.highlight_bg));
        } else {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.default_bg));
        }
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onItemClick(profile, v);
            }
        });
    }

    private void setProfileImage(Profile profile) {
        if (profile.getPhotoUrl() != null) {
            ImageUtil.loadImage(GlideApp.with(context), profile.getPhotoUrl(), avatarImageView);
        } else {
            avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(profile.getUsername(),
                    context.getResources().getDimensionPixelSize(R.dimen.bought_feedback_list_avatar_height),
                    context.getResources().getDimensionPixelSize(R.dimen.bought_feedback_list_avatar_height)));
        }
    }

    private boolean isSelfProfile(String profileId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        if (profileId != null && profileId.equals(currentUser.getUid())) return true;
        return false;
    }
}
