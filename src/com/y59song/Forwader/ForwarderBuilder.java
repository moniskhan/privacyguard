package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderBuilder {
  public static AbsForwarder build(byte protocol, MyVpnService myVpnService) {
    switch(protocol) {
      case IPDatagram.TCP : return new TCPForwarder(myVpnService);
      case IPDatagram.UDP : return new UDPForwarder(myVpnService);
      default:
        return null;
    }
  }
}
