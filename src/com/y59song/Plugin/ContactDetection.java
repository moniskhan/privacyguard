package com.y59song.Plugin;

import android.content.Context;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * Created by frank on 23/07/14.
 */
public class ContactDetection implements IPlugin {
    private final Pattern phone_pattern = Pattern.compile("[0-9]* [0-9]*-[0-9]*-[0-9]*");
    private final Pattern email_pattern = Pattern.compile("[a-zA-Z.]*@[a-zA-z.]*");
    private final boolean DEBUG = false;
    @Override
    public String handleRequest(String request) {
        String ret = "";
        if(DEBUG) Log.i("Contact", request);
        if(phone_pattern.matcher(request).matches()) ret += "phone_number ";
        if(email_pattern.matcher(request).matches()) ret += "email_address ";
        return ret == "" ? null : ret + "are leaking";
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

    }
}
