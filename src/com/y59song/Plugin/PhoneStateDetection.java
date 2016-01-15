package com.y59song.Plugin;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.y59song.Utilities.HashHelpers;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by frank on 23/07/14.
 */
public class PhoneStateDetection implements IPlugin {
    private TelephonyManager telephonyManager;
    private final boolean DEBUG = false;
    private final String TAG = PhoneStateDetection.class.getSimpleName();
    private static HashMap<String, String> nameofValue = new HashMap<String, String>();
    private static boolean init = false;
    @Override
    public String handleRequest(String request) {
        String ret = "";
        for(String key : nameofValue.keySet()) {
            if(request.contains(key)) ret += nameofValue.get(key) + " ";
        }
        if(DEBUG) Log.i(TAG + " request : " + ret + " : " + request.length(), request);
        return ret == "" ? null : ret + " is leaking";
    }

    @Override
    public String handleResponse(String response) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
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
        ArrayList<String> info = new ArrayList<String>();
        String deviceID = telephonyManager.getDeviceId();
        if(deviceID != null) {
            nameofValue.put(deviceID, "IMEI");
            info.add(deviceID);
        }
        String phoneNumber = telephonyManager.getLine1Number();
        if(phoneNumber != null) {
            nameofValue.put(phoneNumber, "Phone Number");
            info.add(phoneNumber);
        }
        String subscriberID = telephonyManager.getSubscriberId();
        if(subscriberID != null) {
            nameofValue.put(subscriberID, "IMSI");
            info.add(subscriberID);
        }
        String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if(androidId != null) {
            nameofValue.put(androidId, "Android ID");
            info.add(androidId);
        }
        for(String key : info) {
            nameofValue.put(HashHelpers.SHA1(key), nameofValue.get(key));
        }
        init = true;
    }
}
