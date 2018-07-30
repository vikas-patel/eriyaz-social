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
import android.net.Uri;
import android.support.annotation.NonNull;

import com.eriyaz.social.enums.UploadImagePrefix;
import com.eriyaz.social.utils.Analytics;
import com.eriyaz.social.utils.ImageUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ValueEventListener;
import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.utils.LogUtil;
import com.google.firebase.storage.UploadTask;

public class CommentManager extends FirebaseListenersManager {

    private static final String TAG = CommentManager.class.getSimpleName();
    private static CommentManager instance;

    private Context context;

    public static CommentManager getInstance(Context context) {
        if (instance == null) {
            instance = new CommentManager(context);
        }

        return instance;
    }

    private CommentManager(Context context) {
        this.context = context;
    }

    public void createOrUpdateComment(String commentText, String postId, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().createComment(commentText, postId, onTaskCompleteListener);
    }

    public void createOrUpdateComment(Comment comment, String postId, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().createComment(comment, postId, onTaskCompleteListener);
    }

    public void createOrUpdateCommentWithAudio(Uri audioUri, final Comment comment, final String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (comment.getId() == null) {
            comment.setId(databaseHelper.generateCommentId());
        }
        final String imageTitle = ImageUtil.generateImageTitle(UploadImagePrefix.COMMENT, comment.getId());
        UploadTask uploadTask = databaseHelper.uploadAudio(audioUri, imageTitle, "comments");
        if (uploadTask != null) {
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    onTaskCompleteListener.onTaskComplete(false);

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    LogUtil.logDebug(TAG, "successful upload audio, audio url: " + String.valueOf(downloadUrl));

                    comment.setAudioPath(String.valueOf(downloadUrl));
                    comment.setAudioTitle(imageTitle);
                    createOrUpdateComment(comment, postId, onTaskCompleteListener);
                }
            });
        }
    }

    public void decrementCommentsCount(String postId, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().decrementCommentsCount(postId, onTaskCompleteListener);
    }

    public void getCommentsList(Context activityContext, String postId, OnDataChangedListener<Comment> onDataChangedListener) {
        ValueEventListener valueEventListener = ApplicationHelper.getDatabaseHelper().getCommentsList(postId, onDataChangedListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public Task<Void> removeAudio(String audioTitle) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        return databaseHelper.removeAudio("comments", audioTitle);
    }

    public void removeComment(final Comment comment, final String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (comment.getAudioTitle() != null && !comment.getAudioTitle().isEmpty()) {
            Task<Void> removeImageTask = removeAudio(comment.getAudioTitle());
            removeImageTask.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    removeCommentText(comment, postId, onTaskCompleteListener);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        LogUtil.logError(TAG, "removeCommentAudio()", exception);
                        onTaskCompleteListener.onTaskComplete(false);
                    }
                });
            return;
        }
        removeCommentText(comment, postId, onTaskCompleteListener);
    }

    public void removeCommentText(Comment comment, final String postId, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        databaseHelper.removeComment(comment.getId(), postId).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                decrementCommentsCount(postId, onTaskCompleteListener);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                onTaskCompleteListener.onTaskComplete(false);
                LogUtil.logError(TAG, "removeComment()", e);
            }
        });
    }

    public void updateComment(String commentId, String commentText, String postId, OnTaskCompleteListener onTaskCompleteListener) {
        ApplicationHelper.getDatabaseHelper().updateComment(commentId, commentText, postId, onTaskCompleteListener);
    }
}
