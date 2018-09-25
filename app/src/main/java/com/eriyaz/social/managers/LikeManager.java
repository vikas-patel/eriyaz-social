/*
 *
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.eriyaz.social.managers;

import android.content.Context;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.interactors.LikeInteractor;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.Like;
import com.eriyaz.social.model.LikeUser;

public class LikeManager extends FirebaseListenersManager {

    private static final String TAG = LikeManager.class.getSimpleName();
    private static LikeManager instance;
    private LikeInteractor likeInteractor;

    private Context context;

    public static LikeManager getInstance(Context context) {
        if (instance == null) {
            instance = new LikeManager(context);
        }
        return instance;
    }

    private LikeManager(Context context) {
        this.context = context;
        likeInteractor = LikeInteractor.getInstance(context);
    }

    public void getCurrentUserCommentLikeListSingleValue(String postId, String userId, final OnDataChangedListener<Like> onDataChangedListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        likeInteractor.getCurrentUserCommentLikeListSingleValue(postId, userId, onDataChangedListener);
    }


//    public void doesUserFollowMe(String myId, String userId, final OnObjectExistListener onObjectExistListener) {
//        followInteractor.isFollowingExist(userId, myId, onObjectExistListener);
//    }
//
//    public void doIFollowUser(String myId, String userId, final OnObjectExistListener onObjectExistListener) {
//        followInteractor.isFollowingExist(myId, userId, onObjectExistListener);
//    }
//
//    public void followUser(Activity activity, String currentUserId, String targetUserId, OnRequestComplete onRequestComplete) {
//        followInteractor.followUser(activity, currentUserId, targetUserId, onRequestComplete);
//    }
//
//    public void unfollowUser(Activity activity, String currentUserId, String targetUserId, OnRequestComplete onRequestComplete) {
//        followInteractor.unfollowUser(activity, currentUserId, targetUserId, onRequestComplete);
//    }

    public void getCommentLikeUsersList(String commentId,
                                     OnDataChangedListener<LikeUser> onDataChangedListener) {
        likeInteractor.getCommentLikeUsersList(commentId, onDataChangedListener);
    }
}
