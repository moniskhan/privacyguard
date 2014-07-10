package com.y59song.Plugin;

import android.content.Context;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
  // May modify the content of the request and response
  public boolean handleRequest(String request);
  public boolean handleResponse(String response);
  public byte[] modifyRequest(byte[] request);
  public byte[] modifyResponse(byte[] response);
  public void setContext(Context context);
}
