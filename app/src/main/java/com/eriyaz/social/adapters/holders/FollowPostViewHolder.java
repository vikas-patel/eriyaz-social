package com.eriyaz.social.adapters.holders;

import android.view.View;

import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.model.FollowingPost;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.utils.LogUtil;

/**
 * Created by Alexey on 22.05.18.
 */
public class FollowPostViewHolder extends PostViewHolder {


    public FollowPostViewHolder(View view, OnClickListener onClickListener) {
        super(view, onClickListener);
    }

    public FollowPostViewHolder(View view, OnClickListener onClickListener, boolean isAuthorNeeded) {
        super(view, onClickListener, isAuthorNeeded);
    }

    public void bindData(FollowingPost followingPost) {
        postManager.getSinglePostValue(followingPost.getPostId(), new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                bindData(obj);
            }

            @Override
            public void onError(String errorText) {
                LogUtil.logError(TAG, "bindData", new RuntimeException(errorText));
            }
        });
    }

}
