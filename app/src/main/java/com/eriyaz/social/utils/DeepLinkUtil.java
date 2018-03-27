package com.eriyaz.social.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manojhimanshu on 3/26/18.
 */

public class DeepLinkUtil {
    private static final String TAG = DeepLinkUtil.class.getSimpleName();
    public Context context;
    public DynamicLinkCallback dynamicLinkCallback;

    public interface DynamicLinkCallback {
        void getLinkSuccess(Uri uri);

        void getLinkFailed();
    }

    public DeepLinkUtil(Context context) {
        this.context = context;
    }

    public void getLink(String link, Integer minVersion, final DynamicLinkCallback dynamicLinkCallback) {
        Log.i(TAG,"getLink: "+link );
        Log.i(TAG,"getLink: domain: "+this.context.getString(R.string.dynamic_link_domain) + "  pkg :"+this.context.getApplicationContext().getPackageName() );
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(link))
                .setDynamicLinkDomain(this.context.getString(R.string.dynamic_link_domain))
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder("com.eriyaz.social")
                                .setMinimumVersion(minVersion)
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
                        LogUtil.logDebug(TAG,"invitationUrl failed : ");
                        dynamicLinkCallback.getLinkFailed();
                    }
                })

                .addOnSuccessListener(new OnSuccessListener<ShortDynamicLink>() {
                    @Override
                    public void onSuccess(ShortDynamicLink shortDynamicLink) {
                        Uri invitationUrl = shortDynamicLink.getShortLink();
                        LogUtil.logDebug(TAG,"invitationUrl: "+invitationUrl);
                        dynamicLinkCallback.getLinkSuccess(invitationUrl);
                    }

                });

    }


    public void onShare(String shareText, String shareEmailSub) {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
            List<LabeledIntent> intentList = new ArrayList<>();
            for (int i = 0; i < resInfo.size(); i++) {
                ResolveInfo ri = resInfo.get(i);
                String packageName = ri.activityInfo.packageName;
                if (packageName.contains("whatsapp") || packageName.contains("twitter") || packageName.contains("facebook") || packageName.contains("android.gm")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, shareText);
                    if (packageName.contains("android.gm")) {
                        intent.putExtra(Intent.EXTRA_TEXT, shareText);
                        intent.putExtra(Intent.EXTRA_SUBJECT, shareEmailSub);
                        intent.setType("message/rfc822");
                    }
                    intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
                }
            }
            Intent chooserIntent = Intent.createChooser(intentList.get(0), context.getString(R.string.app_share_title));
            intentList.remove(0);
            Parcelable[] targetedIntentsParcelable = intentList.toArray(new Parcelable[intentList.size()]);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntentsParcelable);
            context.startActivity(chooserIntent);
        } catch (Exception e) {
            LogUtil.logError(TAG, e.getMessage(), e);
        }
    }
}
