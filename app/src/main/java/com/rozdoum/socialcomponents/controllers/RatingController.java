package com.rozdoum.socialcomponents.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.animation.BounceInterpolator;
import android.widget.RatingBar;
import android.widget.TextView;

import com.rozdoum.socialcomponents.ApplicationHelper;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.activities.BaseActivity;
import com.rozdoum.socialcomponents.activities.MainActivity;
import com.rozdoum.socialcomponents.enums.ProfileStatus;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.managers.ProfileManager;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.model.Rating;
import com.rozdoum.socialcomponents.utils.LogUtil;

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
    private RatingBar ratingBar;

    private boolean isListView = false;

    private Rating rating;
    private boolean updatingRatingCounter = true;

    public RatingController(Context context, Post post, TextView ratingCounterTextView,
                            RatingBar ratingBar, boolean isListView) {
        this.context = context;
        this.postId = post.getId();
        this.postAuthorId = post.getAuthorId();
        this.ratingCounterTextView = ratingCounterTextView;
        this.ratingBar = ratingBar;
        this.isListView = isListView;
        this.rating = new Rating();
    }

    public void ratingClickAction(long prevValue, int ratingValue) {
        if (!updatingRatingCounter) {
            startAnimateLikeButton(likeAnimationType);
            addRating(prevValue, ratingValue);
//            if (!isRated) {
//                addLike(prevValue);
//            } else {
//                removeLike(prevValue);
//            }
        }
    }

    public void ratingClickActionLocal(Post post, int ratingValue) {
        setUpdatingRatingCounter(false);
        updateLocalPostLikeCounter(post);
        ratingClickAction(post.getLikesCount(), ratingValue);
    }

    private void addRating(long prevValue, int ratingValue) {
        updatingRatingCounter = true;
        rating.setRating(ratingValue);
        ratingCounterTextView.setText(String.valueOf(prevValue + 1));
        ApplicationHelper.getDatabaseHelper().createOrUpdateRating(postId, postAuthorId, rating);
    }

//    private void removeLike(long prevValue) {
//        updatingRatingCounter = true;
//        isRated = false;
//        ratingCounterTextView.setText(String.valueOf(prevValue - 1));
//        ApplicationHelper.getDatabaseHelper().removeLike(postId, postAuthorId);
//    }

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
            LogUtil.logInfo("RatingController", String.valueOf(rating.getRating()));
            ratingBar.setRating(rating.getRating());
            this.rating = rating;
        }
    }

    private void updateLocalPostLikeCounter(Post post) {
        // first time rated by user
        if (this.rating == null || this.rating.getRating() == 0) {
            post.setRatingsCount(post.getRatingsCount() + 1);
        }
    }

    public void handleRatingClickAction(final BaseActivity baseActivity, final Post post, final int ratingValue) {
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

    private void doHandleRatingClickAction(BaseActivity baseActivity, Post post, int ratingValue) {
        ProfileStatus profileStatus = ProfileManager.getInstance(baseActivity).checkProfile();

        if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
            if (isListView) {
                ratingClickActionLocal(post, ratingValue);
            } else {
                ratingClickAction(post.getLikesCount(), ratingValue);
            }
        } else {
            baseActivity.doAuthorization(profileStatus);
        }
    }
}
