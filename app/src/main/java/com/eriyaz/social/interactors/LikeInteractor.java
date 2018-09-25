/*
 *
 * Copyright 2018 Rozdoum
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

package com.eriyaz.social.interactors;

import android.content.Context;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Like;
import com.eriyaz.social.model.LikeUser;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.LogUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexey on 05.06.18.
 */

public class LikeInteractor {

    private static final String TAG = LikeInteractor.class.getSimpleName();
    private static LikeInteractor instance;

    private DatabaseHelper databaseHelper;
    private Context context;

    public static LikeInteractor getInstance(Context context) {
        if (instance == null) {
            instance = new LikeInteractor(context);
        }

        return instance;
    }

    private LikeInteractor(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    private DatabaseReference getCommentLikeUsersRef(String commentId) {
        return databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.LIKE_USER_DB_KEY)
                .child(commentId);
    }

    public void getCommentLikeUsersList(String commentId, OnDataChangedListener<LikeUser> onDataChangedListener) {
        getCommentLikeUsersRef(commentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<LikeUser> list = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    LikeUser user = snapshot.getValue(LikeUser.class);
                    user.setProfileId(snapshot.getKey());
                    list.add(user);
                }
                onDataChangedListener.onListChanged(list);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logDebug(TAG, "getFollowersList, onCancelled");
            }
        });
    }

    public void createOrUpdateLike(final String postId, final Comment comment, final String postAuthorId) {
        try {
            String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference mLikesReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.COMMENT_LIKES_DB_KEY).child(authorId).child(postId).child(comment.getId());
            Like like = new Like(authorId);

            mLikesReference.setValue(like, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        DatabaseReference posCommenttRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.POST_COMMENTS_DB_KEY).child(postId).child(comment.getId()).child("likesCount");
                        incrementLikesCount(posCommenttRef);

                        DatabaseReference profileRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.PROFILES_DB_KEY).child(postAuthorId).child("likesCount");
                        incrementLikesCount(profileRef);
                    } else {
                        LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                    }
                }

                private void incrementLikesCount(DatabaseReference postRef) {
                    postRef.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Integer currentValue = mutableData.getValue(Integer.class);
                            if (currentValue == null) {
                                mutableData.setValue(1);
                            } else {
                                mutableData.setValue(currentValue + 1);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            LogUtil.logInfo(TAG, "Updating likes count transaction is completed.");
                        }
                    });
                }

            });
        } catch (Exception e) {
            LogUtil.logError(TAG, "createOrUpdateLike()", e);
        }

    }

    public void removeLike(final String postId, final Comment comment, final String postAuthorId) {
        String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mLikesReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.COMMENT_LIKES_DB_KEY).child(authorId).child(postId).child(comment.getId());
        mLikesReference.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    DatabaseReference postCommentRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.POST_COMMENTS_DB_KEY).child(postId).child(comment.getId()).child("likesCount");
                    decrementLikesCount(postCommentRef);

                    DatabaseReference profileRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.PROFILES_DB_KEY).child(postAuthorId).child("likesCount");
                    decrementLikesCount(profileRef);
                } else {
                    LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                }
            }

            private void decrementLikesCount(DatabaseReference postRef) {
                postRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Long currentValue = mutableData.getValue(Long.class);
                        if (currentValue == null) {
                            mutableData.setValue(0);
                        } else {
                            mutableData.setValue(currentValue - 1);
                        }

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        LogUtil.logInfo(TAG, "Updating likes count transaction is completed.");
                    }
                });
            }
        });
    }

    public void getCurrentUserCommentLikeListSingleValue(String postId, String userId, final OnDataChangedListener<Like> onDataChangedListener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.COMMENT_LIKES_DB_KEY).child(userId).child(postId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Like> list = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Like like = snapshot.getValue(Like.class);
                    like.setId(snapshot.getKey());
                    list.add(like);
                }
                onDataChangedListener.onListChanged(list);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "hasCurrentUserLikeSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }
}
