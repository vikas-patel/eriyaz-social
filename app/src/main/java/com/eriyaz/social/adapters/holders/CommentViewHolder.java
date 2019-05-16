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
import android.content.Intent;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.LeaderboardActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.adapters.CommentsAdapter;
import com.eriyaz.social.controllers.LikeController;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.TimestampTagUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.util.Calendar;

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
    private Spinner userRewardSpinner;
    private TextView adminTitleTextView;
    private TextView userTitleTextView;
    private TextView userRewardTextView;
    private TextView rewardTextView;
    private ImageView playImageView;
    private TextView likeCounterTextView;
    private ImageView likesImageView;
    private ViewGroup likeViewGroup;
    private LikeController likeController;
    private String mUserName = "";
    private final ProfileManager profileManager;
    private CommentsAdapter.Callback callback;
    private Context context;
    private boolean isAdmin;
    String postUserName;

    private HashTagHelper mistakesTextHashTagHelper;


    public CommentViewHolder(View itemView, final CommentsAdapter.Callback callback, boolean aIsAdmin) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        likeCounterTextView = itemView.findViewById(R.id.likeCounterTextView);
        likesImageView = itemView.findViewById(R.id.likesImageView);
        likeViewGroup = itemView.findViewById(R.id.likesContainer);
        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        commentTextView = (ExpandableTextView) itemView.findViewById(R.id.commentText);
        expandableTextView = itemView.findViewById(R.id.expandable_text);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        optionMenuButton = itemView.findViewById(R.id.optionMenuButton);
        playImageView = itemView.findViewById(R.id.playimageView);
        userRewardSpinner = itemView.findViewById(R.id.rewardUserSpinner);
        userRewardTextView = itemView.findViewById(R.id.rewardUserTextView);
        rewardSpinner = itemView.findViewById(R.id.rewardSpinner);
        rewardTextView = itemView.findViewById(R.id.rewardText);
        adminTitleTextView = itemView.findViewById(R.id.admin_title_text_view);
        userTitleTextView = itemView.findViewById(R.id.user_reward_text_view);
        isAdmin = aIsAdmin;

        likesImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (callback != null && position != RecyclerView.NO_POSITION) {
                    callback.onLikeClick(likeController, position);
                }
            }
        });

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
        likeController = new LikeController(context, post, comment, likeCounterTextView, likesImageView, true);
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

        if (comment.getLikesCount() > 0) {
            String heartLabel = context.getResources().getQuantityString(R.plurals.likes_counter_format, comment.getLikesCount(), comment.getLikesCount());
            String likeCounterText = comment.getLikesCount() + " " + heartLabel;
            SpannableString underlineLikeCounterText = new SpannableString(likeCounterText);
            underlineLikeCounterText.setSpan(new UnderlineSpan(), 0, underlineLikeCounterText.length(), 0);
            likeCounterTextView.setText(underlineLikeCounterText);
            likeCounterTextView.setOnClickListener((v)-> {
                callback.onLikeUserListClick(getAdapterPosition());
            });
        } else {
            likeCounterTextView.setText("");
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
            adminTitleTextView.setVisibility(View.VISIBLE);
            rewardTextView.setVisibility(View.VISIBLE);
            rewardTextView.setText(Html.fromHtml(String.format(context.getString(R.string.comment_reward_points), comment.getReputationPoints())));
            rewardTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showReputationDialog("Admin");
                }
            });
        } else {
            rewardTextView.setVisibility(View.GONE);
            adminTitleTextView.setVisibility(View.GONE);
        }

        if(!(comment.getAuthorId().equals(post.getAuthorId())) && hasAccessToModifyPost(post)) {

            userRewardSpinner.setVisibility(View.VISIBLE);
            if(comment.getUserRewardPoints() == 0)
                userRewardSpinner.setSelection(0);
            else
                userRewardSpinner.setSelection(comment.getUserRewardPoints()+2);
            int initialPosition = userRewardSpinner.getSelectedItemPosition();
            userRewardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if(initialPosition == position)return;
                    String val = (String)parent.getItemAtPosition(position);
                    if(position == 0)
                        callback.onUserRewardClick(view, getAdapterPosition(), -2);
                    else{
                        callback.onUserRewardClick(view, getAdapterPosition(), Integer.parseInt(val));
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        if(comment.getUserRewardPoints()> -2 && comment.getUserRewardPoints()!=0){
            userTitleTextView.setVisibility(View.VISIBLE);
            userRewardTextView.setVisibility(View.VISIBLE);
            if(comment.getUserRewardPoints()!=-1)
                userRewardTextView.setText(Html.fromHtml(String.format(context.getString(R.string.comment_reward_points), comment.getUserRewardPoints())));
            else
                userRewardTextView.setText(Html.fromHtml(String.format(context.getString(R.string.comment_reward_negative), comment.getUserRewardPoints())));

            userRewardTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showReputationDialog("Post Author");
                }
            });
        }
        else {
            userRewardTextView.setVisibility(View.GONE);
            userTitleTextView.setVisibility(View.GONE);
        }
    }

    private void showReputationDialog(String str) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_reputation_points_about, null);

        TextView messageTextView = view.findViewById(R.id.dialog_message_textView);
        messageTextView.setText(String.format(context.getResources().getString(R.string.reward_points_dialog_text), str));

        final TextView reputationLinkTextView = view.findViewById(R.id.reputationLinkTextView);
        reputationLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open profile activity
                Intent intent = new Intent(context, LeaderboardActivity.class);
                context.startActivity(intent);
            }
        });
        AlertDialog.Builder builder = new BaseAlertDialogBuilder(context);
        builder.setView(view);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView, final ImageView avatarImageView, final String comment) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
//                // Added to resolve NPE
                if (obj.getUsername() != null) {
                    mUserName = obj.getUsername();
                    fillComment(mUserName, comment, expandableTextView);
                }
                if (obj.getPhotoUrl() != null) {
                    ImageUtil.loadImage(GlideApp.with(context), obj.getPhotoUrl(), avatarImageView);
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

    public void initLike(boolean isLiked) {
        likeController.initLike(isLiked);
    }
}
