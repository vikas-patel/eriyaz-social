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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.adapters.CommentsAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.TimestampTagUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

/**
 * Created by alexey on 10.05.17.
 */

public class
CommentViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatarImageView;
    private final ExpandableTextView commentTextView;
    private final TextView expandableTextView;
    private final TextView dateTextView;
    protected ImageButton optionMenuButton;
    protected Spinner rewardSpinner;
    private TextView rewardTextView;
    private ImageView playImageView;
    private String mUserName = "";
    private final ProfileManager profileManager;
    private CommentsAdapter.Callback callback;
    private Context context;
    private boolean isAdmin;

    private HashTagHelper mistakesTextHashTagHelper;


    public CommentViewHolder(View itemView, final CommentsAdapter.Callback callback, boolean aIsAdmin) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        commentTextView = (ExpandableTextView) itemView.findViewById(R.id.commentText);
        expandableTextView = itemView.findViewById(R.id.expandable_text);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        optionMenuButton = itemView.findViewById(R.id.optionMenuButton);
        playImageView = itemView.findViewById(R.id.playimageView);
        rewardSpinner = itemView.findViewById(R.id.rewardSpinner);
        rewardTextView = itemView.findViewById(R.id.rewardText);
        isAdmin = aIsAdmin;

        mistakesTextHashTagHelper = HashTagHelper.Creator.create(itemView.getResources().getColor(R.color.red), new HashTagHelper.OnHashTagClickListener() {
            @Override
            public void onHashTagClicked(String hashTag) {
                if(TimestampTagUtil.isValidTimestamp(hashTag))
                    callback.onTimeStampClick(commentTextView.getText().toString(), hashTag);
            }
        }, new char[] {':'});


        // pass a TextView or any descendant of it (incliding EditText) here.
        // Hash tags that are in the text will be hightlighed with a color passed to HasTagHelper
        mistakesTextHashTagHelper.handle(commentTextView.getTextView());
    }

    public void bindData(final Comment comment, final Post post) {
        final String authorId = comment.getAuthorId();
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(commentTextView,
                    avatarImageView, comment.getText()));

        if (comment.getText() == null || comment.getText().isEmpty()) {
            expandableTextView.setVisibility(View.GONE);
        } else {
            expandableTextView.setVisibility(View.VISIBLE);
            commentTextView.setText(comment.getText());
        }

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, comment.getCreatedDate());
        dateTextView.setText(date);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(authorId, v);
            }
        });

        if (comment.getAudioPath() != null && !comment.getAudioPath().isEmpty()) {
            playImageView.setVisibility(View.VISIBLE);
            playImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onPlayClick(v, getAdapterPosition(), mUserName);
                }
            });
        } else {
            playImageView.setVisibility(View.GONE);
        }

        optionMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //creating a popup menu
                PopupMenu popup = new PopupMenu(context, optionMenuButton);
                //inflating menu from xml resource
                popup.inflate(R.menu.comment_context_menu);
                if (hasAccessToEditComment(comment.getAuthorId())) {
                    popup.getMenu().findItem(R.id.editMenuItem).setVisible(true);
                    popup.getMenu().findItem(R.id.deleteMenuItem).setVisible(true);
                } else if (hasAccessToModifyPost(post)) {
                    popup.getMenu().findItem(R.id.deleteMenuItem).setVisible(true);
                    popup.getMenu().findItem(R.id.blockMenuItem).setVisible(true);
                }
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.deleteMenuItem:
                                callback.onDeleteClick(view, getAdapterPosition());
                                break;
                            case R.id.editMenuItem:
                                callback.onEditClick(view, getAdapterPosition());
                                break;
                            case R.id.blockMenuItem:
                                callback.onBlockClick(view, getAdapterPosition());
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

        if (isAdmin) {
            rewardSpinner.setVisibility(View.VISIBLE);
            rewardSpinner.setSelection(comment.getReputationPoints());
            final int initial = rewardSpinner.getSelectedItemPosition();
            rewardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (initial == i) return;
                    callback.onRewardClick(view, getAdapterPosition(), i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        if (comment.getReputationPoints() > 0) {
            rewardTextView.setVisibility(View.VISIBLE);
            rewardTextView.setText(Html.fromHtml(String.format(context.getString(R.string.comment_reward_points), comment.getReputationPoints())));
            rewardTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog(R.string.reputation_ranking_coming_soon);
                }
            });
        } else {
            rewardTextView.setVisibility(View.GONE);
        }
    }

    private void showDialog(int messageId) {
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(context);
        builder.setMessage(Html.fromHtml(context.getString(messageId)));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView, final ImageView avatarImageView, final String comment) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
                mUserName = obj.getUsername();
                fillComment(mUserName, comment, expandableTextView);

                if (obj.getPhotoUrl() != null) {
                    Glide.with(context)
                            .load(obj.getPhotoUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .crossFade()
                            .error(R.drawable.ic_stub)
                            .into(avatarImageView);
                } else {
                    avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(mUserName,
                            context.getResources().getDimensionPixelSize(R.dimen.comment_list_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.comment_list_avatar_height)));
                }
            }
        };
    }

    private void fillComment(String userName, String comment, ExpandableTextView commentTextView) {
        Spannable contentString = new SpannableStringBuilder(userName + "   " + comment);
        int usernameLen = userName != null ? userName.length():0;
        contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                0, usernameLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        commentTextView.setText(contentString);
    }

    private boolean hasAccessToEditComment(String commentAuthorId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && commentAuthorId.equals(currentUser.getUid());
    }

    private boolean hasAccessToModifyPost(Post post) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && post != null && post.getAuthorId().equals(currentUser.getUid());
    }
}
