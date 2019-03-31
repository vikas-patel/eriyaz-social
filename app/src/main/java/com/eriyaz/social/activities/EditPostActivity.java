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

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.eriyaz.social.R;
import com.eriyaz.social.enums.FeedbackScope;
import com.eriyaz.social.fragments.RecordPlayFragment;
import com.eriyaz.social.fragments.SavePostFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.managers.listeners.OnPostCreatedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.GlideApp;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.ValidationUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EditPostActivity extends CreatePostActivity {
    private static final String TAG = EditPostActivity.class.getSimpleName();
    public static final String POST_EXTRA_KEY = "EditPostActivity.POST_EXTRA_KEY";
    public static final int EDIT_POST_REQUEST = 33;

    private Post post;


    private TextView vName,vLength,postLimitErrorTextView;
    private CardView cardView;
    private EditText titleEditText,nickNameEditText,descriptionEditText;
    private CheckBox anonymousCheckBox;
    private ImageView avatarImageView;
    private Button retryButton,saveButton,postButton;
    private RadioGroup feedbackRadioGroup;
    private RadioButton feedbackAllRadioButton,feedbackExpertRadioButton;
    private RecordingItem item;
    private FeedbackScope feedbackScope;

    private FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_save_post);
        vName = findViewById(R.id.file_name_text);
        vLength =findViewById(R.id.file_length_text);
        cardView = findViewById(R.id.card_view);
        titleEditText =findViewById(R.id.titleEditText);
        nickNameEditText =findViewById(R.id.nickNameEditText);
        descriptionEditText =findViewById(R.id.descriptionEditText);
        anonymousCheckBox =findViewById(R.id.anonymousCheckboxId);
        avatarImageView =findViewById(R.id.avatarImageViewId);
        postLimitErrorTextView =findViewById(R.id.post_limit_error_text);
        retryButton =findViewById(R.id.retryButton);
        retryButton.setVisibility(View.INVISIBLE);
        saveButton = findViewById(R.id.saveLaterButton);
        saveButton.setVisibility(View.INVISIBLE);
        postButton =findViewById(R.id.postButton);
        postButton.setVisibility(View.INVISIBLE);
        feedbackAllRadioButton = findViewById(R.id.feedbackAllButton);
        feedbackExpertRadioButton=findViewById(R.id.feedbackExpertButton);
        feedbackRadioGroup = (RadioGroup)findViewById(R.id.feedbackRadioGroup);
        feedbackRadioGroup.clearCheck();
        post = (Post) getIntent().getSerializableExtra(POST_EXTRA_KEY);
        showProgress();
        bindData(post);
        fillUIFields();

        anonymousCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                post.setAnonymous(b);
                if(b){
                    nickNameEditText.setVisibility(View.VISIBLE);
                }
                else{
                    nickNameEditText.setVisibility(View.GONE);
                }
            }
        });


    }


    private void bindData(Post post) {
        long itemDuration = post.getAudioDuration();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes);

        vName.setText(post.getTitle());
        vLength.setText(String.format("%02d:%02d", minutes, seconds));
        titleEditText.setText(post.getTitle());
        descriptionEditText.setText(post.getDescription());

        anonymousCheckBox.setChecked(post.isAnonymous());
        if(post.isAnonymous()){
            nickNameEditText.setVisibility(View.VISIBLE);
            nickNameEditText.setText(post.getNickName());
        }
        else{
            nickNameEditText.setVisibility(View.GONE);
        }

        GlideApp.with(this).load(post.getAvatarImageUrl()).into(avatarImageView);
        if(post.getFeedbackScope()==FeedbackScope.ALL){
            feedbackAllRadioButton.setChecked(true);
        }
        else{
            feedbackExpertRadioButton.setChecked(true);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        addCheckIsPostChangedListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        postManager.closeListeners(this);
    }

    public void onPostSaved(boolean success) {
        hideProgress();
        creatingPost = false;

        if (success) {
            setResult(RESULT_OK);
            finish();
        } else {
            showSnackBar(R.string.error_fail_update_post);
        }
    }

//    @Override
//    protected void savePost(final String title, final String description) {
//        doSavePost(title, description);
//    }

    private void addCheckIsPostChangedListener() {
        PostManager.getInstance(this).getPost(this, post.getId(), new OnPostChangedListener() {
            @Override
            public void onObjectChanged(Post obj) {
                if (obj == null) {
                    showWarningDialog(getResources().getString(R.string.error_post_was_removed));
                } else {
                    checkIsPostCountersChanged(obj);
                }
            }

            @Override
            public void onError(String errorText) {
                showWarningDialog(errorText);
            }

            private void showWarningDialog(String message) {
                AlertDialog.Builder builder = new BaseAlertDialogBuilder(EditPostActivity.this);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openMainActivity();
                        finish();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        });
    }

    private void checkIsPostCountersChanged(Post updatedPost) {
        if (post.getLikesCount() != updatedPost.getLikesCount()) {
            post.setLikesCount(updatedPost.getLikesCount());
        }

        if (post.getCommentsCount() != updatedPost.getCommentsCount()) {
            post.setCommentsCount(updatedPost.getCommentsCount());
        }

        if (post.getWatchersCount() != updatedPost.getWatchersCount()) {
            post.setWatchersCount(updatedPost.getWatchersCount());
        }

        if (post.isHasComplain() != updatedPost.isHasComplain()) {
            post.setHasComplain(updatedPost.isHasComplain());
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void doSavePost(String title, String description) {
        showProgress(R.string.message_saving);
        post.setTitle(title);
        post.setDescription(description);

//        if (imageUri != null) {
//            postManager.createOrUpdatePostWithImage(imageUri, EditPostActivity.this, post);
//        } else {
//            postManager.createOrUpdatePost(post);
//            onPostSaved(true);
//        }
    }

    private void fillUIFields() {
//        titleEditText.setText(post.getTitle());
//        descriptionEditText.setText(post.getDescription());
        //loadPostDetailsImage();
        hideProgress();
    }

    private void loadPostDetailsImage() {
        ImageUtil.loadImageCenterCrop(GlideApp.with(this), post.getImagePath(), imageView, new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save:

                if ((hasInternetConnection())) {
                    attemptCreatePost();
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }


                if (!creatingPost) {
                    if (hasInternetConnection()) {

                        attemptCreatePost();
                    } else {
                        showSnackBar(R.string.internet_connection_failed);
                    }
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void attemptCreatePost() {
        // Reset errors.
        titleEditText.setError(null);
        descriptionEditText.setError(null);
        nickNameEditText.setError(null);
        postLimitErrorTextView.setText("");
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String nickName = nickNameEditText.getText().toString().trim();
        if(feedbackRadioGroup.getCheckedRadioButtonId()==R.id.feedbackAllButton){
            feedbackScope=FeedbackScope.ALL;
        }
        else{
            feedbackScope=FeedbackScope.EXPERT;
        }
        View focusView = null;
        boolean cancel = false;

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.warning_empty_title));
            focusView = titleEditText;
            cancel = true;
        } else if (!ValidationUtil.isPostTitleValid(title)) {
            titleEditText.setError(getString(R.string.error_post_title_length));
            focusView = titleEditText;
            cancel = true;
        }

        if (!TextUtils.isEmpty(description) && !ValidationUtil.isPostDescriptionValid(description)) {
            descriptionEditText.setError(getString(R.string.error_post_description_length));
            focusView = descriptionEditText;
            cancel = true;
        }

        if (anonymousCheckBox.isChecked() && TextUtils.isEmpty(nickName)) {
            nickNameEditText.setError("Nick name is required for anonymous post");
            focusView = anonymousCheckBox;
            cancel = true;
        }

        if (feedbackScope == null) {
            feedbackAllRadioButton.setError("Choose an option.");

            focusView = feedbackAllRadioButton;
            cancel = true;
        }

//        long lastPostDate = this.getProfile().getLastPostCreatedDate();
//        long currentTime = Calendar.getInstance().getTimeInMillis();
//        // 4 hrs
//        long minInterval = 2* DateUtils.HOUR_IN_MILLIS;
//        if (currentTime - lastPostDate < minInterval) {
//            cancel = true;
//            long hours = (lastPostDate + minInterval - currentTime)/(1000 * 60 * 60);
//            long mins = (((lastPostDate + minInterval - currentTime))/(1000*60)) % 60;
//            String nextPostDateText = "";
//            if (hours > 0) {
//                nextPostDateText = hours + " hours ";
//            }
//            if (mins > 0) {
//                nextPostDateText = nextPostDateText +  mins + " minutes";
//            }
//        }

        if (!cancel) {
            this.hideKeyboard();
            post.setNickName(nickName);
            post.setDescription(description);
            post.setTitle(title);
            post.setFeedbackScope(feedbackScope);
            post.setAnonymous(anonymousCheckBox.isChecked());

            //since PostManager.createOrUpdatePost method is also updating the lastpostCreatedDate and upload count
            //so this method is for avoiding that.
            createOrUpdatePost(post, new OnPostCreatedListener() {
                @Override
                public void onPostSaved(boolean success, String error) {


                    hideProgress();
                    if (success) {

                        Log.d("if","inside on ost saved");
                        setResult(RESULT_OK);
                        LogUtil.logDebug(TAG, "Post was created");
                    } else {

                        Log.d("else","inside on ost saved");
                        creatingPost = false;
                        showWarningDialog("Fail to create post: " + error);
                        showSnackBar(R.string.error_fail_create_post);
                        analytics.logPostFailed(error);
                    }
                    Log.d("onpostsaved","inside on ost saved") ;
                    finish();

                }
            });



        } else if (focusView != null) {
            focusView.requestFocus();
        }
        }

    public void createOrUpdatePost(final Post post, final OnPostCreatedListener onPostCreatedListener) {
        try {
            database=FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = database.getReference();

            Map<String, Object> postValues = post.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/posts/" + post.getId(), postValues);

            databaseReference.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {
  //                      DatabaseReference profileRef = database.getReference("profiles/" + post.getAuthorId());
//                        incrementPostCount(profileRef);

                        //need to call this here because, post successfully updated
                        onPostCreatedListener.onPostSaved(true, "");
                    } else {
                        onPostCreatedListener.onPostSaved(false, databaseError.getMessage());
                        LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                    }
                }

//                private void incrementPostCount(DatabaseReference profileRef) {
//                    profileRef.runTransaction(new Transaction.Handler() {
//                        @Override
//                        public Transaction.Result doTransaction(MutableData mutableData) {
//                            Profile currentValue = mutableData.getValue(Profile.class);
//                            if (currentValue != null) {
//                                currentValue.setPostCount(currentValue.getPostCount() + 1);
//                                currentValue.setLastPostCreatedDate(Calendar.getInstance().getTimeInMillis());
//                                mutableData.setValue(currentValue);
//                            }
//
//                            return Transaction.success(mutableData);
//                     }

//                        @Override
//                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
//                            onPostCreatedListener.onPostSaved(true, "");
//                            LogUtil.logInfo(TAG, "Updating post count transaction is completed.");
//                        }
//                    });
//                }
            });
            analytics.logPost();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
