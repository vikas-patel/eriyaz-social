package com.eriyaz.social.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;

/**
 * Created by vikas on 4/3/18.
 */

public class RatedPostViewHolder extends PostViewHolder {

    private Post post;
    private TextView ratedValueTextView;
    private View ratedContainerView;

    public RatedPostViewHolder(View view, final OnClickListener onClickListener) {
        super(view, true);
        ratedValueTextView = view.findViewById(R.id.ratedValueTextView);
        ratedContainerView = view.findViewById(R.id.ratedContainer);
        ratedContainerView.setVisibility(View.VISIBLE);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (post == null) {
                    ((BaseActivity)v.getContext()).showSnackBar(R.string.message_post_was_removed);
                    return;
                }
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onItemClick(getAdapterPosition(), post, v);
                }
            }
        });

        authorImageContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    if (post == null) {
                        ((BaseActivity)v.getContext()).showSnackBar(R.string.message_post_was_removed);
                        return;
                    }
                    onClickListener.onAuthorClick(post.getAuthorId(), v);
                }
            }
        });

        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (post == null) {
                    ((BaseActivity)view.getContext()).showSnackBar(R.string.message_post_was_removed);
                    return;
                }
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onPlayClick(getAdapterPosition(), post, ratingByCurrentUser, view);
                }
            }
        });
    }

    public void bindData(final Rating ratingByProfileUser) {
        ratedValueTextView.setText(Integer.toString((int) ratingByProfileUser.getRating()));
        postManager.getSinglePostValue(ratingByProfileUser.getPostId(), createOnPostChangeListener(ratingByProfileUser.getPostId()));
    }

    private OnPostChangedListener createOnPostChangeListener(final String postId) {
        return new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                if (obj != null) {
                    post = obj;
                }
                RatedPostViewHolder.super.bindData(post);
            }

            @Override
            public void onError(String errorText) {

            }
        };
    }

    public interface OnClickListener {
        void onItemClick(int position, Post post, View view);
        void onPlayClick(int position, Post post, Rating rating, View view);
        void onAuthorClick(String authorId, View view);
    }

}
