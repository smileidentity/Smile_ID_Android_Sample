
package com.demo.smileid.sid_sdk.sidNet;

import android.content.Context;
import android.net.ConnectivityManager;

public final class SIDNetworkingUtils {

    public static boolean haveNetworkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected());
    }
}