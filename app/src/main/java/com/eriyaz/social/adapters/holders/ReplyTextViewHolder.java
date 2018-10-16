package com.eriyaz.social.adapters.holders;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.MessagesAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.ListItem;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.ReplyTextItem;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by vikas on 21/4/18.
 */

public class ReplyTextViewHolder extends ViewHolder {
    private final ImageView avatarImageView;
    private final EditText messageEditText;
    private Button sendButton;
    private final ProfileManager profileManager;
    private String parentId;
    private Context context;

    public ReplyTextViewHolder(View itemView, final MessagesAdapter.Callback callback) {
        super(itemView);
        this.context = itemView.getContext();
        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = (ImageView) itemView.findViewById(R.id.avatarImageView);
        messageEditText = (EditText) itemView.findViewById(R.id.messageEditText);
        sendButton = (Button) itemView.findViewById(R.id.sendButton);
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageEditText.getText().toString();

                if (messageText.length() > 0) {
                    callback.sendReply(messageText, parentId);
                    messageEditText.setText(null);
                    messageEditText.clearFocus();
                }

//                if (hasInternetConnection()) {
//                    ProfileStatus profileStatus = ProfileManager.getInstance(MessageActivity.this).checkProfile();
//
//                    if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
//                        sendComment();
//                    } else {
//                        doAuthorization(profileStatus);
//                    }
//                } else {
//                    showSnackBar(R.string.internet_connection_failed);
//                }
            }
        });
    }

    @Override
    public void bindData(ListItem item) {
        ReplyTextItem replyItem = (ReplyTextItem) item;
        parentId = replyItem.getParentId();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            profileManager.getProfileSingleValue(currentUserId, createOnProfileChangeListener(avatarImageView));
        } else {
            avatarImageView.setImageResource(R.drawable.ic_person);
        }
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ImageView avatarImageView) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;

                if (obj.getPhotoUrl() != null) {
                    ImageUtil.loadImage(GlideApp.with(context), obj.getPhotoUrl(), avatarImageView);
                } else {
                    avatarImageView.setImageDrawable(ImageUtil.getTextDrawable(obj.getUsername(),
                            context.getResources().getDimensionPixelSize(R.dimen.reply_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.reply_avatar_height)));
                }
            }
        };
    }
}
