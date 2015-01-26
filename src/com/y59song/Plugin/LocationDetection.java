package com.y59song.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import com.y59song.LocationGuard.LocationGuard;
import com.y59song.Utilities.ByteOperations;
import com.y59song.Utilities.MyLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
  private final static String TAG = LocationGuard.class.getSimpleName();
  private LocationManager locationManager;
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

    ArrayList<Location> locations = getLocations();
    for(Location loc : locations) {
      double latD = Math.round(loc.getLatitude() * 10) / 10.0, lonD = Math.round(loc.getLongitude() * 10) / 10.0;
      String latS = "" + latD, lonS = "" + lonD;
      MyLogger.debugInfo(TAG, "" + loc.getLatitude() + " " + loc.getLongitude() + " " + latS + " " + lonS);
      ret |= requestStr.contains(latS) && requestStr.contains(lonS);
      ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));
      latD = ((int)(loc.getLatitude() * 10)) / 10.0;
      lonD = ((int)(loc.getLongitude() * 10)) / 10.0;
      latS = "" + latD;
      lonS = "" + lonD;
      //Log.d(TAG, "" + loc.getLatitude() + " " + loc.getLongitude() + " " + latS + " " + lonS);
      ret |= requestStr.contains(latS) && requestStr.contains(lonS);
      ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));
      MyLogger.debugInfo(TAG, latS + " " + lonS);
    }

    String msg = ret ? "is leaking location" : null;
    if(ret) MyLogger.debugInfo(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
    return msg;
  }

  @Override
  public String handleResponse(String responseStr) {
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
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }

}
