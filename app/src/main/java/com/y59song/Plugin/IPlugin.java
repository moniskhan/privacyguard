package com.y59song.Plugin;

import android.content.Context;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
  // May modify the content of the request and response
  public String handleRequest(String request);
  public String handleResponse(String response);
  public String modifyRequest(String request);
  public String modifyResponse(String response);
  public void setContext(Context context);
}
