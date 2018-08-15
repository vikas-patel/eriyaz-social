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

package com.eriyaz.social.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtil {

    private static final String TAG = PreferencesUtil.class.getSimpleName();

    private static final String SHARED_PREFERENCES_NAME = "com.eriyaz.social";
    private static final String PREF_PARAM_IS_PROFILE_CREATED = "isProfileCreated";
    private static final String PREF_PARAM_IS_POST_CREATED = "isPostCreated";
    private static final String PREF_PARAM_IS_RECORD_OPENED = "isPostCreated";
    private static final String PREF_PARAM_IS_POSTS_WAS_LOADED_AT_LEAST_ONCE = "isPostsWasLoadedAtLeastOnce";
    private static final String PREF_PARAM_IS_USER_RATED_AT_LEAST_ONCE = "isUserRatedAtLeastOnce";
    private static final String PREF_PARAM_IS_USER_VIEWED_RATING_AT_LEAST_ONCE = "isUserViewedRatingAtLeastOnce";
    private static final String PREF_PARAM_USER_RATING_COUNT = "userRatedCount";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static Boolean isProfileCreated(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_PROFILE_CREATED, false);
    }

    public static Boolean isPostWasLoadedAtLeastOnce(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_POSTS_WAS_LOADED_AT_LEAST_ONCE, false);
    }

    public static void setProfileCreated(Context context, Boolean isProfileCreated) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_PROFILE_CREATED, isProfileCreated).commit();
    }

    public static void setPostWasLoadedAtLeastOnce(Context context, Boolean isPostWasLoadedAtLeastOnce) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_POSTS_WAS_LOADED_AT_LEAST_ONCE, isPostWasLoadedAtLeastOnce).commit();
    }

    public static Boolean isUserRatedAtLeastOnce(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_USER_RATED_AT_LEAST_ONCE, false);
    }

    public static void setUserRatedAtLeastOnce(Context context, Boolean isUserRatedAtLeastOnce) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_USER_RATED_AT_LEAST_ONCE, isUserRatedAtLeastOnce).commit();
    }

    public static Boolean isUserViewedRatingAtLeastOnce(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_USER_VIEWED_RATING_AT_LEAST_ONCE, false);
    }

    public static void setUserViewedRatingAtLeastOnce(Context context, Boolean isUserViewedRatingAtLeastOnce) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_USER_VIEWED_RATING_AT_LEAST_ONCE, isUserViewedRatingAtLeastOnce).commit();
    }

    // many -> 3
    public static Boolean isUserRatedMany(Context context) {
        int ratingCount =  getSharedPreferences(context).getInt(PREF_PARAM_USER_RATING_COUNT, 0);
        if (ratingCount >= 3) return true;
        return false;
    }

    public static void incrementUserRatingCount(Context context) {
        int ratingCount =  getSharedPreferences(context).getInt(PREF_PARAM_USER_RATING_COUNT, 0);
        getSharedPreferences(context).edit().putInt(PREF_PARAM_USER_RATING_COUNT, ratingCount + 1).commit();
    }

    public static void clearPreferences(Context context){
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.clear();
        editor.apply();
    }

    public static Boolean isPostCreated(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_POST_CREATED, false);
    }

    public static void setPostCreated(Context context, Boolean isPostCreated) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_POST_CREATED, isPostCreated).commit();
    }

    public static Boolean isRecordOpened(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_PARAM_IS_RECORD_OPENED, false);
    }

    public static void setRecordOpened(Context context, Boolean isRecordOpened) {
        getSharedPreferences(context).edit().putBoolean(PREF_PARAM_IS_RECORD_OPENED, isRecordOpened).commit();
    }
}
