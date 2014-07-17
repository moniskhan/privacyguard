package com.y59song.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import com.y59song.LocationGuard.LocationGuard;

import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
  private final static String TAG = LocationGuard.class.getSimpleName();
  private final boolean DEBUG = true;
  private LocationManager locationManager;
  private final static String[] sensitiveList = {"geolocation", "longitude", "latitude"};
  @Override
  public boolean handleRequest(String requestStr) {
    boolean ret = false;
    for(String s : sensitiveList) if(requestStr.contains(s)) ret = true;
    Location loc = null;
    List<String> providers = locationManager.getAllProviders();
    for(String provider : providers) {
      loc = locationManager.getLastKnownLocation(provider);
      if(loc != null) break;
    }
    if(loc != null) {
      String latS = String.format("%.2f", loc.getLatitude()), lonS = String.format("%.2f", loc.getLongitude());
      ret |= requestStr.contains(latS) || requestStr.contains(lonS);
      ret |= requestStr.contains(latS.replace(".", "")) || requestStr.contains(lonS.replace(".", ""));
    }
    if(!DEBUG) return ret;
    else if(ret) Log.i(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
    else Log.i(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
    return ret;
  }

  @Override
  public boolean handleResponse(String responseStr) {
    return false;
  }

  @Override
  public byte[] modifyRequest(byte[] request) {
    return request;
  }

  @Override
  public byte[] modifyResponse(byte[] response) {
    return response;
  }

  @Override
  public void setContext(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }
}
