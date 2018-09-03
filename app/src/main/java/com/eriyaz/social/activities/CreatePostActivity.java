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
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.eriyaz.social.Constants;
import com.eriyaz.social.fragments.HostFragment;
import com.eriyaz.social.fragments.SavedRecordingsFragment;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnRecordingEndListener;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.RecordingItem;
import com.google.firebase.auth.FirebaseAuth;
import com.eriyaz.social.R;
import com.eriyaz.social.fragments.SavePostFragment;
import com.eriyaz.social.fragments.RecordFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnPostCreatedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.utils.LogUtil;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CreatePostActivity extends BaseActivity implements OnRecordingEndListener {
    private static final String TAG = CreatePostActivity.class.getSimpleName();
    public static final int CREATE_NEW_POST_REQUEST = 11;

    protected ImageView imageView;
    protected ProgressBar progressBar;

    protected PostManager postManager;
    protected boolean creatingPost = false;
    private ViewPager pager;
    private TabLayout tabLayout;
    private RecordingTabAdapter tabAdapter;
    private Profile profile;
    protected ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_post_activity);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        postManager = PostManager.getInstance(CreatePostActivity.this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        pager = (ViewPager) findViewById(R.id.pager);
        tabAdapter = new RecordingTabAdapter(getSupportFragmentManager());
        pager.setAdapter(tabAdapter);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
        profileManager = ProfileManager.getInstance(this);
        profileManager.getProfileSingleValue(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                createOnProfileChangedListener());
    }

    private OnObjectChangedListener<Profile> createOnProfileChangedListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile aProfile) {
                profile = aProfile;
            }
        };
    }

    public void savePost(final RecordingItem item, String title, String description, String audioFilePath, long audioDuration,
                         boolean anonymous, String nickName, String avatarImageUrl) {
        showProgress(R.string.message_creating_post);
        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setAuthorId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        if (anonymous) {
            post.setAnonymous(true);
            post.setNickName(nickName);
            post.setAvatarImageUrl(avatarImageUrl);
        }
        if (profile != null && profile.getPostCount() == 0) {
            post.setAuthorFirstPost(true);
        }
//        FileViewerFragment fileFragment = (FileViewerFragment) getSupportFragmentManager().findFragmentById(R.id.record_fragment);
        post.setAudioDuration(audioDuration);
        if (TimeUnit.MILLISECONDS.toSeconds(post.getAudioDuration()) > Constants.RECORDING.DEFAULT_RECORDING) {
            post.setLongRecording(true);
        }
        Uri audioUri = Uri.fromFile(new File(audioFilePath));
        postManager.createOrUpdatePostWithAudio(audioUri, new OnPostCreatedListener() {
            @Override
            public void onPostSaved(boolean success) {
                hideProgress();
                if (success) {
                    setResult(RESULT_OK);
                    removeLocalRecording(item);
                    CreatePostActivity.this.finish();
                    LogUtil.logDebug(TAG, "Post was created");
                } else {
                    creatingPost = false;
                    showSnackBar(R.string.error_fail_create_post);
                    LogUtil.logDebug(TAG, "Failed to create a post");
                }
            }
        }, post);
    }

    private void removeLocalRecording(RecordingItem item) {
        File file = new File(item.getFilePath());
        file.delete();
        if (item.getId() != null && !item.getId().isEmpty()) {
            profileManager.removeSavedRecording(item.getId());
        }
    }

    public void recordingSaved() {
        startRecordFragment();
        pager.setCurrentItem(1);
    }

    public void startRecordFragment() {
        tabAdapter.replaceFragment(RecordFragment.newInstance());
    }

    @Override
    public void onRecordEnd(RecordingItem item) {
        tabAdapter.replaceFragment(SavePostFragment.newInstance(item));
        if (pager.getCurrentItem() != 0) {
            pager.setCurrentItem(0);
        }
    }

    public Profile getProfile() {
        return profile;
    }

    public class RecordingTabAdapter extends FragmentPagerAdapter {
        private String[] titles = { getString(R.string.tab_title_record),
                getString(R.string.tab_title_saved_recordings) };

        private HostFragment hostFragment;

        public RecordingTabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0:{
                    hostFragment = HostFragment.newInstance(RecordFragment.newInstance());
                    return hostFragment;
                }
                case 1:{
                    return SavedRecordingsFragment.newInstance();
                }
            }
            return null;
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public int getItemPosition(Object object)
        {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        public void replaceFragment(Fragment fragment) {
            hostFragment.replaceFragment(fragment, false);
        }
    }
}
