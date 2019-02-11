package com.eriyaz.social.apprater;

import android.content.Context;

import com.eriyaz.social.enums.AppRaterAction;
import com.eriyaz.social.utils.Analytics;

public class AppRaterCallbackImp implements AppRaterCallback{
    private Analytics analytics;

    public AppRaterCallbackImp(Context context) {
        analytics = new Analytics(context);
    }

    @Override
    public void processNever() {
        analytics.appRater(AppRaterAction.NEVER);
    }

    @Override
    public void processRate() {
        analytics.appRater(AppRaterAction.RATE);
    }

    @Override
    public void processRemindMe() {
        analytics.appRater(AppRaterAction.LATER);
    }
}
