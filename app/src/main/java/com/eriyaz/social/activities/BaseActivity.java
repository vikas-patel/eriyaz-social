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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.utils.Analytics;
import com.eriyaz.social.utils.DeepLinkUtil;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

/**
 * Created by alexey on 05.12.16.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();
    public ProgressDialog progressDialog;
    public ActionBar actionBar;
    protected Analytics analytics;
    protected DeepLinkUtil deepLinkUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        analytics = new Analytics(this);
        deepLinkUtil = new DeepLinkUtil(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.logActivity(this);
    }

    public void doAuthorization(ProfileStatus status) {
        if (status.equals(ProfileStatus.NOT_AUTHORIZED) || status.equals(ProfileStatus.NO_PROFILE)) {
            startLoginActivity();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void showProgress() {
        showProgress(R.string.loading);
    }

    public void showProgress(int message) {
        hideProgress();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(message));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void showSnackBar(int messageId) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                messageId, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void showSnackBar(View view, int messageId) {
        Snackbar snackbar = Snackbar.make(view, messageId, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void showWarningDialog(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    public void showWarningDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    public boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public boolean checkInternetConnection() {
        boolean hasInternetConnection = hasInternetConnection();
        if (!hasInternetConnection) {
            showWarningDialog(R.string.internet_connection_failed);
        }

        return hasInternetConnection;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return (super.onOptionsItemSelected(menuItem));
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public DeepLinkUtil getDeepLinkUtil() {
        return deepLinkUtil;
    }

    public boolean isActivityDestroyed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && this != null && this.isDestroyed()) {
            return true;
        }
        return false;
    }

    public void getDynamicLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        LogUtil.logInfo(TAG,"getDynamicLink.onSuccess");
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                            LogUtil.logInfo(TAG,"getDynamicLink.onSuccess :" +deepLink);
                            Toast.makeText(getApplicationContext(),deepLink.toString(), Toast.LENGTH_SHORT).show();
                        }
                        //
                        // If the user isn't signed in and the pending Dynamic Link is
                        // an invitation, sign in the user anonymously, and record the
                        // referrer's UID.
                        //
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null && deepLink != null && deepLink.getBooleanQueryParameter("invitedby", false)) {
                            String referrerUid = deepLink.getQueryParameter("invitedby");
                            getAnalytics().logInvite(referrerUid);
                            //createAnonymousAccountWithReferrerInfo(referrerUid);
                        }
                    }
                });
    }

}
