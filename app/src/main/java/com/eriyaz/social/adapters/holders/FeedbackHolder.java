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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.FeedbackActivity;
import com.eriyaz.social.adapters.FeedbackAdapter;
import com.eriyaz.social.adapters.MessagesAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Feedback;
import com.eriyaz.social.model.ListItem;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by alexey on 10.05.17.
 */

public class FeedbackHolder extends ViewHolder {

    private final ImageView avatarImageView;
    private final ImageView deleteImageView;
    private final ImageView replyImageView;
    private final ExpandableTextView messageTextView;
    private final TextView dateTextView;
    private final ProfileManager profileManager;
    private FeedbackAdapter.Callback callback;
    private Context context;

    public FeedbackHolder(View itemView, final FeedbackAdapter.Callback callback) {
        super(itemView);
        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        deleteImageView = itemView.findViewById(R.id.deleteImageView);
        replyImageView = itemView.findViewById(R.id.replyImageView);
        messageTextView = (ExpandableTextView) itemView.findViewById(R.id.messageText);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);

        if (callback != null) {
            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        callback.onDeleteClick(position);
                    }
                }
            });
        }
    }


    @Override
    public void bindData(ListItem item) {
        Feedback feedback = (Feedback) item;
        LogUtil.logInfo(FeedbackActivity.TAG, feedback.toString());
        final String senderId = feedback.getAuthorId();
        String msgText;
        if (feedback.isRemoved()) {
            msgText = context.getString(R.string.placeholder_feedback_removed);
        } else {
            msgText = feedback.getText();
        }
        messageTextView.setText(msgText);
        if (senderId != null)
            profileManager.getProfileSingleValue(senderId, createOnProfileChangeListener(messageTextView,
                    avatarImageView, msgText));


        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, feedback.getCreatedDate());
        dateTextView.setText(date);
        deleteImageView.setVisibility(View.GONE);
        if (replyImageView != null) replyImageView.setVisibility(View.GONE);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            if (currentUserId.equals(senderId)) {
                deleteImageView.setVisibility(View.VISIBLE);
            }
        }

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(senderId);
            }
        });
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView, final ImageView avatarImageView, final String message) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
                String userName = obj.getUsername();
                fillMessage(userName, message, expandableTextView);

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

    private void fillMessage(String userName, String message, ExpandableTextView messageTextView) {
        Spannable contentString = new SpannableStringBuilder(userName + "   " + message);
        contentString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_text)),
                0, userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        messageTextView.setText(contentString);
    }
}