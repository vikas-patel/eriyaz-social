package com.eriyaz.social.utils;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.eriyaz.social.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

/**
 * Created by manojhimanshu on 3/26/18.
 */

public class DeepLinkUtil {

    public Context context;
    public DynamicLinkCallback dynamicLinkCallback;
    public interface DynamicLinkCallback {
        void getLinkSuccess(Uri uri);
        void getLinkFailed();
    }

    public DeepLinkUtil(Context context) {
        this.context = context;
    }

    private void getLink(String link,Integer minVersion, final DynamicLinkCallback dynamicLinkCallback){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = "anonymous";
        if(user != null) {
            uid = user.getUid();
        }
        //String
        link = "http://eriyaz.com/?invitedby=" + uid;
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(link))
                .setDynamicLinkDomain(this.context.getString(R.string.dynamic_link_domain))
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(this.context.getApplicationContext().getPackageName())
                                .setMinimumVersion(125)
                                .build())
                /*
                .setIosParameters(
                        new DynamicLink.IosParameters.Builder("com.example.ios")
                                .setAppStoreId("123456789")
                                .setMinimumVersion("1.0.1")
                                .build())
                                */
                .buildShortDynamicLink()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dynamicLinkCallback.getLinkFailed();
                    }
                })

                .addOnSuccessListener(new OnSuccessListener<ShortDynamicLink>() {
                    @Override
                    public void onSuccess(ShortDynamicLink shortDynamicLink) {
                        Uri invitationUrl = shortDynamicLink.getShortLink();
                        dynamicLinkCallback.getLinkSuccess(invitationUrl);
                    }

                });

    }
}
