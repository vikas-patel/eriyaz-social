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

package com.eriyaz.social.adapters.holders;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.utils.Analytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.FormatterUtil;
import com.eriyaz.social.utils.Utils;

import java.util.concurrent.TimeUnit;

/**
 * Created by alexey on 27.12.16.
 */

public class PostViewHolder extends RecyclerView.ViewHolder {
    public static final String TAG = PostViewHolder.class.getSimpleName();

    private Context context;
    private TextView fileName;
    private TextView detailsTextView;
//    private TextView audioLength;
    private TextView attemptTextView;
    protected View playImageView;
    private TextView authorTextView;
    private TextView averageRatingTextView;
    private TextView ratingCounterTextView;
    private ImageView ratingsImageView;

    private TextView commentsCountTextView;
    private TextView watcherCounterTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    protected View authorImageContainerView;

    protected ProfileManager profileManager;
    protected PostManager postManager;
    protected Rating ratingByCurrentUser;
    private Analytics analytics;

    public PostViewHolder(View view, final OnClickListener onClickListener) {
        this(view, onClickListener, true);
    }

    public PostViewHolder(View view, boolean isAuthorNeeded) {
        super(view);
        this.context = view.getContext();
        analytics = new Analytics(this.context);

        fileName = view.findViewById(R.id.file_name_text);
//        audioLength = view.findViewById(R.id.file_length_text);
        attemptTextView = view.findViewById(R.id.attemptTextView);
        playImageView = view.findViewById(R.id.imageView);
        averageRatingTextView = (TextView) view.findViewById(R.id.averageRatingTextView);
        ratingCounterTextView = (TextView) view.findViewById(R.id.ratingCounterTextView);
        ratingsImageView = (ImageView) view.findViewById(R.id.ratingImageView);

        commentsCountTextView = (TextView) view.findViewById(R.id.commentsCountTextView);
        watcherCounterTextView = (TextView) view.findViewById(R.id.watcherCounterTextView);
        dateTextView = (TextView) view.findViewById(R.id.dateTextView);
        authorTextView = (TextView) view.findViewById(R.id.authorTextView);
        detailsTextView = (TextView) view.findViewById(R.id.detailsTextView);
        authorImageView = (ImageView) view.findViewById(R.id.authorImageView);
        authorImageContainerView = view.findViewById(R.id.authorImageContainer);
//        likeViewGroup = (ViewGroup) view.findViewById(R.id.likesContainer);

        authorImageContainerView.setVisibility(isAuthorNeeded ? View.VISIBLE : View.GONE);

        profileManager = ProfileManager.getInstance(context.getApplicationContext());
        postManager = PostManager.getInstance(context.getApplicationContext());
    }

    public PostViewHolder(View view, final OnClickListener onClickListener, boolean isAuthorNeeded) {
        this(view, isAuthorNeeded);
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

        authorImageContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onAuthorClick(getAdapterPosition(), v);
                }
            }
        });

        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onPlayClick(getAdapterPosition(), ratingByCurrentUser, view);
                }
            }
        });
    }


    public void bindData(final Post post) {
        String title = removeNewLinesDividers(post.getTitle());
        fileName.setText(title);
//        long minutes = TimeUnit.MILLISECONDS.toMinutes(post.getAudioDuration());
//        long seconds = TimeUnit.MILLISECONDS.toSeconds(post.getAudioDuration())
//                - TimeUnit.MINUTES.toSeconds(minutes);
//        audioLength.setText(String.format("%02d:%02d", minutes, seconds));
        if (TextUtils.isEmpty(post.getVersion())) {
            attemptTextView.setVisibility(View.GONE);
        } else {
            attemptTextView.setVisibility(View.VISIBLE);
            attemptTextView.setText(post.getVersion());
        }
        String description = removeNewLinesDividers(post.getDescription());
        if (TextUtils.isEmpty(description)) {
            detailsTextView.setVisibility(View.GONE);
        } else {
            detailsTextView.setVisibility(View.VISIBLE);
            detailsTextView.setText(description);
        }
        String avgRatingText = "";
        if (post.getAverageRating() > 0) {
            avgRatingText = String.format( "%.1f", post.getAverageRating());
        }
        averageRatingTextView.setText(avgRatingText);
        ratingCounterTextView.setText("(" + post.getRatingsCount() + ")");
        commentsCountTextView.setText(String.valueOf(post.getCommentsCount()));
        watcherCounterTextView.setText(String.valueOf(post.getWatchersCount()));

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(context, post.getCreatedDate());
        dateTextView.setText(date);

//        String imageUrl = post.getImagePath();
//        int width = Utils.getDisplayWidth(context);
//        int height = (int) context.getResources().getDimension(R.dimen.post_detail_image_height);

        // Displayed and saved to cache image, as needs for post detail.
//        Glide.with(context)
//                .load(imageUrl)
//                .centerCrop()
//                .override(width, height)
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .crossFade()
//                .error(R.drawable.ic_stub)
//                .into(postImageView);

        if (post.getAuthorId() != null) {
            profileManager.getProfileSingleValue(post.getAuthorId(), createProfileChangeListener(authorImageView));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            postManager.getCurrentUserRatingSingleValue(post.getId(), firebaseUser.getUid(), createOnRatingObjectChangedListener());
        }
    }

    private String removeNewLinesDividers(String text) {
        if (TextUtils.isEmpty(text)) return text;
        int decoratedTextLength = text.length() < Constants.Post.MAX_TEXT_LENGTH_IN_LIST ?
                text.length() : Constants.Post.MAX_TEXT_LENGTH_IN_LIST;
        return text.substring(0, decoratedTextLength).replaceAll("\n", " ").trim();
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener(final ImageView authorImageView) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(final Profile obj) {
                if (((BaseActivity)context).isActivityDestroyed()) return;
                authorTextView.setText(obj.getUsername());
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
                ratingByCurrentUser = obj;
                if (obj != null && obj.getRating() > 0) {
                    ratingsImageView.setImageResource(R.drawable.ic_star_active);
                } else {
                    ratingsImageView.setImageResource(R.drawable.ic_star);
                }
            }
        };
    }

    public interface OnClickListener {
        void onItemClick(int position, View view);
        void onPlayClick(int position, Rating rating, View view);

        //void onLikeClick(LikeController likeController, int position);

        void onAuthorClick(int position, View view);
    }
}