package com.y59song.Plugin;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
  // May modify the content of the request and response
  public boolean handleRequest(byte[] request);
  public boolean handleResponse(byte[] response);
}
