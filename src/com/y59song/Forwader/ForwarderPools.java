package com.y59song.Forwader;

import android.util.Log;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Utilities.Pool.IPool;
import com.y59song.Utilities.RealPool.BoundedBlockingPool;
import com.y59song.Utilities.RealPool.ForwarderValidator;
import com.y59song.Utilities.RealPool.TCPForwarderFactory;
import com.y59song.Utilities.RealPool.UDPForwarderFactory;

import java.util.Collections;
import java.util.HashMap;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderPools {
  private IPool<UDPForwarder> udpForwarderPool;
  private IPool<TCPForwarder> tcpForwarderPool;
  private static final int udpPoolSize = 50;
  private static final int tcpPoolSize = 100;
  private int cu = 0, ct = 0;
  private HashMap<Integer, AbsForwarder> portToForwarder;

  public ForwarderPools(MyVpnService vpnService) {
    udpForwarderPool = new BoundedBlockingPool<UDPForwarder>(
      udpPoolSize,
      new ForwarderValidator(),
      new UDPForwarderFactory(vpnService));
    tcpForwarderPool = new BoundedBlockingPool<TCPForwarder>(
      tcpPoolSize,
      new ForwarderValidator(),
      new TCPForwarderFactory(vpnService));
    portToForwarder = new HashMap<Integer, AbsForwarder>();
  }

  public AbsForwarder get(int port, byte protocol) {
    Log.d("ForwarderPools", "" + cu + ", " + ct);
    if(portToForwarder.containsKey(port) && !portToForwarder.get(port).isClosed())
      return portToForwarder.get(port);
    else {
      AbsForwarder temp = getByProtocol(protocol);
      temp.open();
      portToForwarder.put(port, temp);
      return temp;
    }
  }

  private AbsForwarder getByProtocol(byte protocol) {
    switch(protocol) {
      case IPDatagram.TCP : ct ++; TCPForwarder temp = tcpForwarderPool.get(); new Thread(temp).start(); return temp;
      case IPDatagram.UDP : cu ++; return udpForwarderPool.get();
      default:
        return null;
    }
  }

  public void release(UDPForwarder udpForwarder) {
    cu --;
    udpForwarderPool.release(udpForwarder);
    portToForwarder.values().removeAll(Collections.singleton(udpForwarder));
  }

  public void release(TCPForwarder tcpForwarder) {
    ct --;
    tcpForwarderPool.release(tcpForwarder);
    portToForwarder.values().removeAll(Collections.singleton(tcpForwarder));
  }
}
