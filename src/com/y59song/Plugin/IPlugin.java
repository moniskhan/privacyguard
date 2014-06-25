package com.y59song.Plugin;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
  // May modify the content of the request and response
  public byte[] handleRequest(byte[] request);
  public byte[] handleResponse(byte[] response);
}
