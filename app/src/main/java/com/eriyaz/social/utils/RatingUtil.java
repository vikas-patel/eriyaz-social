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
import com.eriyaz.social.enums.UploadImagePrefix;

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
}
