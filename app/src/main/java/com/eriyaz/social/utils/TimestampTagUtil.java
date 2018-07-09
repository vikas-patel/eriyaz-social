package com.eriyaz.social.utils;

import java.util.regex.Pattern;

/**
 * Created by ashwin on 29-Jun-18.
 */

public class TimestampTagUtil {
    public static String millisToTimestamp(long millis) {
        long mins = (millis/1000)/60;
        long secs = (millis/1000)%60;
        return String.format("%02d:%02d", mins,secs);
    }

    public static Long timestampToMillis(String timestamp) {
        if(isValidTimestamp(timestamp)) {
            return 1000 * (60 * Long.parseLong(timestamp.split(":")[0])
                                +  Long.parseLong(timestamp.split(":")[1]));
        } else return 0L;
    }

    public static boolean isValidTimestamp(String timestamp) {
        Pattern pattern = Pattern.compile("[0-5][0-9]:[0-5][0-9]");
        return pattern.matcher(timestamp).matches();
    }
}
