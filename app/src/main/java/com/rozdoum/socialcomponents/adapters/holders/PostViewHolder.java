/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.rozdoum.socialcomponents.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rozdoum.socialcomponents.Constants;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.controllers.RatingController;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.managers.ProfileManager;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectChangedListener;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.model.Profile;
import com.rozdoum.socialcomponents.model.Rating;
import com.rozdoum.socialcomponents.utils.FormatterUtil;
import com.rozdoum.socialcomponents.utils.Utils;

/**
 * Created by alexey on 27.12.16.
 */

public class PostViewHolder extends RecyclerView.ViewHolder {
    public static final String TAG = PostViewHolder.class.getSimpleName();

    private Context context;
    private ImageView postImageView;
    private TextView titleTextView;
    private TextView detailsTextView;
//    private TextView likeCounterTextView;
//    private ImageView likesImageView;
    private TextView averageRatingTextView;
    private TextView ratingCounterTextView;
    private ImageView ratingsImageView;

    private TextView commentsCountTextView;
    private TextView watcherCounterTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    private ViewGroup likeViewGroup;
    private RatingBar ratingBar;

    private ProfileManager profileManager;
    private PostManager postManager;

//    private LikeController likeController;
    private RatingController ratingController;

    public PostViewHolder(View view, final OnClickListener onClickListener) {
        this(view, onClickListener, true);
    }

    public PostViewHolder(View view, final OnClickListener onClickListener, boolean isAuthorNeeded) {
        super(view);
        this.context = view.getContext();

        postImageView = (ImageView) view.findViewById(R.id.postImageView);
//        likeCounterTextView = (TextView) view.findViewById(R.id.likeCounterTextView);
//        likesImageView = (ImageView) view.findViewById(R.id.likesImageView);
        averageRatingTextView = (TextView) view.findViewById(R.id.averageRatingTextView);
        ratingCounterTextView = (TextView) view.findViewById(R.id.ratingCounterTextView);
        ratingsImageView = (ImageView) view.findViewById(R.id.ratingImageView);

        commentsCountTextView = (TextView) view.findViewById(R.id.commentsCountTextView);
        watcherCounterTextView = (TextView) view.findViewById(R.id.watcherCounterTextView);
        dateTextView = (TextView) view.findViewById(R.id.dateTextView);
        titleTextView = (TextView) view.findViewById(R.id.titleTextView);
        detailsTextView = (TextView) view.findViewById(R.id.detailsTextView);
        authorImageView = (ImageView) view.findViewById(R.id.authorImageView);
//        likeViewGroup = (ViewGroup) view.findViewById(R.id.likesContainer);
        ratingBar = (RatingBar) view.findViewById(R.id.ratingBar);

        authorImageView.setVisibility(isAuthorNeeded ? View.VISIBLE : View.GONE);

        profileManager = ProfileManager.getInstance(context.getApplicationContext());
        postManager = PostManager.getInstance(context.getApplicationContext());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onItemClick(getAdapterPosition(), v);
                }
            }
        });

//        likeViewGroup.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                int position = getAdapterPosition();
//                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
//                    onClickListener.onLikeClick(likeController, position);
//                }
//            }
//        });

        authorImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onAuthorClick(getAdapterPosition(), v);
                }
            }
        });

        //if rating value is changed.
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION && fromUser) {
                    onClickListener.onRatingClick(ratingController, position, rating);
                }
            }
        });
    }

    public void bindData(Post post) {

//        likeController = new LikeController(context, post, likeCounterTextView, likesImageView, true);
        ratingController = new RatingController(context, post, ratingCounterTextView, averageRatingTextView, ratingBar, true);

        String title = removeNewLinesDividers(post.getTitle());
        titleTextView.setText(title);
        String description = removeNewLinesDividers(post.getDescription());
        detailsTextView.setText(description);
//        likeCounterTextView.setText(String.valueOf(post.getLikesCount()));
        String avgRatingText = "";
        if (post.getAverageRating() > 0) {
            avgRatingText = String.format( "%.1f", post.getAverageRating());
        }
        averageRatingTextView.setText(avgRatingText);
        ratingCounterTextView.setText("(" + post.getRatingsCount() + ")");
        if (post.getRatingsCount() > 0) {
            ratingsImageView.setImageResource(R.drawable.ic_star_active);
        } else {
            ratingsImageView.setImageResource(R.drawable.ic_star);
        }
        commentsCountTextView.setText(String.valueOf(post.getCommentsCount()));
        watcherCounterTextView.setText(String.valueOf(post.getWatchersCount()));

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(context, post.getCreatedDate());
        dateTextView.setText(date);

        String imageUrl = post.getImagePath();
        int width = Utils.getDisplayWidth(context);
        int height = (int) context.getResources().getDimension(R.dimen.post_detail_image_height);

        // Displayed and saved to cache image, as needs for post detail.
        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .error(R.drawable.ic_stub)
                .into(postImageView);

        if (post.getAuthorId() != null) {
            profileManager.getProfileSingleValue(post.getAuthorId(), createProfileChangeListener(authorImageView));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
//            postManager.hasCurrentUserLikeSingleValue(post.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
            postManager.getCurrentUserRatingSingleValue(post.getId(), firebaseUser.getUid(), createOnRatingObjectChangedListener());
        }
    }

    private String removeNewLinesDividers(String text) {
        int decoratedTextLength = text.length() < Constants.Post.MAX_TEXT_LENGTH_IN_LIST ?
                text.length() : Constants.Post.MAX_TEXT_LENGTH_IN_LIST;
        return text.substring(0, decoratedTextLength).replaceAll("\n", " ").trim();
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener(final ImageView authorImageView) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(final Profile obj) {
                if (obj.getPhotoUrl() != null) {

                    Glide.with(context)
                            .load(obj.getPhotoUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .crossFade()
                            .into(authorImageView);
                }
            }
        };
    }

//    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
//        return new OnObjectExistListener<Like>() {
//            @Override
//            public void onDataChanged(boolean exist) {
//                likeController.initLike(exist);
//            }
//        };
//    }

    private OnObjectChangedListener<Rating> createOnRatingObjectChangedListener() {
        return new OnObjectChangedListener<Rating>() {
            @Override
            public void onObjectChanged(Rating obj) {
                ratingController.initRating(obj);
            }
        };
    }

    public interface OnClickListener {
        void onItemClick(int position, View view);

        //void onLikeClick(LikeController likeController, int position);

        void onAuthorClick(int position, View view);

        void onRatingClick(RatingController ratingController, int position, float rating);
    }
}