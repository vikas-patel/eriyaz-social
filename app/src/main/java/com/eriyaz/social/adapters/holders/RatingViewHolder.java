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
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.RatingsAdapter;
import com.eriyaz.social.dialogs.EditCommentDialog;
import com.eriyaz.social.dialogs.RatingPercentileDialog;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.PreferencesUtil;
import com.eriyaz.social.utils.RatingUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

/**
 * Created by alexey on 10.05.17.
 */

public class RatingViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatarImageView;
    private final ExpandableTextView ratingExpandedTextView;
    private final TextView ratingTextView;
    //    private final TextView authorNameTextView;
    private final TextView dateTextView;
    private final ImageView questionTextView;
    private TextView tapToTextView;
    private final ProfileManager profileManager;
    private RatingsAdapter.Callback callback;
    protected ImageButton optionMenuButton;
    private String mUserName = "";
    private Post mPost;
    private Context context;

    public RatingViewHolder(View itemView, final RatingsAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        questionTextView = itemView.findViewById(R.id.questionTextView);
        tapToTextView = itemView.findViewById(R.id.tapToTextView);
        ratingExpandedTextView = (ExpandableTextView) itemView.findViewById(R.id.ratingText);
        ratingTextView = itemView.findViewById(R.id.expandable_text);
//        authorNameTextView = itemView.findViewById(R.id.authorNameTextView);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        optionMenuButton = itemView.findViewById(R.id.optionMenuButton);
    }

    public void bindData(final Rating rating, final Post post) {
        final String authorId = rating.getAuthorId();
        mPost = post;

        questionTextView.setVisibility(View.GONE);
        ratingTextView.setVisibility(View.VISIBLE);

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
                if (hasAccessToModifyPost(post)) {
                    popup.getMenu().findItem(R.id.blockMenuItem).setVisible(true);
                    popup.getMenu().findItem(R.id.requestFeedbackMenuItem).setVisible(true);
                }
                if(showRatingRemoveOption(post,rating) && !rating.isRatingRemoved()){
                    popup.getMenu().findItem(R.id.removeRating).setVisible(true);
                }
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.requestFeedbackMenuItem:
                                callback.onRequestFeedbackClick(getAdapterPosition());
                                break;
                            case R.id.reportMenuItem:
                                callback.onReportClick(view, getAdapterPosition());
                                break;
                            case R.id.blockMenuItem:
                                callback.onBlockClick(view, getAdapterPosition());
                                break;
                            case R.id.removeRating:
                                callback.onRemoveRatingClick(view, getAdapterPosition());
                                break;
                        }
                        return false;
                    }
                });
                //displaying the popup
                popup.show();
            }
        });
        if (hasAccessToModifyPost(post)) {
            if (!rating.isViewedByPostAuthor()) {
                questionTextView.setVisibility(View.VISIBLE);
                if (!PreferencesUtil.isUserViewedRatingAtLeastOnce(context)) {
                    tapToTextView.setVisibility(View.VISIBLE);
                    tapToTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            showRating(v);
                        }
                    });

                }
                questionTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        showRating(v);
                    }
                });
            } else {
                int normalizedRating = rating.getNormalizedRating();
                if (normalizedRating == 0) normalizedRating = (int) rating.getRating();
                String ratingText = RatingUtil.getRatingPercentile(normalizedRating);
                if (rating.getDetailedText() != null && !rating.getDetailedText().isEmpty()) {
                    ratingText = ratingText + "\n" + rating.getDetailedText();
                }

                ratingExpandedTextView.setText(ratingText);
            }
        } else {
            String ratingText = String.valueOf(rating.getRating());
            if (rating.getDetailedText() != null && !rating.getDetailedText().isEmpty()) {
                ratingText = ratingText + "\n" + rating.getDetailedText();
            }

            ratingExpandedTextView.setText(ratingText);
        }
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId,
                    createOnProfileChangeListener(ratingExpandedTextView, avatarImageView, rating));
    }


    private void showRating(final View v) {
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

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView,
                                                                           final ImageView avatarImageView,
                                                                           final Rating rating) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity) context).isActivityDestroyed()) return;
                mUserName = obj.getUsername();
                fillRating(rating, expandableTextView);
                if (obj.getPhotoUrl() != null) {
                    ImageUtil.loadImage(GlideApp.with(context), obj.getPhotoUrl(), avatarImageView);
                } else {
                    avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(obj.getUsername(),
                            context.getResources().getDimensionPixelSize(R.dimen.rating_list_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.rating_list_avatar_height)));
                }
            }
        };
    }

    private void fillRating(final Rating rating, ExpandableTextView commentTextView) {
        int usernameLen = mUserName != null ? mUserName.length() : 0;
        String text = mUserName + "   ";
        String extra = "";
        if (!hasAccessToModifyPost(mPost)) {
            String ratingText = String.valueOf(rating.getRating());
            if (rating.getDetailedText() != null && !rating.getDetailedText().isEmpty()) {
                ratingText = ratingText + "\n" + rating.getDetailedText();
            }

            if(rating.isRatingRemoved())
                extra = context.getString(R.string.rating_removed_text);
            else
                extra = ratingText;

            Spannable contentString = new SpannableStringBuilder(text + extra);
            contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                    0, usernameLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            commentTextView.setText(contentString);

            return;
        }
        if (!rating.isViewedByPostAuthor()) {
            Spannable contentString = new SpannableStringBuilder(mUserName);
            contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                    0, usernameLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            commentTextView.setText(contentString);
            return;
        }
        // post author view
        final int actualRating = (int) rating.getRating();
        final int normalizedRating = rating.getNormalizedRating() == 0 ? actualRating : rating.getNormalizedRating();
        String ratingText = RatingUtil.getRatingPercentile(normalizedRating);
        String removedRatings = context.getString(R.string.rating_removed_text);;
        int ratingLen = ratingText.length();
        if (rating.getDetailedText() != null && !rating.getDetailedText().isEmpty()) {
            ratingText = ratingText + "\n" + rating.getDetailedText();
        }

        if(rating.isRatingRemoved()){
            text = text + removedRatings;
        }
        else {
            text = text + ratingText;
        }

        SpannableString contentString = new SpannableString(text);
        contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                0, usernameLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if(!rating.isRatingRemoved()) {
            URLSpan urlSpan = new URLSpan("") {
                @Override
                public void onClick(View widget) {
                    RatingPercentileDialog ratingPercentileDialogDialog = new RatingPercentileDialog();
                    Bundle args = new Bundle();
                    args.putInt(RatingPercentileDialog.NORMALIZED_RATING_KEY, normalizedRating);
                    args.putInt(RatingPercentileDialog.ACTUAL_RATING_KEY, actualRating);
                    args.putString(RatingPercentileDialog.RATER_ID_KEY, rating.getAuthorId());
                    args.putString(RatingPercentileDialog.RATER_NAME_KEY, mUserName);
                    ratingPercentileDialogDialog.setArguments(args);
                    ratingPercentileDialogDialog.show(((BaseActivity) context).getFragmentManager(), RatingPercentileDialog.TAG);
                }
            };
            contentString.setSpan(urlSpan,
                    usernameLen + 3, usernameLen + 3 + ratingLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if(rating.isRatingRemoved())
            commentTextView.setText(contentString);
        else
            ratingTextView.setText(contentString);

        Pattern pattern = Pattern.compile("(Top|Bottom).*%");
        Linkify.addLinks(ratingTextView, pattern, "");
    }

    private boolean hasAccessToModifyPost(Post post) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && post != null && post.getAuthorId().equals(currentUser.getUid());
    }

    private boolean showRatingRemoveOption(Post post, Rating rating) {

        String currentUserId = FirebaseAuth.getInstance().getUid();
        return post!=null && rating!=null && post.getAuthorId().equals(currentUserId);

    }

}
