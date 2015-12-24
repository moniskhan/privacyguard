package com.y59song.PrivacyGuard;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by MAK on 03/11/2015.
 */
public class DataLeak {
    //private variables
    int _id;
    String _appName;
    String _leakType;
    int _frequency;
    String _timestamp;
    int _ignore;

    // Empty constructor
    public DataLeak(){
    }

    // constructor
    public DataLeak(int id, String name, String type, int frequency, String timestamp){
        this._id = id;
        this._appName = name;
        this._leakType = type;
        this._frequency = frequency;
        this._timestamp = timestamp;
        this._ignore = 0;
    }

    // constructor
    public DataLeak(int id, String name, String type){
        this._id = id;
        this._appName = name;
        this._leakType = type;
        this._frequency = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this._timestamp = dateFormat.format(new Date());
        this._ignore = 0;
    }
    // getting ID
    public int getID(){
        return this._id;
    }

    // setting id
    public void setID(int id){
        this._id = id;
    }

    // getting name
    public String getAppName(){
        return this._appName;
    }

    // setting name
    public void setAppName(String name){
        this._appName = name;
    }

    // getting ignore
    public int getIgnore(){
        return this._ignore;
    }

    // setting ignore
    public void setIgnore(int ignore){
        this._ignore = ignore;
    }

    // getting leak type
    public String getLeakType(){
        this._leakType = this._leakType.replace("is leaking", "");
        int endIndex = this._leakType.lastIndexOf(":");
        if (endIndex != -1) {
            this._leakType = this._leakType.substring(0, endIndex);
        }
        this._leakType = this._leakType.trim();
        return this._leakType;
    }

    // setting leak type
    public void setLeakType(String type){
        this._leakType = type;
    }

    // getting leak frequency/counter
    public int getFrequency(){
        return this._frequency;
    }

    // setting leak frequency/counter
    public void setFrequency(int freq){
        this._frequency = freq;
    }

    // getting last leak time stamp
    public String getTimeStamp(){
        return this._timestamp;
    }

    // setting last leak time stamp
    public void setTimeStamp(String timestamp){
        this._timestamp = timestamp;
    }
}
