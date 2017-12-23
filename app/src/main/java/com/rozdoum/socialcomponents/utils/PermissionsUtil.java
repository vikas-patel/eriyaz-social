package com.rozdoum.socialcomponents.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Created by vikas on 22/12/17.
 */

public class PermissionsUtil {

    public static final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    public static boolean isExplicitPermissionRequired(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean cameraPermission = hasPermissionInManifest(context, "android.permission.RECORD_AUDIO") &&
                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
            boolean storagePermission = hasPermissionInManifest(context, "android.permission.WRITE_EXTERNAL_STORAGE") &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
            return cameraPermission || storagePermission;
        }
        return false;
    }

    public static boolean hasPermissionInManifest(@NonNull Context context, @NonNull String permissionName) {
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            final String[] declaredPermisisons = packageInfo.requestedPermissions;
            if (declaredPermisisons != null && declaredPermisisons.length > 0) {
                for (String p : declaredPermisisons) {
                    if (p.equalsIgnoreCase(permissionName)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

}
