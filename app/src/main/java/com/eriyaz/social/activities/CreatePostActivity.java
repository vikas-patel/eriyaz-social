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

package com.eriyaz.social.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.eriyaz.social.R;
import com.eriyaz.social.fragments.FileViewerFragment;
import com.eriyaz.social.fragments.RecordFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnPostCreatedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.utils.LogUtil;

import java.io.File;

public class CreatePostActivity extends BaseActivity implements OnPostCreatedListener {
    private static final String TAG = CreatePostActivity.class.getSimpleName();
    public static final int CREATE_NEW_POST_REQUEST = 11;

    protected ImageView imageView;
    protected ProgressBar progressBar;
//    protected EditText titleEditText;
//    protected EditText descriptionEditText;

    protected PostManager postManager;
    protected boolean creatingPost = false;
    protected RecordFragment recordFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_post_activity);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        postManager = PostManager.getInstance(CreatePostActivity.this);

//        titleEditText = (EditText) findViewById(R.id.titleEditText);
//        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        recordFragment  = RecordFragment.newInstance();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.record_fragment, recordFragment, "record_fragment")
                .disallowAddToBackStack()
                .commit();

//        imageView = (ImageView) findViewById(R.id.imageView);
//
//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onSelectImageClick(v);
//            }
//        });

//        titleEditText.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (titleEditText.hasFocus() && titleEditText.getError() != null) {
//                    titleEditText.setError(null);
//                    return true;
//                }
//                return false;
//            }
//        });
    }

//    protected void attemptCreatePost() {
//        // Reset errors.
//        titleEditText.setError(null);
////        descriptionEditText.setError(null);
//
//        String title = titleEditText.getText().toString().trim();
////        String description = descriptionEditText.getText().toString().trim();
//
//        View focusView = null;
//        boolean cancel = false;
//
////        if (TextUtils.isEmpty(description)) {
////            descriptionEditText.setError(getString(R.string.warning_empty_description));
////            focusView = descriptionEditText;
////            cancel = true;
////        }
//
//        if (TextUtils.isEmpty(title)) {
//            titleEditText.setError(getString(R.string.warning_empty_title));
//            focusView = titleEditText;
//            cancel = true;
//        } else if (!ValidationUtil.isPostTitleValid(title)) {
//            titleEditText.setError(getString(R.string.error_post_title_length));
//            focusView = titleEditText;
//            cancel = true;
//        }
//
////        if (!(this instanceof EditPostActivity) && imageUri == null) {
////            showWarningDialog(R.string.warning_empty_image);
////            focusView = imageView;
////            cancel = true;
////        }
//
//        if (!cancel) {
//            creatingPost = true;
//            hideKeyboard();
//            savePost(title);
//        } else if (focusView != null) {
//            focusView.requestFocus();
//        }
//    }

    public void savePost(String title, String description, String version, String audioFilePath) {
        showProgress(R.string.message_creating_post);
        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setVersion(version);
        post.setAuthorId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        FileViewerFragment fileFragment = (FileViewerFragment) getFragmentManager().findFragmentById(R.id.record_fragment);
        post.setAudioDuration(fileFragment.getRecordingItem().getLength());
        Uri audioUri = Uri.fromFile(new File(audioFilePath));
        postManager.createOrUpdatePostWithAudio(audioUri, CreatePostActivity.this, post);
    }

    @Override
    public void onPostSaved(boolean success) {
        hideProgress();

        if (success) {
            setResult(RESULT_OK);
            CreatePostActivity.this.finish();
            LogUtil.logDebug(TAG, "Post was created");
        } else {
            creatingPost = false;
            showSnackBar(R.string.error_fail_create_post);
            LogUtil.logDebug(TAG, "Failed to create a post");
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.create_post_menu, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.post:
//                if (!creatingPost) {
//                    if (hasInternetConnection()) {
//                        attemptCreatePost();
//                    } else {
//                        showSnackBar(R.string.internet_connection_failed);
//                    }
//                }
//
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
}
