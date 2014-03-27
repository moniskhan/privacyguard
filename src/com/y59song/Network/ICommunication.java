package com.y59song.Network;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by frank on 2014-03-27.
 */
public interface ICommunication {
  public Socket connect(InetAddress dstAddress, int port);
  public void send(InetAddress dstAddress, int port);
  public byte[] receive();
}
