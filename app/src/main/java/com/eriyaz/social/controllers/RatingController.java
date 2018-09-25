package com.eriyaz.social.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
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
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.xw.repo.BubbleSeekBar;

/**
 * Created by vikas on 18/12/17.
 */

public class RatingController {
    private static final int ANIMATION_DURATION = 300;

    private String postId;

    private LikeController.AnimationType likeAnimationType = LikeController.AnimationType.BOUNCE_ANIM;

    private TextView ratingCounterTextView;
    private TextView averageRatingTextView;
    private BubbleSeekBar ratingBar;

    private boolean isListView = false;

    private Rating rating;
    private boolean updatingRatingCounter = true;

    public RatingController(String postId, TextView ratingCounterTextView, TextView averageRatingTextView,
                            BubbleSeekBar ratingBar, boolean isListView) {
        this.postId = postId;
        this.ratingCounterTextView = ratingCounterTextView;
        this.averageRatingTextView = averageRatingTextView;
        this.ratingBar = ratingBar;
        this.isListView = isListView;
        this.rating = new Rating();
    }

    public RatingController(BubbleSeekBar ratingBar, String postId, Rating rating) {
        this.postId = postId;
        this.rating = rating;
        this.ratingBar = ratingBar;
    }

    public void ratingClickAction(float ratingValue) {
        if (!updatingRatingCounter) {
//            startAnimateLikeButton(likeAnimationType);
            if (ratingValue > 0)
                addRating(ratingValue);
            else
                removeRating();
        }
    }

    public Rating getRating() {
        return rating;
    }

    public boolean isRatingPresent() {
        if (this.rating == null || this.rating.getId() == null) return false;
        return true;
    }

    public void ratingClickActionLocal(Post post, float ratingValue) {
        setUpdatingRatingCounter(false);
        if (ratingValue > 0) {
            updateLocalPostRatingCounter(post, ratingValue);
        } else {
            removeLocalPostRatingCounter(post);
        }
        ratingClickAction(ratingValue);
    }

    public void updateDetailedText(String detailedText, OnTaskCompleteListener onTaskCompleteListener) {
        rating.setDetailedText(detailedText);
        if (rating == null || rating.getId() == null) {
            onTaskCompleteListener.onTaskComplete(false);
            return;
        }
        ApplicationHelper.getDatabaseHelper().updateRatingDetailedText(postId, rating.getId(), detailedText, onTaskCompleteListener);
    }

    private void addRating(float ratingValue) {
        updatingRatingCounter = true;
        rating.setRating(ratingValue);
        ApplicationHelper.getDatabaseHelper().createOrUpdateRating(postId, rating);
    }

    private void removeRating() {
        if (this.rating == null || this.rating.getId() == null) return;
        updatingRatingCounter = true;
        ApplicationHelper.getDatabaseHelper().removeRating(postId, this.rating);
        rating.reinit();
    }

    public void startAnimateRatingButton(LikeController.AnimationType animationType) {
        switch (animationType) {
            case BOUNCE_ANIM:
                bounceAnimateImageView();
                break;
            case COLOR_ANIM:
                colorAnimateRatingThumb();
                break;
        }
    }

    private void bounceAnimateImageView() {
        AnimatorSet animatorSet = new AnimatorSet();

//        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(ratingBar, "scaleX", 0.5f, 1f);
//        bounceAnimX.setDuration(ANIMATION_DURATION);
//        bounceAnimX.setInterpolator(new BounceInterpolator());

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(ratingBar, "scaleY", 1.5f, 1f);
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

//        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.play(bounceAnimY);
        animatorSet.start();
    }

    private void colorAnimateRatingThumb() {
        final int activatedColor = ContextCompat.getColor(ratingBar.getContext(), R.color.primary);
        final int defaultColor = ContextCompat.getColor(ratingBar.getContext(), R.color.primary_light);
        final ValueAnimator colorAnim = ObjectAnimator.ofFloat(0f, 1f);
        colorAnim.setDuration(ANIMATION_DURATION);
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float mul = (Float) animation.getAnimatedValue();
                int alpha = adjustAlpha(activatedColor, mul);
                ratingBar.setThumbColor(alpha);
                if (mul == 1.0) {
                    ratingBar.setThumbColor(defaultColor);
                }
//                ratingsImageView.setColorFilter(alpha, PorterDuff.Mode.SRC_ATOP);
//                if (mul == 0.0) {
//                    ratingsImageView.setColorFilter(null);
//                }
            }
        });
        colorAnim.start();
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
            ratingBar.setProgress(0);
        }
    }

    private void updateLocalPostRatingCounter(Post post, float ratingValue) {
        float avgRating = post.getAverageRating();
        if (rating.getId() == null || rating.getId().isEmpty()) {
            ratingCounterTextView.setText("(" + (post.getRatingsCount() + 1) + ")");
            avgRating = (avgRating*post.getRatingsCount() + ratingValue)/(post.getRatingsCount() + 1);
            post.setRatingsCount(post.getRatingsCount()+1);
        } else {
            avgRating = avgRating + (ratingValue - rating.getRating())/post.getRatingsCount();
        }
        post.setAverageRating(avgRating);
        averageRatingTextView.setText(String.format( "%.1f", avgRating));
    }

    private void removeLocalPostRatingCounter(Post post) {
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
    }

    public void handleRatingClickAction(final BaseActivity baseActivity, final Post post, final float ratingValue) {
        if (baseActivity.hasInternetConnection()) {
            doHandleRatingClickAction(baseActivity, post, ratingValue);
        } else {
            showWarningMessage(baseActivity, R.string.internet_connection_failed);
        }
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
                ratingClickAction(ratingValue);
            }
        } else {
            baseActivity.doAuthorization(profileStatus);
        }
    }
}
