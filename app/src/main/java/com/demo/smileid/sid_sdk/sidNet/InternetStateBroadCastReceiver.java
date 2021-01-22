package com.demo.smileid.sid_sdk.sidNet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

public class InternetStateBroadCastReceiver extends BroadcastReceiver {
    boolean isRegistered = false;
    private OnConnectionReceivedListener mOnConnectionReceivedListener;
    private boolean discardInitialCheck = false;
    private IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;
        switch (action) {
            case ConnectivityManager.CONNECTIVITY_ACTION:
                if (mOnConnectionReceivedListener != null) {
                    if (!discardInitialCheck) {
                        mOnConnectionReceivedListener.onInternetStateChanged(SIDNetworkingUtils.haveNetworkConnection(context));
                    }
                    discardInitialCheck = false;
                }
                break;
        }
    }

    public void register(Context context) {
        if (isRegistered) return;
        discardInitialCheck = true;
        context.registerReceiver(this, intentFilter);
        isRegistered = true;
    }

    public void unregister(Context context) {
        if (!isRegistered) return;
        context.unregisterReceiver(this);
        isRegistered = false;
    }

    public void setOnConnectionReceivedListener(OnConnectionReceivedListener listener) {
        mOnConnectionReceivedListener = listener;
    }

    public interface OnConnectionReceivedListener {
        void onInternetStateChanged(boolean recovered);
    }

}
