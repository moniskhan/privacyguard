package com.y59song.Plugin;

import com.y59song.Utilities.ByteOperations;

import java.util.regex.Pattern;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
  private String renren_lat = "lat_gps=[\\-0-9]*&";
  private String renren_lon = "lon_gps=[\\-0-9]*&";
  @Override
  public byte[] handleRequest(byte[] request) {
    String request_str = ByteOperations.byteArrayToString(request);
    request_str = Pattern.compile(renren_lat).matcher(request_str).replaceAll("lat_gps=0&");
    request_str = Pattern.compile(renren_lon).matcher(request_str).replaceAll("lon_gps=90&");
    return request_str.getBytes();
  }

  @Override
  public byte[] handleResponse(byte[] response) {
    return response;
  }
}
