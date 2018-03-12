package com.mavencluster.firebaseuser.utility;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by maven cluster on 21/02/2018.
 * Connection util class to provide methods to check Internet connectivity.
 */

public class ConnectionUtil {

    /**
     * This method checks internet connection on device
     *
     * @param context context of calling class.
     * @return boolean true if internet connected else false
     */

    public static boolean isInternetOn(Context context) {
        // get Connectivity Manager object to check connection
        ConnectivityManager connectionInfo = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        if (connectionInfo != null) {
            // Check for network connections
            if (connectionInfo.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||
                    connectionInfo.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                    connectionInfo.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                    connectionInfo.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {
                return true;

            } else if (connectionInfo.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                    connectionInfo.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}

