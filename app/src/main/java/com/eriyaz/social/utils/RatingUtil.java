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
import android.support.v4.content.ContextCompat;

import com.eriyaz.social.R;
import com.eriyaz.social.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;


public class RatingUtil {

    public static int getRatingColor(Context context, int progress) {
        int color;
        if (progress == 0) {
            color = ContextCompat.getColor(context, R.color.primary_light);
        } else if (progress <= 5) {
            color = ContextCompat.getColor(context, R.color.red);
        } else if (progress <= 10) {
            color = ContextCompat.getColor(context, R.color.accent);
        } else if (progress <= 15) {
            color = ContextCompat.getColor(context, R.color.light_green);
        } else {
            color = ContextCompat.getColor(context, R.color.dark_green);
        }
        return color;
    }

    public static String getRatingPercentile(int rating) {
        switch (rating) {
            case 1:
                return "Bottom 5%";
            case 2:
                return "Bottom 20%";
            case 3:
                return "Bottom 30%";
            case 4:
                return "Bottom 40%";
            case 5:
                return "Bottom 50%";
            case 6:
                return "Top 50%";
            case 7:
                return "Top 40%";
            case 8:
                return "Top 35%";
            case 9:
                return "Top 20%";
            case 10:
                return "Top 15%";
            case 11:
                return "Top 10%";
            case 12:
                return "Top 8%";
            case 13:
                return "Top 6%";
            case 14:
                return "Top 4%";
            case 15:
                return "Top 2%";
            case 16:
                return "Top 1%";
            case 17:
                return "Top 0.5%";
            case 18:
                return "Top 0.2%";
            case 19:
                return "Top 0.1%";
            case 20:
                return "Top 0.05%";
        }
        return null;
    }

    public static boolean viewedByAuthor(Post post) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            if (currentUserId.equals(post.getAuthorId())) {
                return true;
            }
        }
        return false;
    }
}
