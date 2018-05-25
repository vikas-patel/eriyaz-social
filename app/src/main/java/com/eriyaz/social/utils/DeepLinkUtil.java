package com.eriyaz.social.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Html;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

        void getShortLinkFailed(String dynamicLinkStr);
    }

    public DeepLinkUtil(Context context) {
        this.context = context;
    }

    public void getshortLink(final String link, final DynamicLinkCallback dynamicLinkCallback) {
        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(Uri.parse(link+"&d=1"))//+"&d=1"
                //.setLongLink(Uri.parse("https://abc123.app.goo.gl/?link=https://example.com/&apn=com.example.android&ibn=com.example.ios&d=1"))
                .buildShortDynamicLink()
                .addOnCompleteListener((Activity) context, new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            // Short link created
                            Uri shortLink = task.getResult().getShortLink();
                            Uri flowchartLink = task.getResult().getPreviewLink();
                            dynamicLinkCallback.getLinkSuccess(shortLink);
                            LogUtil.logInfo(TAG,shortLink.toString());
                            Toast.makeText(context.getApplicationContext(),shortLink.toString(), Toast.LENGTH_SHORT).show();
                        } else {
                            // Error
                            // ...
                            Toast.makeText(context.getApplicationContext(),"error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void getLink(final String link, final Integer minVersion, final DynamicLinkCallback dynamicLinkCallback) {
        LogUtil.logInfo(TAG,"getLink: "+link );
        //Log.i(TAG,"getLink: domain: "+this.context.getString(R.string.dynamic_link_domain) + "  pkg :"+this.context.getApplicationContext().getPackageName() );
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
                .setSocialMetaTagParameters(   new DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(context.getString(R.string.app_share_title))
                        .setDescription(context.getString(R.string.app_share_description))
                        .build())
                .buildShortDynamicLink()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LogUtil.logDebug(TAG,"ShortDynamicLink creation failed");
                        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                                .setLink(Uri.parse(link))
                                .setDynamicLinkDomain(context.getString(R.string.dynamic_link_domain))
                                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.eriyaz.social")
                                        .setMinimumVersion(minVersion)
                                         //.setFallbackUrl(Uri.parse("https://google.com"))
                                        .build())
                                .setSocialMetaTagParameters(   new DynamicLink.SocialMetaTagParameters.Builder()
                                        .setTitle(context.getString(R.string.app_share_title))
                                        .setDescription(context.getString(R.string.app_share_description))
                                        .build())
                                .buildDynamicLink();
                        Uri dynamicLinkUri = dynamicLink.getUri();
                        String dynamicLin = dynamicLinkUri.toString();
                        dynamicLin = dynamicLin.replace("goo.gl", "goo.gl/");

                        LogUtil.logInfo(TAG, "dynamicLinkUri :" + dynamicLinkUri);


                        //getshortLink(dynamicLin,dynamicLinkCallback);


                        //Toast.makeText(getApplicationContext(),dynamicLinkUri.toString(), Toast.LENGTH_SHORT).show();
                        dynamicLinkCallback.getShortLinkFailed(dynamicLin);
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
            //Toast.makeText(context.getApplicationContext(),shareText,Toast.LENGTH_SHORT).show();
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
            Intent chooserIntent = Intent.createChooser(intentList.get(0), Html.fromHtml(context.getString(R.string.app_share_popup_title)));
            intentList.remove(0);
            Parcelable[] targetedIntentsParcelable = intentList.toArray(new Parcelable[intentList.size()]);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntentsParcelable);
            context.startActivity(chooserIntent);
        } catch (Exception e) {
            LogUtil.logError(TAG, e.getMessage(), e);
        }
    }
}
