package com.y59song.Plugin;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by frank on 23/07/14.
 */
public class PhoneStateDetection implements IPlugin {
    private TelephonyManager telephonyManager;
    private final boolean DEBUG = false;
    private final String TAG = PhoneStateDetection.class.getSimpleName();
    private static String deviceID = null;
    private static String phoneNumber = null;
    private static String subscriberID = null;
    private static String androidId = null;
    private static boolean init = false;
    @Override
    public String handleRequest(String request) {
        String ret = "";
        if(deviceID != null) ret += request.contains(deviceID) ? "deviceID " : "";
        if(phoneNumber != null) ret += request.contains(phoneNumber) ? "phonenumber " : "";
        if(subscriberID != null) ret += request.contains(subscriberID) ? "subscriberID " : "";
        if(androidId != null) ret += request.contains(androidId) ? "androidID" : "";
        if(!DEBUG) return ret == "" ? null : ret;
        else {
            Log.i(TAG + " request : " + ret + " : " + request.length(), request);
            if(deviceID != null) Log.i(TAG + " deviceID", deviceID);
            if(phoneNumber != null) Log.i(TAG + " phoneNumber", phoneNumber);
            if(subscriberID != null) Log.i(TAG + " subscriberID", subscriberID);
            if(androidId != null) Log.i(TAG + " androidId", androidId);
        }
        return ret == "" ? null : " is leaking " + ret;
    }

    @Override
    public String handleResponse(String response) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
        if(deviceID != null) request = request.replace(deviceID, "49015420323751");
        request = request.replace("imei=65", "imei=55");
        return request;
    }

    @Override
    public String modifyResponse(String response) {
        return response;
    }

    @Override
    public void setContext(Context context) {
        if(init) return;
        telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        deviceID = telephonyManager.getDeviceId();
        phoneNumber = telephonyManager.getLine1Number();
        subscriberID = telephonyManager.getSubscriberId();
        androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        init = true;
    }
}
