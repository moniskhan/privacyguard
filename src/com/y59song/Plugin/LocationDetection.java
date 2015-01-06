package com.y59song.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import com.y59song.LocationGuard.LocationGuard;
import com.y59song.Utilities.ByteOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
  private final static String TAG = LocationGuard.class.getSimpleName();
  private final boolean DEBUG = true;
  private LocationManager locationManager;
  //private final static String[] sensitiveList = {"geolocation", "longitude", "latitude"};
  private ArrayList<Double> latitudes = new ArrayList<Double>(), longitudes = new ArrayList<Double>();

  public ArrayList<Location> getLocations() {
      List<String> providers = locationManager.getAllProviders();
      ArrayList<Location> ret = new ArrayList<Location>();
      for(String provider : providers) {
          Location loc = locationManager.getLastKnownLocation(provider);
          if(loc == null) continue;
          ret.add(loc);
      }
      return ret;
  }


  @Override
  public String handleRequest(String requestStr) {
    boolean ret = false;
    //for(String s : sensitiveList) if(requestStr.contains(s)) ret = true;

    ArrayList<Location> locations = getLocations();
    for(Location loc : locations) {
        double latD = Math.round(loc.getLatitude() * 100) / 100.0, lonD = Math.round(loc.getLongitude() * 100) / 100.0;
        String latS = "" + latD, lonS = "" + lonD;
        if(DEBUG) Log.i(TAG, "" + loc.getLatitude() + " " + loc.getLongitude() + " " + latS + " " + lonS);
        ret |= requestStr.contains(latS) && requestStr.contains(lonS);
        ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));
        if(DEBUG) Log.i(TAG, latS + " " + lonS);
    }

    String msg = ret ? "is leaking location" : null;
    if(!DEBUG) return msg;
    else if(ret) Log.i(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
    //else Log.i(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);\\


    return msg;
  }

  @Override
  public String handleResponse(String responseStr) {
      //if(DEBUG) Log.i(TAG + "response : ", responseStr);
      return null;
  }

  @Override
  public String modifyRequest(String request) {
      /*
      if(DEBUG) Log.i(TAG, request);
      request = request.replace("longitude=0", "longitude=1"); // air push
      request = request.replace("ll=43", "ll=44"); // mopub
      request = request.replace("43.466667", "35.422222"); // amazon
      request = request.replace("-80.55", "139.46"); // amazon
      if(DEBUG) Log.i(TAG, request);
      */
      return request;
  }

  @Override
  public String modifyResponse(String response) {
    return response;
  }

  @Override
  public void setContext(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }

}
