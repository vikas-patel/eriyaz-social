package com.eriyaz.social.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Point;
import com.eriyaz.social.model.Profile;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by vikas on 1/9/18.
 */

public class BaseCurrentProfileActivity extends BaseActivity {
    protected ProfileManager profileManager;
    protected Profile currentProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileManager = ProfileManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profileManager.checkProfile().equals(ProfileStatus.PROFILE_CREATED)) {
            profileManager.onNewPointAddedListener(BaseCurrentProfileActivity.this,
                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    newPointAddedListener());
            profileManager.getProfileValue(BaseCurrentProfileActivity.this,
                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    createOnProfileChangedListener());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        profileManager.closeChildListeners(this);
        profileManager.closeListeners(BaseCurrentProfileActivity.this);
    }

    protected void onProfileObjectChanged(Profile profile) {
        currentProfile = profile;
    }

    private OnObjectChangedListener<Profile> createOnProfileChangedListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile aProfile) {
                onProfileObjectChanged(aProfile);
            }
        };
    }

    private OnObjectChangedListener<Point> newPointAddedListener() {
        return new OnObjectChangedListener<Point>() {
            @Override
            public void onObjectChanged(Point point) {
                showPointSnackbar(point);
            }
        };
    }

    public boolean isNewUser() {
        if (currentProfile == null) return false;
        if (currentProfile.getRatingCount() <= 10) return true;
        if (currentProfile.getPostCount() == 0) return true;
        return false;
    }
}
