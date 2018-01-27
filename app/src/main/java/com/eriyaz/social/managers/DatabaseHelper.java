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

package com.eriyaz.social.managers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eriyaz.social.utils.Analytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.managers.listeners.OnPostListChangedListener;
import com.eriyaz.social.managers.listeners.OnProfileCreatedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Like;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.PostListResult;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Kristina on 10/28/16.
 */

public class DatabaseHelper {

    public static final String TAG = DatabaseHelper.class.getSimpleName();

    private static DatabaseHelper instance;

    private Context context;
    private FirebaseDatabase database;
    FirebaseStorage storage;
    FirebaseAuth firebaseAuth;
    private Analytics analytics;
    private Map<ValueEventListener, DatabaseReference> activeListeners = new HashMap<>();

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }

        return instance;
    }

    public DatabaseHelper(Context context) {
        this.context = context;
        analytics = new Analytics(context);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public void init() {
        database = FirebaseDatabase.getInstance();
//        database.setPersistenceEnabled(true);
        storage = FirebaseStorage.getInstance();

//        Sets the maximum time to retry upload operations if a failure occurs.
        storage.setMaxUploadRetryTimeMillis(Constants.Database.MAX_UPLOAD_RETRY_MILLIS);
    }

    public DatabaseReference getDatabaseReference() {
        return database.getReference();
    }

    public void closeListener(ValueEventListener listener) {
        if (activeListeners.containsKey(listener)) {
            DatabaseReference reference = activeListeners.get(listener);
            reference.removeEventListener(listener);
            activeListeners.remove(listener);
            LogUtil.logDebug(TAG, "closeListener(), listener was removed: " + listener);
        } else {
            LogUtil.logDebug(TAG, "closeListener(), listener not found :" + listener);
        }
    }

    public void closeAllActiveListeners() {
        for (ValueEventListener listener : activeListeners.keySet()) {
            DatabaseReference reference = activeListeners.get(listener);
            reference.removeEventListener(listener);
        }

        activeListeners.clear();
    }

    public void createOrUpdateProfile(final Profile profile, final OnProfileCreatedListener onProfileCreatedListener) {
        DatabaseReference databaseReference = ApplicationHelper.getDatabaseHelper().getDatabaseReference();
        Task<Void> task = databaseReference.child("profiles").child(profile.getId()).setValue(profile);
        task.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                onProfileCreatedListener.onProfileCreated(task.isSuccessful());
                addRegistrationToken(FirebaseInstanceId.getInstance().getToken(), profile.getId());
                LogUtil.logDebug(TAG, "createOrUpdateProfile, success: " + task.isSuccessful());
            }
        });
    }

    public void updateRegistrationToken(final String token) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            final String currentUserId = firebaseUser.getUid();

            getProfileSingleValue(currentUserId, new OnObjectChangedListener<Profile>() {
                @Override
                public void onObjectChanged(Profile obj) {
                    if(obj != null) {
                        addRegistrationToken(token, currentUserId);
                    } else {
                        LogUtil.logError(TAG, "updateRegistrationToken",
                                new RuntimeException("Profile is not found"));
                    }
                }
            });
        }
    }

    public void addRegistrationToken(String token, String userId) {
        DatabaseReference databaseReference = ApplicationHelper.getDatabaseHelper().getDatabaseReference();
        Task<Void> task = databaseReference.child("profiles").child(userId).child("notificationTokens").child(token).setValue(true);
        task.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                LogUtil.logDebug(TAG, "addRegistrationToken, success: " + task.isSuccessful());
            }
        });
    }

    public void removeRegistrationToken(String token, String userId) {
        DatabaseReference databaseReference = ApplicationHelper.getDatabaseHelper().getDatabaseReference();
        DatabaseReference tokenRef = databaseReference.child("profiles").child(userId).child("notificationTokens").child(token);
        Task<Void> task = tokenRef.removeValue();
        task.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                LogUtil.logDebug(TAG, "removeRegistrationToken, success: " + task.isSuccessful());
            }
        });
    }

    public String generatePostId() {
        DatabaseReference databaseReference = database.getReference();
        return databaseReference.child("posts").push().getKey();
    }

    public void createOrUpdatePost(Post post) {
        try {
            DatabaseReference databaseReference = database.getReference();

            Map<String, Object> postValues = post.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/posts/" + post.getId(), postValues);

            databaseReference.updateChildren(childUpdates);
            analytics.logPost(firebaseAuth.getCurrentUser().getUid());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public Task<Void> removePost(Post post) {
        DatabaseReference databaseReference = database.getReference();
        DatabaseReference postRef = databaseReference.child("posts").child(post.getId());
        return postRef.removeValue();
    }
/*
    public void updateProfileLikeCountAfterRemovingPost(Post post) {
        DatabaseReference profileRef = database.getReference("profiles/" + post.getAuthorId() + "/likesCount");
        final long likesByPostCount = post.getLikesCount();

        profileRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue != null && currentValue >= likesByPostCount) {
                    mutableData.setValue(currentValue - likesByPostCount);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                LogUtil.logInfo(TAG, "Updating likes count transaction is completed.");
            }
        });

    }
*/
    public Task<Void> removeImage(String imageTitle) {
        StorageReference storageRef = storage.getReferenceFromUrl("gs://social.appspot.com");
        StorageReference desertRef = storageRef.child("images/" + imageTitle);

        return desertRef.delete();
    }

    public Task<Void> removeAudio(String audioTitle) {
//        StorageReference storageRef = storage.getReferenceFromUrl(context.getResources().getString(R.string.storage_link));
        StorageReference storageRef = storage.getReference();
        StorageReference desertRef = storageRef.child("audios/" + audioTitle);

        return desertRef.delete();
    }

    public void createComment(String commentText, final String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        try {
            String authorId = firebaseAuth.getCurrentUser().getUid();
            DatabaseReference mCommentsReference = database.getReference().child("post-comments/" + postId);
            String commentId = mCommentsReference.push().getKey();
            Comment comment = new Comment(commentText);
            comment.setId(commentId);
            comment.setAuthorId(authorId);
            analytics.logComment(authorId);

            mCommentsReference.child(commentId).setValue(comment, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        incrementCommentsCount(postId);
                    } else {
                        LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                    }
                }

                private void incrementCommentsCount(String postId) {
                    DatabaseReference postRef = database.getReference("posts/" + postId + "/commentsCount");
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
                            if (onTaskCompleteListener != null) {
                                onTaskCompleteListener.onTaskComplete(true);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            LogUtil.logError(TAG, "createComment()", e);
        }
    }

    public void updateComment(String commentId, String commentText, String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        DatabaseReference mCommentReference = database.getReference().child("post-comments").child(postId).child(commentId).child("text");
        mCommentReference.setValue(commentText).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onTaskCompleteListener != null) {
                    onTaskCompleteListener.onTaskComplete(true);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onTaskCompleteListener != null) {
                    onTaskCompleteListener.onTaskComplete(false);
                }
                LogUtil.logError(TAG, "updateComment", e);
            }
        });
    }

    public void decrementCommentsCount(String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        DatabaseReference postRef = database.getReference("posts/" + postId + "/commentsCount");
        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue != null && currentValue >= 1) {
                    mutableData.setValue(currentValue - 1);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (onTaskCompleteListener != null) {
                    onTaskCompleteListener.onTaskComplete(true);
                }
            }
        });
    }

    public Task<Void> removeComment(String commentId,  String postId) {
        DatabaseReference databaseReference = database.getReference();
        DatabaseReference postRef = databaseReference.child("post-comments").child(postId).child(commentId);
        return postRef.removeValue();
    }

    public void onNewLikeAddedListener(ChildEventListener childEventListener) {
        DatabaseReference mLikesReference = database.getReference().child("post-likes");
        mLikesReference.addChildEventListener(childEventListener);
    }

    public void onNewPointAddedListener(ChildEventListener childEventListener) {
        String authorId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference mPointsReference = database.getReference().child("user-points").child(authorId);
        Query query = mPointsReference.orderByChild("creationDate").startAt(new Date().getTime());
        query.addChildEventListener(childEventListener);
    }

    public void createOrUpdateRating(final String postId, final String postAuthorId, final Rating rating) {
        try {
            String authorId = firebaseAuth.getCurrentUser().getUid();
            DatabaseReference mLikesReference = database.getReference().child("post-ratings").child(postId).child(authorId);
            // add rating, else update
            if (rating.getId() == null) {
                mLikesReference.push();
                String id = mLikesReference.push().getKey();
                rating.setId(id);
                rating.setAuthorId(authorId);
                analytics.logRating(authorId, Math.round(rating.getRating()));
            }
            mLikesReference.child(rating.getId()).setValue(rating);
        } catch (Exception e) {
            LogUtil.logError(TAG, "createOrUpdateRating()", e);
        }

    }

    public void createOrUpdateLike(final String postId, final String postAuthorId) {
        try {
            String authorId = firebaseAuth.getCurrentUser().getUid();
            DatabaseReference mLikesReference = database.getReference().child("post-likes").child(postId).child(authorId);
            mLikesReference.push();
            String id = mLikesReference.push().getKey();
            Like like = new Like(authorId);
            like.setId(id);

            mLikesReference.child(id).setValue(like, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        DatabaseReference postRef = database.getReference("posts/" + postId + "/likesCount");
                        incrementLikesCount(postRef);

                        DatabaseReference profileRef = database.getReference("profiles/" + postAuthorId + "/likesCount");
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

    public void incrementWatchersCount(String postId) {
        DatabaseReference postRef = database.getReference("posts/" + postId + "/watchersCount");
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
                LogUtil.logInfo(TAG, "Updating Watchers count transaction is completed.");
            }
        });
     }

    public void removeLike(final String postId, final String postAuthorId) {
        String authorId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference mLikesReference = database.getReference().child("post-likes").child(postId).child(authorId);
        mLikesReference.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    DatabaseReference postRef = database.getReference("posts/" + postId + "/likesCount");
                    decrementLikesCount(postRef);

                    DatabaseReference profileRef = database.getReference("profiles/" + postAuthorId + "/likesCount");
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

    public void removeRating(final String postId, final String postAuthorId, final Rating rating) {
        if (rating.getId() == null) return;
        String authorId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference mLikesReference = database.getReference().child("post-ratings").child(postId).child(authorId);
        mLikesReference.removeValue();
    }

    public UploadTask uploadImage(Uri uri, String imageTitle) {
//        StorageReference storageRef = storage.getReferenceFromUrl(context.getResources().getString(R.string.storage_link));
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("images/" + imageTitle);
        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCacheControl("max-age=7776000, Expires=7776000, public, must-revalidate")
                .build();

        return riversRef.putFile(uri, metadata);
    }

    public UploadTask uploadAudio(Uri uri, String audioTitle) {
//        StorageReference storageRef = storage.getReferenceFromUrl(context.getResources().getString(R.string.storage_link));
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("audios/" + audioTitle);
        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCacheControl("max-age=7776000, Expires=7776000, public, must-revalidate")
                .setContentType("audio/mpeg")
                .build();

        return riversRef.putFile(uri, metadata);
    }

    public void getPostList(final OnPostListChangedListener<Post> onDataChangedListener, long date) {
        DatabaseReference databaseReference = database.getReference("posts");
        Query postsQuery;
        if (date == 0) {
            postsQuery = databaseReference.limitToLast(Constants.Post.POST_AMOUNT_ON_PAGE).orderByChild("createdDate");
        } else {
            postsQuery = databaseReference.limitToLast(Constants.Post.POST_AMOUNT_ON_PAGE).endAt(date).orderByChild("createdDate");
        }

//        postsQuery.keepSynced(true);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> objectMap = (Map<String, Object>) dataSnapshot.getValue();
                PostListResult result = parsePostList(objectMap);

                if (result.getPosts().isEmpty() && result.isMoreDataAvailable()) {
                    getPostList(onDataChangedListener, result.getLastItemCreatedDate() - 1);
                } else {
                    onDataChangedListener.onListChanged(parsePostList(objectMap));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getPostList(), onCancelled", new Exception(databaseError.getMessage()));
                onDataChangedListener.onCanceled(context.getString(R.string.permission_denied_error));
            }
        });
    }

    public void getPostListByUser(final OnDataChangedListener<Post> onDataChangedListener, String userId) {
        DatabaseReference databaseReference = database.getReference("posts");
        Query postsQuery;
        postsQuery = databaseReference.orderByChild("authorId").equalTo(userId);

//        postsQuery.keepSynced(true);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PostListResult result = parsePostList((Map<String, Object>) dataSnapshot.getValue());
                onDataChangedListener.onListChanged(result.getPosts());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getPostListByUser(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public ValueEventListener getPost(final String id, final OnPostChangedListener listener) {
        DatabaseReference databaseReference = getDatabaseReference().child("posts").child(id);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (isPostValid((Map<String, Object>) dataSnapshot.getValue())) {
                        Post post = dataSnapshot.getValue(Post.class);
                        if (post != null) {
                            post.setId(id);
                        }
                        listener.onObjectChanged(post);
                    } else {
                        listener.onError(String.format(context.getString(R.string.error_general_post), id));
                    }
                } else {
                    listener.onObjectChanged(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getPost(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public void getSinglePost(final String id, final OnPostChangedListener listener) {
        DatabaseReference databaseReference = getDatabaseReference().child("posts").child(id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isPostValid((Map<String, Object>) dataSnapshot.getValue())) {
                    Post post = dataSnapshot.getValue(Post.class);
                    post.setId(id);
                    listener.onObjectChanged(post);
                } else {
                    listener.onError(String.format(context.getString(R.string.error_general_post), id));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getSinglePost(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    private PostListResult parsePostList(Map<String, Object> objectMap) {
        PostListResult result = new PostListResult();
        List<Post> list = new ArrayList<Post>();
        boolean isMoreDataAvailable = true;
        long lastItemCreatedDate = 0;

        if (objectMap != null) {
            isMoreDataAvailable = Constants.Post.POST_AMOUNT_ON_PAGE == objectMap.size();

            for (String key : objectMap.keySet()) {
                Object obj = objectMap.get(key);
                if (obj instanceof Map) {
                    Map<String, Object> mapObj = (Map<String, Object>) obj;

                    if (!isPostValid(mapObj)) {
                        LogUtil.logDebug(TAG, "Invalid post, id: " + key);
                        continue;
                    }

                    boolean hasComplain = mapObj.containsKey("hasComplain") && (boolean) mapObj.get("hasComplain");
                    long createdDate = (long) mapObj.get("createdDate");

                    if (lastItemCreatedDate == 0 || lastItemCreatedDate > createdDate) {
                        lastItemCreatedDate = createdDate;
                    }

                    if (!hasComplain) {
                        Post post = new Post();
                        post.setId(key);
                        post.setTitle((String) mapObj.get("title"));
                        post.setDescription((String) mapObj.get("description"));
                        post.setImagePath((String) mapObj.get("imagePath"));
                        post.setImageTitle((String) mapObj.get("imageTitle"));
                        post.setAuthorId((String) mapObj.get("authorId"));
                        post.setCreatedDate(createdDate);
                        if (mapObj.containsKey("commentsCount")) {
                            post.setCommentsCount((long) mapObj.get("commentsCount"));
                        }
//                        if (mapObj.containsKey("likesCount")) {
//                            post.setLikesCount((long) mapObj.get("likesCount"));
//                        }
                        if (mapObj.containsKey("ratingsCount")) {
                            post.setRatingsCount((long) mapObj.get("ratingsCount"));
                        }
                        if (mapObj.containsKey("audioDuration")) {
                            post.setAudioDuration((long) mapObj.get("audioDuration"));
                        }
                        if (mapObj.containsKey("averageRating")) {
                            post.setAverageRating(Float.parseFloat("" + mapObj.get("averageRating")));
                        }
                        if (mapObj.containsKey("watchersCount")) {
                            post.setWatchersCount((long) mapObj.get("watchersCount"));
                        }
                        list.add(post);
                    }
                }
            }

            Collections.sort(list, new Comparator<Post>() {
                @Override
                public int compare(Post lhs, Post rhs) {
                    return ((Long) rhs.getCreatedDate()).compareTo(lhs.getCreatedDate());
                }
            });

            result.setPosts(list);
            result.setLastItemCreatedDate(lastItemCreatedDate);
            result.setMoreDataAvailable(isMoreDataAvailable);
        }

        return result;
    }

    private boolean isPostValid(Map<String, Object> post) {
        return post.containsKey("title")
                && post.containsKey("imagePath")
                && post.containsKey("imageTitle")
                && post.containsKey("authorId");
//                && post.containsKey("description");
    }

    public void getProfileSingleValue(String id, final OnObjectChangedListener<Profile> listener) {
        DatabaseReference databaseReference = getDatabaseReference().child("profiles").child(id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Profile profile = dataSnapshot.getValue(Profile.class);
                listener.onObjectChanged(profile);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getProfileSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public ValueEventListener getUserPointsValue(final OnObjectChangedListener<Integer> listener) {
        String authorId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference databaseReference = getDatabaseReference().child("profiles").child(authorId).child("points");
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer pointValue = dataSnapshot.getValue(Integer.class);
                listener.onObjectChanged(pointValue);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getProfileSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public ValueEventListener getProfile(String id, final OnObjectChangedListener<Profile> listener) {
        DatabaseReference databaseReference = getDatabaseReference().child("profiles").child(id);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Profile profile = dataSnapshot.getValue(Profile.class);
                listener.onObjectChanged(profile);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getProfile(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public ValueEventListener getCommentsList(String postId, final OnDataChangedListener<Comment> onDataChangedListener) {
        DatabaseReference databaseReference = database.getReference("post-comments").child(postId);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Comment> list = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Comment comment = snapshot.getValue(Comment.class);
                    list.add(comment);
                }

                Collections.sort(list, new Comparator<Comment>() {
                    @Override
                    public int compare(Comment lhs, Comment rhs) {
                        return ((Long) rhs.getCreatedDate()).compareTo((Long) lhs.getCreatedDate());
                    }
                });

                onDataChangedListener.onListChanged(list);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getCommentsList(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public ValueEventListener getRatingsList(String postId, final OnDataChangedListener<Rating> onDataChangedListener) {
        DatabaseReference databaseReference = database.getReference("post-ratings").child(postId);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Rating> list = new ArrayList<>();
                for (DataSnapshot authorSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot snapshot = authorSnapshot.getChildren().iterator().next();
                    Rating rating = snapshot.getValue(Rating.class);
                    list.add(rating);
                }

                Collections.sort(list, new Comparator<Rating>() {
                    @Override
                    public int compare(Rating lhs, Rating rhs) {
                        return ((Long) rhs.getCreatedDate()).compareTo((Long) lhs.getCreatedDate());
                    }
                });

                onDataChangedListener.onListChanged(list);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getRatingssList(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public ValueEventListener getCurrentUserRating(String postId, String userId, final OnObjectChangedListener<Rating> listener) {
        DatabaseReference databaseReference = database.getReference("post-ratings").child(postId).child(userId);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() == false || dataSnapshot.hasChildren() == false) {
                    listener.onObjectChanged(null);
                    return;
                }
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Rating rating = snapshot.getValue(Rating.class);
                    listener.onObjectChanged(rating);
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getProfile(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public void getCurrentUserRatingSingleValue(String postId, String userId, final OnObjectChangedListener<Rating> listener) {
        DatabaseReference databaseReference = database.getReference("post-ratings").child(postId).child(userId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() == false || dataSnapshot.hasChildren() == false) {
                    listener.onObjectChanged(null);
                    return;
                }
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Rating rating = snapshot.getValue(Rating.class);
                    listener.onObjectChanged(rating);
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getCurrentUserRating(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public ValueEventListener hasCurrentUserLike(String postId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseReference databaseReference = database.getReference("post-likes").child(postId).child(userId);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "hasCurrentUserLike(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        activeListeners.put(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public void hasCurrentUserLikeSingleValue(String postId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseReference databaseReference = database.getReference("post-likes").child(postId).child(userId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "hasCurrentUserLikeSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public void addComplainToPost(Post post) {
        DatabaseReference databaseReference = getDatabaseReference();
        databaseReference.child("posts").child(post.getId()).child("hasComplain").setValue(true);
    }

    public void isPostExistSingleValue(String postId, final OnObjectExistListener<Post> onObjectExistListener) {
        DatabaseReference databaseReference = database.getReference("posts").child(postId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "isPostExistSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public void subscribeToNewPosts() {
        FirebaseMessaging.getInstance().subscribeToTopic("postsTopic");
    }
}
