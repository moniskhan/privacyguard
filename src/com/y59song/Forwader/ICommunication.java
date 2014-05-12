package com.y59song.Forwader;

import com.y59song.Network.IPPayLoad;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-27.
 */
public interface ICommunication {
  public void setup(InetAddress dstAddress, int dstPort);
  public void send(IPPayLoad payload);
  public void close();
}
