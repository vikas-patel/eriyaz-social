package com.eriyaz.social.adapters.holders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.LogUtil;

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
        messageTextView = itemView.findViewById(R.id.message);
        dateTextView = itemView.findViewById(R.id.dateTextView);
    }

    public void bindData(final Notification notification) {
        final String authorId = notification.getFromUserId();
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(avatarImageView));

        messageTextView.setText(notification.getMessage());

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, notification.getCreatedDate());
        dateTextView.setText(date);
        if (!notification.isRead()) itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));

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
        try {
            Class<?> c = Class.forName(notification.getAction());
            Intent intent = new Intent(context, c);
            intent.putExtra(notification.getExtraKey(), notification.getExtraKeyValue());
            ((Activity)context).startActivity(intent);
        } catch (ClassNotFoundException e) {
            LogUtil.logError("NotificationHolder", e.getMessage(), e);
        }
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ImageView avatarImageView) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
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
}
