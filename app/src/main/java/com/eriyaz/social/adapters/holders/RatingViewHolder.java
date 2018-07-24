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
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.PreferencesUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by alexey on 10.05.17.
 */

public class RatingViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatarImageView;
    private final ExpandableTextView ratingExpandedTextView;
    private final TextView ratingTextView;
//    private final TextView authorNameTextView;
    private final TextView dateTextView;
    private final TextView questionTextView;
    private final ProfileManager profileManager;
    private RatingsAdapter.Callback callback;
    protected ImageButton optionMenuButton;
    private Context context;

    public RatingViewHolder(View itemView, final RatingsAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        questionTextView = itemView.findViewById(R.id.questionTextView);
        ratingExpandedTextView = (ExpandableTextView) itemView.findViewById(R.id.ratingText);
        ratingTextView = itemView.findViewById(R.id.expandable_text);
//        authorNameTextView = itemView.findViewById(R.id.authorNameTextView);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        optionMenuButton = itemView.findViewById(R.id.optionMenuButton);
    }

    public void bindData(final Rating rating, final Post post) {
        final String authorId = rating.getAuthorId();

        questionTextView.setVisibility(View.GONE);
        ratingTextView.setVisibility(View.VISIBLE);
        String ratingText = String.valueOf(rating.getRating());
        if (rating.getDetailedText() != null && !rating.getDetailedText().isEmpty()) {
            ratingText = ratingText + "\n" + rating.getDetailedText();
        }

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, rating.getCreatedDate());
        dateTextView.setText(date);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(authorId, v);
            }
        });

        optionMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //creating a popup menu
                PopupMenu popup = new PopupMenu(context, optionMenuButton);
                //inflating menu from xml resource
                popup.inflate(R.menu.rating_context_menu);
                if (showReplyOption(post, rating)) {
                    popup.getMenu().findItem(R.id.messageMenuItem).setVisible(true);
                }
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.messageMenuItem:
                                callback.onReplyClick(getAdapterPosition());
                                break;
                            case R.id.reportMenuItem:
                                callback.onReportClick(view, getAdapterPosition());
                                break;
                        }
                        return false;
                    }
                });
                //displaying the popup
                popup.show();
            }
        });
        boolean hideRating = false;
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            if (currentUserId.equals(post.getAuthorId()) && !currentUserId.equals(rating.getAuthorId())
                    &&  !rating.isViewedByPostAuthor()) {
                questionTextView.setVisibility(View.VISIBLE);
                hideRating = true;
                if (!PreferencesUtil.isUserViewedRatingAtLeastOnce(context)) {
                    questionTextView.setText("Tap to view");
                }
                questionTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (!PreferencesUtil.isUserViewedRatingAtLeastOnce(context)) {
                            PreferencesUtil.setUserViewedRatingAtLeastOnce(context, true);
                        }
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            v.setEnabled(false);
                            v.postDelayed(new Runnable() {
                                public void run() {
                                    v.setEnabled(true);
                                }
                            }, 500);
                            callback.makeRatingVisible(position);
                        }
                    }
                });
            }
        }
        if (!hideRating) ratingExpandedTextView.setText(ratingText);
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(ratingExpandedTextView,
                    avatarImageView, ratingText, hideRating));
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView,
                                                                           final ImageView avatarImageView,
                                                                           final String ratingText,
                                                                           final boolean hideRating) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
                fillRating(obj.getUsername(), ratingText, expandableTextView, hideRating);
                if (obj.getPhotoUrl() != null) {
                    Glide.with(context)
                            .load(obj.getPhotoUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .crossFade()
                            .error(R.drawable.ic_stub)
                            .into(avatarImageView);
                } else {
                    avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(obj.getUsername(),
                            context.getResources().getDimensionPixelSize(R.dimen.rating_list_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.rating_list_avatar_height)));
                }
            }
        };
    }

    private void fillRating(String userName, String comment, ExpandableTextView commentTextView, boolean hideRating) {
        String text = userName + "   ";
        if (!hideRating) {
            text = text + comment;
        }
        Spannable contentString = new SpannableStringBuilder(text);
        int usernameLen = userName != null ? userName.length():0;
        contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                0, usernameLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        commentTextView.setText(contentString);
    }

    private boolean showReplyOption(Post post, Rating rating) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || post == null || !post.getAuthorId().equals(currentUser.getUid())) return false;
        if (currentUser.getUid().equals(rating.getAuthorId())) return false;
        return true;
    }
}
