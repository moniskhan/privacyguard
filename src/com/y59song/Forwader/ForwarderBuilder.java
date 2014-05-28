package com.y59song.Forwader;

import android.util.Log;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderBuilder {
  private static int count = 0;
  public static AbsForwarder build(byte protocol, MyVpnService myVpnService) {
    count ++;
    Log.d("ForwarderBuilder", "" + count);
    switch(protocol) {
      case IPDatagram.TCP : return new TCPForwarder(myVpnService);
      case IPDatagram.UDP : return new UDPForwarder(myVpnService);
      default:
        return null;
    }
  }
}
