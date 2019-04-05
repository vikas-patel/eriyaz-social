/*
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
 */

package com.eriyaz.social.managers;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.Constants;
import com.eriyaz.social.enums.UploadImagePrefix;
import com.eriyaz.social.interactors.FollowInteractor;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.managers.listeners.OnPostCreatedListener;
import com.eriyaz.social.managers.listeners.OnPostListChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Flag;
import com.eriyaz.social.model.FollowingPost;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.Like;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.PostListResult;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.ValidationUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Kristina on 10/28/16.
 */

public class PostManager extends FirebaseListenersManager {

    private static final String TAG = PostManager.class.getSimpleName();
    private static PostManager instance;
    private int newPostsCounter = 0;
    private PostCounterWatcher postCounterWatcher;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

    private Context context;

    public static PostManager getInstance(Context context) {
        if (instance == null) {
            instance = new PostManager(context);
        }

        return instance;
    }

    private PostManager(Context context) {
        this.context = context;
    }

    public void createOrUpdatePost(boolean isUpdate, Post post, final OnPostCreatedListener onPostCreatedListener) {
        try {
            ApplicationHelper.getDatabaseHelper().createOrUpdatePost(isUpdate,post, onPostCreatedListener);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
    }

    private PostListResult parsePostResult(HashMap<String, Object> data) {
        PostListResult result = new PostListResult();
        List<Post> list = new ArrayList<Post>();
        if (data.get("lastRecentDate") != null) result.setLastItemCreatedDate((long) data.get("lastRecentDate"));
        if (data.get("lastFriendDate") != null) result.setLastFriendItemDate((long) data.get("lastFriendDate"));
        ArrayList<HashMap> postMapList = (ArrayList<HashMap>) data.get("result");
        Iterator iter = postMapList.iterator();
        while (iter.hasNext()) {
            HashMap postMap = (HashMap) iter.next();
            if (ValidationUtil.isPostValid(postMap)) {
                list.add(new Post(postMap));
            }
        }
        result.setPosts(list);
        boolean isMoreDataAvailable = Constants.Post.POST_AMOUNT_ON_PAGE == list.size();
        result.setMoreDataAvailable(isMoreDataAvailable);
        return result;
    }

    public Task<PostListResult> getPostsList(long lastRecentDate, long lastFriendDate) {
        Map<String, Object> data = new HashMap<>();
        if (lastRecentDate > 0) data.put("lastRecentDate", lastRecentDate);
        if (lastFriendDate > 0) data.put("lastFriendDate", lastFriendDate);
        return mFunctions
                .getHttpsCallable("postList")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, PostListResult>() {
                    @Override
                    public PostListResult then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Object data = task.getResult().getData();
                        return parsePostResult((HashMap<String, Object>) data);
                    }
                });
    }

    public void getFollowingPosts(String userId, OnDataChangedListener<FollowingPost> listener) {
        FollowInteractor.getInstance(context).getFollowingPosts(userId, listener);
    }

    public void followPost(Activity activity, String userId, String postId, OnTaskCompleteListener onTaskCompleteListener) {
        FollowInteractor.getInstance(context).followPost(activity, userId, postId, onTaskCompleteListener);
    }

    public void getPostsByComment(OnPostListChangedListener<Post> onDataChangedListener, long date) {
        ApplicationHelper.getDatabaseHelper().getPostList(onDataChangedListener, true, date);
    }

    public void getPostsListByUser(OnDataChangedListener<Post> onDataChangedListener, String userId) {
        ApplicationHelper.getDatabaseHelper().getPostListByUser(onDataChangedListener, userId);
    }

    public void getRatingsListByUser(OnObjectChangedListener<ItemListResult> onObjectChangedListener, long date, String userId) {
        ApplicationHelper.getDatabaseHelper().getRatingListByUser(onObjectChangedListener, date, userId);
    }

    public void getPost(Context context, String postId, OnPostChangedListener onPostChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getPost(postId, onPostChangedListener);
        addListenerToMap(context, valueEventListener);
    }

    public void getSinglePostValue(String postId, OnPostChangedListener onPostChangedListener) {
        ApplicationHelper.getDatabaseHelper().getSinglePost(postId, onPostChangedListener);
    }

    public void createOrUpdatePostWithImage(Uri imageUri, final OnPostCreatedListener onPostCreatedListener, final Post post) {
        // Register observers to listen for when the download is done or if it fails
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (post.getId() == null) {
            post.setId(databaseHelper.generatePostId());
        }

        final String imageTitle = ImageUtil.generateImageTitle(UploadImagePrefix.POST, post.getId());
        UploadTask uploadTask = databaseHelper.uploadImage(imageUri, imageTitle);

        if (uploadTask != null) {
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    onPostCreatedListener.onPostSaved(false, exception.getMessage());
                    Crashlytics.logException(exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    LogUtil.logDebug(TAG, "successful upload image, image url: " + String.valueOf(downloadUrl));

                    post.setImagePath(String.valueOf(downloadUrl));
                    post.setImageTitle(imageTitle);
                    createOrUpdatePost(false, post, onPostCreatedListener);

//                    onPostCreatedListener.onPostSaved(true);
                }
            });
        }
    }

    public void createOrUpdatePostWithAudio(Uri audioUri, final OnPostCreatedListener onPostCreatedListener, final Post post) {
        // Register observers to listen for when the download is done or if it fails
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (post.getId() == null) {
            post.setId(databaseHelper.generatePostId());
        }

        final String imageTitle = ImageUtil.generateImageTitle(UploadImagePrefix.POST, post.getId());
        UploadTask uploadTask = databaseHelper.uploadAudio(audioUri, imageTitle, "audios");

        if (uploadTask != null) {
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    onPostCreatedListener.onPostSaved(false, exception.getMessage());

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    LogUtil.logDebug(TAG, "successful upload audio, audio url: " + String.valueOf(downloadUrl));

                    post.setImagePath(String.valueOf(downloadUrl));
                    post.setImageTitle(imageTitle);
                    createOrUpdatePost(false, post, onPostCreatedListener);
                }
            });
        }
    }

    public Task<Void> removeImage(String imageTitle) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        return databaseHelper.removeImage(imageTitle);
    }

    public Task<Void> removeAudio(String audioTitle) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        return databaseHelper.removeAudio("audios", audioTitle);
    }

    public void removePost(final Post post, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        Task<Void> removeImageTask = removeAudio(post.getImageTitle());

        removeImageTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                databaseHelper.removePost(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        onTaskCompleteListener.onTaskComplete(task.isSuccessful());
//                        databaseHelper.updateProfileLikeCountAfterRemovingPost(post);
                        LogUtil.logDebug(TAG, "removePost(), is success: " + task.isSuccessful());
                    }
                });
                LogUtil.logDebug(TAG, "removeImage(): success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                LogUtil.logError(TAG, "removeImage()", exception);
                onTaskCompleteListener.onTaskComplete(false);
            }
        });
    }

    public void addComplain(Post post) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.addComplainToPost(post);
    }

    public void flagUser(Flag flag, OnTaskCompleteListener onTaskCompleteListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.createFlag(flag, onTaskCompleteListener);
    }

    public void makePublic(Post post) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.makePostPublic(post);
    }

    public void hasCurrentUserLike(Context activityContext, String postId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        ValueEventListener valueEventListener = databaseHelper.hasCurrentUserLike(postId, userId, onObjectExistListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void hasCurrentUserLikeSingleValue(String postId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.hasCurrentUserLikeSingleValue(postId, userId, onObjectExistListener);
    }

    public void getCurrentUserRating(Context activityContext, String postId, String userId, final OnObjectChangedListener<Rating> onObjectChangedListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        ValueEventListener valueEventListener = databaseHelper.getCurrentUserRating(postId, userId, onObjectChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void getCurrentUserRatingSingleValue(String postId, String userId, final OnObjectChangedListener<Rating> onObjectChangedListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.getCurrentUserRatingSingleValue(postId, userId, onObjectChangedListener);
    }

    public void getUserRatingSingleValue(String userId, String ratingId, final OnObjectChangedListener<Rating> onObjectChangedListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.geUserRatingSingleValue(userId, ratingId, onObjectChangedListener);
    }

    public void getRatingsList(Context activityContext, String postId, OnDataChangedListener<Rating> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getRatingsList(postId, onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void isPostExistSingleValue(String postId, final OnObjectExistListener<Post> onObjectExistListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.isPostExistSingleValue(postId, onObjectExistListener);
    }

    public void incrementWatchersCount(String postId) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.incrementWatchersCount(postId);
    }

    public void incrementNewPostsCounter() {
        newPostsCounter++;
        notifyPostCounterWatcher();
    }

    public void clearNewPostsCounter() {
        newPostsCounter = 0;
        notifyPostCounterWatcher();
    }

    public int getNewPostsCounter() {
        return newPostsCounter;
    }

    public void setPostCounterWatcher(PostCounterWatcher postCounterWatcher) {
        this.postCounterWatcher = postCounterWatcher;
    }

    private void notifyPostCounterWatcher() {
        if (postCounterWatcher != null) {
            postCounterWatcher.onPostCounterChanged(newPostsCounter);
        }
    }

    public interface PostCounterWatcher {
        void onPostCounterChanged(int newValue);
    }
}
