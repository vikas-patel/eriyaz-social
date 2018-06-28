package com.eriyaz.social.utils;

import java.util.regex.Pattern;

/**
 * Created by ashwin on 29-Jun-18.
 */

public class TimestampTagUtil {
    public static String millisToTimestamp(long millis) {
        return String.format("00:%02d", Math.round(millis/1000));
    }

    public static Long timestampToMillis(String timestamp) {
        if(isValidTimestamp(timestamp)) {
            return 1000 * Long.parseLong(timestamp.split(":")[1]);
        } else return 0L;
    }

    public static boolean isValidTimestamp(String timestamp) {
        Pattern pattern = Pattern.compile("00:[0-5][0-9]");
        return pattern.matcher(timestamp).matches();
    }
}
