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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.fragments.EmailPasswordFragment;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.utils.GoogleApiHelper;
import com.eriyaz.social.utils.LogUtil;
import com.eriyaz.social.utils.LogoutHelper;
import com.eriyaz.social.utils.PreferencesUtil;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int SIGN_IN_GOOGLE = 9001;
    private static final int SIGN_IN_PHONE = 9002;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mCallbackManager;
    private String profilePhotoUrlLarge;

    private TextView tosTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Configure Google Sign In
        mGoogleApiClient = GoogleApiHelper.createGoogleApiClient(this);
        findViewById(R.id.googleSignInButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });
        findViewById(R.id.phoneSignInButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithPhone();
            }
        });

        // Configure firebase auth
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            LogoutHelper.signOut(mGoogleApiClient, this, false);
        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null && !user.isAnonymous()) {
                    // Profile is signed in
                    LogUtil.logDebug(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    checkIsProfileExist(user.getUid());
                } else {
                    // Profile is signed out
                    LogUtil.logDebug(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        // Configure Facebook  Sign In
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                LogUtil.logDebug(TAG, "facebook:onSuccess:" + loginResult);
                profilePhotoUrlLarge = String.format(getString(R.string.facebook_large_image_url_pattern),
                        loginResult.getAccessToken().getUserId());
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                LogUtil.logDebug(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                LogUtil.logError(TAG, "facebook:onError", error);
                showSnackBar(error.getMessage());
            }
        });

        findViewById(R.id.facebookSignInButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithFacebook();
            }
        });

        findViewById(R.id.localSignInButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EmailPasswordFragment emailPasswordFragment = new EmailPasswordFragment();
                emailPasswordFragment.show(getSupportFragmentManager(), EmailPasswordFragment.TAG);
            }
        });


        tosTextView = findViewById(R.id.tos_tv);
        String text = "By selecting one of the Account Creation Options, you agree to RateMySinging's Terms of Service and Privacy Policy";

        SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan1 = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                tosTextView.invalidate();
                startActivity(new Intent(LoginActivity.this, TnCActivity.class));
            }
        };

        spannableString.setSpan(clickableSpan1, text.indexOf('T'), text.lastIndexOf('y')+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tosTextView.setText(spannableString);
        tosTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_GOOGLE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                showProgress();
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                profilePhotoUrlLarge = String.format(getString(R.string.google_large_image_url_pattern),
                        account.getPhotoUrl(), Constants.Profile.MAX_AVATAR_SIZE);
                firebaseAuthWithGoogle(account);
            } else {
                LogUtil.logDebug(TAG, "SIGN_IN_GOOGLE failed :" + result);
                // Google Sign In failed, update UI appropriately
                hideProgress();
            }
        } else if (requestCode == SIGN_IN_GOOGLE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private void checkIsProfileExist(final String userId) {
        ProfileManager.getInstance(this).isProfileExist(userId, new OnObjectExistListener<Profile>() {
            @Override
            public void onDataChanged(boolean exist) {
                analytics.getFirebase().setUserId(userId);
                analytics.signIn();
                if (!exist) {
                    startCreateProfileActivity();
                } else {
                    PreferencesUtil.setProfileCreated(LoginActivity.this, true);
                    DatabaseHelper.getInstance(LoginActivity.this.getApplicationContext())
                            .addRegistrationToken(FirebaseInstanceId.getInstance().getToken(), userId);
                }
                hideProgress();
                finish();
            }
        });
    }

    private void startCreateProfileActivity() {
        Intent intent = new Intent(LoginActivity.this, CreateProfileActivity.class);
        intent.putExtra(CreateProfileActivity.LARGE_IMAGE_URL_EXTRA_KEY, profilePhotoUrlLarge);
        startActivity(intent);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        LogUtil.logDebug(TAG, "handleFacebookAccessToken:" + token);
        showProgress();
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        if (mAuth.getCurrentUser() !=null && mAuth.getCurrentUser().isAnonymous()) {
            migrateAnonymousToPermanent(credential);
        } else {
            sendCredentialsToFirebase(credential);
        }
    }

    private void sendCredentialsToFirebase(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    LogUtil.logDebug(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        handleAuthError(task);
                    }
                }
            });
    }

    private void handleAuthError(Task<AuthResult> task) {
        Exception exception = task.getException();
        LogUtil.logError(TAG, "signInWithCredential", exception);

        if (exception != null) {
            showWarningDialog(exception.getMessage());
        } else {
            showSnackBar(R.string.error_authentication);
        }

        hideProgress();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        LogUtil.logDebug(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        showProgress();

        final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        if (mAuth.getCurrentUser() !=null && mAuth.getCurrentUser().isAnonymous()) {
            migrateAnonymousToPermanent(credential);
        } else {
            sendCredentialsToFirebase(credential);
        }
    }

    private void migrateAnonymousToPermanent(final AuthCredential credential) {
        LogUtil.logInfo(TAG, "link credential");
        mAuth.getCurrentUser().linkWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    checkIsProfileExist(mAuth.getCurrentUser().getUid());
                } else {
                    // Failed, may be account already exists
                    mAuth.signOut();

                    sendCredentialsToFirebase(credential);
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        LogUtil.logDebug(TAG, "onConnectionFailed:" + connectionResult);
        showSnackBar(R.string.error_google_play_services);
        hideProgress();
    }

    private void signInWithGoogle() {
        if (hasInternetConnection()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, SIGN_IN_GOOGLE);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void signInWithPhone() {
        if (hasInternetConnection()) {
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.PhoneBuilder().build());

// Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    SIGN_IN_PHONE);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void signInWithFacebook() {
        if (hasInternetConnection()) {
            LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("email", "public_profile"));
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }
}

