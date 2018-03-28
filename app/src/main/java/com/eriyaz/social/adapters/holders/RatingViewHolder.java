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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.RatingsAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by alexey on 10.05.17.
 */

public class RatingViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatarImageView;
    private final ExpandableTextView ratingExpandedTextView;
    private final TextView ratingText;
    private final TextView dateTextView;
    private final ImageView questionImageView;
    private final ImageView replyImageView;
    private final ProfileManager profileManager;
    private RatingsAdapter.Callback callback;
    private Context context;

    public RatingViewHolder(View itemView, final RatingsAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        questionImageView = itemView.findViewById(R.id.questionImageView);
        ratingExpandedTextView = (ExpandableTextView) itemView.findViewById(R.id.ratingText);
        ratingText = itemView.findViewById(R.id.expandable_text);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        replyImageView = itemView.findViewById(R.id.replyImageView);

        if (callback != null) {
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        callback.onLongItemClick(v, position);
                        return true;
                    }

                    return false;
                }
            });

            replyImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        callback.onReplyClick(position);
                    }
                }
            });
        }
    }

    public void bindData(final Rating rating, final Post post) {
        final String authorId = rating.getAuthorId();
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(ratingExpandedTextView,
                    avatarImageView, String.valueOf(rating.getRating())));
        questionImageView.setVisibility(View.GONE);
        ratingText.setVisibility(View.VISIBLE);

        ratingExpandedTextView.setText(String.valueOf(rating.getRating()));

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, rating.getCreatedDate());
        dateTextView.setText(date);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(authorId, v);
            }
        });
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            if (currentUserId.equals(post.getAuthorId()) && !rating.isViewedByPostAuthor()) {
                questionImageView.setVisibility(View.VISIBLE);
                ratingText.setVisibility(View.GONE);
                questionImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            v.setEnabled(false);
                            v.postDelayed(new Runnable() {
                                public void run() {
                                    v.setEnabled(true);
                                }
                            }, 1000);
                            callback.makeRatingVisible(position);
                        }
                    }
                });
            }
        }
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView, final ImageView avatarImageView, final String rating) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
                String userName = obj.getUsername();
                fillRating(userName, rating, expandableTextView);

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

    private void fillRating(String userName, String rating, ExpandableTextView ratingTextView) {
        Spannable contentString = new SpannableStringBuilder(userName + "   " + rating);
        contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                0, userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        ratingTextView.setText(contentString);
    }
}
