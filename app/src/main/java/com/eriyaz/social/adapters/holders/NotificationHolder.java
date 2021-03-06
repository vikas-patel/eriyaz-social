package com.eriyaz.social.adapters.holders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.enums.PostOrigin;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.facebook.internal.BoltsMeasurementEventListener;

import static com.eriyaz.social.dialogs.warningDialog.notificationMessage;

/**
 * Created by vikas on 12/2/18.
 */

public class NotificationHolder extends RecyclerView.ViewHolder {
    private final ImageView avatarImageView;
    private final TextView messageTextView;
    private final TextView dateTextView;
    private final ProfileManager profileManager;
    private Context context;

    public NotificationHolder(View itemView) {
        super(itemView);
        this.context = itemView.getContext();

        profileManager = ProfileManager.getInstance(itemView.getContext().getApplicationContext());

        avatarImageView = itemView.findViewById(R.id.avatarImageView);
        messageTextView = itemView.findViewById(R.id.text);
        dateTextView = itemView.findViewById(R.id.dateTextView);
    }

    public void bindData(final Notification notification) {
        final String authorId = notification.getFromUserId();
        if (authorId != null) {
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(avatarImageView));
        } else {
            avatarImageView.setImageResource(R.drawable.ratemysinging);
        }

        messageTextView.setText(notification.getMessage());

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, notification.getCreatedDate());
        dateTextView.setText(date);
        if (notification.isFromSystem()) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.highlight_bg));
        } else if (!notification.isRead()) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
        } else {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.default_bg));
        }

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActionActivity(notification);
            }
        });
    }

    private void openActionActivity(Notification notification) {
        if (!notification.isRead()) {
            profileManager.markNotificationRead(notification);
        }
        if (notification.isOpenPlayStore()) {
            final String appPackageName = notification.getAction(); // getPackageName() from Context or Activity object
            try {
                ((Activity)context).startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                ((Activity)context).startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        } else if (notification.getAction() != null && !notification.getAction().isEmpty()) {
            try {
                Class<?> c = Class.forName(notification.getAction());
                Intent intent = new Intent(context, c);
                intent.putExtra(notification.getExtraKey(), notification.getExtraKeyValue());
                intent.putExtra(PostDetailsActivity.POST_ORIGIN_EXTRA_KEY, PostOrigin.APP_NOTIFICATION);
                intent.putExtra(PostDetailsActivity.IS_COMMENT_NOTIFICATION, notification.isForCommentNotification());
                //TODO: crashing in production
//                int l = notification.getMessage().length() - notificationMessage.length();
//                int l2 = notification.getMessage().length();
//                String m = notification.getMessage().substring(l, l2);
//                boolean isFeedbackRequest = false;
//                if(m.equals(notificationMessage)) isFeedbackRequest = true;
//                Log.d("TAG", String.valueOf(isFeedbackRequest));
//
//                intent.putExtra(PostDetailsActivity.IS_FEEDBACK_REQUEST_NOTIFICATION, isFeedbackRequest);

                ((Activity)context).startActivity(intent);
            } catch (ClassNotFoundException e) {
                LogUtil.logError("NotificationHolder", e.getMessage(), e);
            }
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
                            context.getResources().getDimensionPixelSize(R.dimen.notification_list_avatar_height),
                            context.getResources().getDimensionPixelSize(R.dimen.notification_list_avatar_height)));
                }
            }
        };
    }
}
