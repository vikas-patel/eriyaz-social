package com.eriyaz.social.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.MainActivity;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.Analytics;
import com.xw.repo.BubbleSeekBar;

/**
 * Created by vikas on 18/12/17.
 */

public class RatingController {
    private static final int ANIMATION_DURATION = 300;

    private Context context;
    private String postId;
    private String postAuthorId;

    private LikeController.AnimationType likeAnimationType = LikeController.AnimationType.BOUNCE_ANIM;

    private TextView ratingCounterTextView;
    private TextView averageRatingTextView;
    private BubbleSeekBar ratingBar;

    private boolean isListView = false;

    private Rating rating;
    private boolean updatingRatingCounter = true;

    public RatingController(Context context, Post post, TextView ratingCounterTextView, TextView averageRatingTextView,
                            BubbleSeekBar ratingBar, boolean isListView) {
        this.context = context;
        this.postId = post.getId();
        this.postAuthorId = post.getAuthorId();
        this.ratingCounterTextView = ratingCounterTextView;
        this.averageRatingTextView = averageRatingTextView;
        this.ratingBar = ratingBar;
        this.isListView = isListView;
        this.rating = new Rating();
    }

    public void ratingClickAction(Post post, float ratingValue) {
        if (!updatingRatingCounter) {
            startAnimateLikeButton(likeAnimationType);
            if (ratingValue > 0) addRating(post, ratingValue);
            else removeRating(post);
//            if (!isRated) {
//                addLike(prevValue);
//            } else {
//                removeLike(prevValue);
//            }
        }
    }

    public void ratingClickActionLocal(Post post, float ratingValue) {
        setUpdatingRatingCounter(false);
        //updateLocalPostLikeCounter(post);
        ratingClickAction(post, ratingValue);
    }

    private void addRating(Post post, float ratingValue) {
        updatingRatingCounter = true;
        float oldRatingValue = rating.getRating();
        rating.setRating(ratingValue);
        float avgRating = post.getAverageRating();
        if (rating.getId() == null || rating.getId().isEmpty()) {
            ratingCounterTextView.setText("(" + (post.getRatingsCount() + 1) + ")");
            avgRating = (avgRating*post.getRatingsCount() + ratingValue)/(post.getRatingsCount() + 1);
            post.setRatingsCount(post.getRatingsCount()+1);
        } else {
            avgRating = avgRating + (ratingValue - oldRatingValue)/post.getRatingsCount();
        }
        post.setAverageRating(avgRating);
        averageRatingTextView.setText(String.format( "%.1f", avgRating));
        ApplicationHelper.getDatabaseHelper().createOrUpdateRating(postId, postAuthorId, rating, oldRatingValue);
    }

    private void removeRating(Post post) {
        updatingRatingCounter = true;
        float avgRating = post.getAverageRating();
        if (post.getRatingsCount() > 1) {
            avgRating = (avgRating*post.getRatingsCount() - this.rating.getRating())/(post.getRatingsCount() - 1);
            post.setRatingsCount(post.getRatingsCount() - 1);
            post.setAverageRating(avgRating);
        } else {
            post.setRatingsCount(0);
            post.setAverageRating(0);
        }
        ratingCounterTextView.setText("(" + post.getRatingsCount() + ")");
        averageRatingTextView.setText(String.format( "%.1f", post.getAverageRating()));
        ApplicationHelper.getDatabaseHelper().removeRating(postId, postAuthorId, this.rating.getRating());
    }

    private void startAnimateLikeButton(LikeController.AnimationType animationType) {
        switch (animationType) {
            case BOUNCE_ANIM:
                bounceAnimateImageView();
                break;
            case COLOR_ANIM:
                //colorAnimateImageView();
                break;
        }
    }

    private void bounceAnimateImageView() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(ratingBar, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(ANIMATION_DURATION);
        bounceAnimX.setInterpolator(new BounceInterpolator());

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(ratingBar, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(ANIMATION_DURATION);
        bounceAnimY.setInterpolator(new BounceInterpolator());
//        bounceAnimY.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationStart(Animator animation) {
//                ratingsImageView.setImageResource(!isRated ? R.drawable.ic_like_active
//                        : R.drawable.ic_like);
//            }
//        });

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();
    }

//    private void colorAnimateImageView() {
//        final int activatedColor = context.getResources().getColor(R.color.like_icon_activated);
//
//        final ValueAnimator colorAnim = !isRated ? ObjectAnimator.ofFloat(0f, 1f)
//                : ObjectAnimator.ofFloat(1f, 0f);
//        colorAnim.setDuration(ANIMATION_DURATION);
//        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float mul = (Float) animation.getAnimatedValue();
//                int alpha = adjustAlpha(activatedColor, mul);
//                ratingsImageView.setColorFilter(alpha, PorterDuff.Mode.SRC_ATOP);
//                if (mul == 0.0) {
//                    ratingsImageView.setColorFilter(null);
//                }
//            }
//        });
//
//        colorAnim.start();
//    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public LikeController.AnimationType getLikeAnimationType() {
        return likeAnimationType;
    }

    public void setLikeAnimationType(LikeController.AnimationType likeAnimationType) {
        this.likeAnimationType = likeAnimationType;
    }

    public boolean isUpdatingRatingCounter() {
        return updatingRatingCounter;
    }

    public void setUpdatingRatingCounter(boolean updatingRatingCounter) {
        this.updatingRatingCounter = updatingRatingCounter;
    }

    public void initRating(Rating rating) {
        if (rating != null) {
            ratingBar.setProgress(rating.getRating());
            this.rating = rating;
        } else {
            this.rating = new Rating();
        }
    }

    private void updateLocalPostLikeCounter(Post post) {
        // first time rated by user
        if (this.rating == null || this.rating.getRating() == 0) {
            post.setRatingsCount(post.getRatingsCount() + 1);
        }
    }

    public void handleRatingClickAction(final BaseActivity baseActivity, final Post post, final float ratingValue) {
        PostManager.getInstance(baseActivity.getApplicationContext()).isPostExistSingleValue(post.getId(), new OnObjectExistListener<Post>() {
            @Override
            public void onDataChanged(boolean exist) {
                if (exist) {
                    if (baseActivity.hasInternetConnection()) {
                        doHandleRatingClickAction(baseActivity, post, ratingValue);
                    } else {
                        showWarningMessage(baseActivity, R.string.internet_connection_failed);
                    }
                } else {
                    showWarningMessage(baseActivity, R.string.message_post_was_removed);
                }
            }
        });
    }

    private void showWarningMessage(BaseActivity baseActivity, int messageId) {
        if (baseActivity instanceof MainActivity) {
            ((MainActivity) baseActivity).showFloatButtonRelatedSnackBar(messageId);
        } else {
            baseActivity.showSnackBar(messageId);
        }
    }

    private void doHandleRatingClickAction(BaseActivity baseActivity, Post post, float ratingValue) {
        ProfileStatus profileStatus = ProfileManager.getInstance(baseActivity).checkProfile();

        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
            if (isListView) {
                ratingClickActionLocal(post, ratingValue);
            } else {
                ratingClickAction(post, ratingValue);
            }
        } else {
            baseActivity.doAuthorization(profileStatus);
        }
    }
}
