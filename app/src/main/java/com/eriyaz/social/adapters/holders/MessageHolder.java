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
import com.eriyaz.social.adapters.MessagesAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.ListItem;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.views.ExpandableTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by alexey on 10.05.17.
 */

public class MessageHolder extends ViewHolder {

    private final ImageView avatarImageView;
//    private final ImageView deleteImageView;
    private final ExpandableTextView messageTextView;
    protected ImageButton optionMenuButton;
    private final TextView dateTextView;
    private final ProfileManager profileManager;
    private MessagesAdapter.Callback callback;
    private Context context;

    public MessageHolder(View itemView, final MessagesAdapter.Callback callback) {
        super(itemView);
        this.callback = callback;
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
//        deleteImageView = itemView.findViewById(R.id.deleteImageView);
        messageTextView = (ExpandableTextView) itemView.findViewById(R.id.messageText);
        dateTextView = (TextView) itemView.findViewById(R.id.dateTextView);
        optionMenuButton = itemView.findViewById(R.id.optionMenuButton);

        if (callback != null) {
//            deleteImageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    int position = getAdapterPosition();
//                    if (position != RecyclerView.NO_POSITION) {
//                        callback.onDeleteClick(position);
//                    }
//                }
//            });
        }
    }


    @Override
    public void bindData(ListItem item) {
        Message message = (Message) item;
        final String senderId = message.getSenderId();
        String msgText;
        if (message.isRemoved()) {
            msgText = context.getString(R.string.placeholder_feedback_removed);
        } else {
            msgText = message.getText();
        }
        messageTextView.setText(msgText);
        if (senderId != null) {
            profileManager.getProfileSingleValue(senderId, createOnProfileChangeListener(messageTextView,
                    avatarImageView, msgText));
        } else {
            avatarImageView.setImageResource(R.drawable.ic_person);
        }

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, message.getCreatedDate());
        dateTextView.setText(date);
//        deleteImageView.setVisibility(View.GONE);
//        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (firebaseUser != null) {
//            String currentUserId = firebaseUser.getUid();
//            if (currentUserId.equals(senderId) || currentUserId.equals(message.getReceiverId())) {
//                deleteImageView.setVisibility(View.VISIBLE);
//            }
//        }

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAuthorClick(senderId);
            }
        });

        if (hasAccessToEditMessage(message.getSenderId(), message.getReceiverId()) && !message.isRemoved()) {
            optionMenuButton.setVisibility(View.VISIBLE);
            optionMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(context, optionMenuButton);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.message_context_menu);
                    //adding click listener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.deleteMenuItem:
                                    callback.onDeleteClick(getAdapterPosition());
                                    break;
                            }
                            return false;
                        }
                    });
                    //displaying the popup
                    popup.show();
                }
            });
        } else {
            optionMenuButton.setVisibility(View.GONE);
        }
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
                } else {
                    avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(userName,
                            context.getResources().getDimensionPixelSize(R.dimen.message_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.message_avatar_height)));
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

    private boolean hasAccessToEditMessage(String messageAuthorId, String messageReceiverId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        if (messageAuthorId != null && messageAuthorId.equals(currentUser.getUid())) return true;
        if (messageReceiverId != null && messageReceiverId.equals(currentUser.getUid())) return true;
        return false;
    }
}
