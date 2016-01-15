package com.y59song.PrivacyGuard;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by MAK on 03/11/2015.
 */
public class LocationLeak {
    //private variables
    int _id;
    String _appName;
    String _location;
    String _timestamp;

    // Empty constructor
    public LocationLeak(){
    }

    // constructor
    public LocationLeak(int id, String name, String location, String timestamp){
        this._id = id;
        this._appName = name;
        this._location = location;
        this._timestamp = timestamp;
    }

    // constructor
    public LocationLeak(int id, String name, String location){
        this._id = id;
        this._appName = name;
        this._location = location;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this._timestamp = dateFormat.format(new Date());
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

    // getting leaked location
    public String getLocation(){
        return this._location;
    }

    // setting leaked location
    public void setLocation(String loc){
        this._location = loc;
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
