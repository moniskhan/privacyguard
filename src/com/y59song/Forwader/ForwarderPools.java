/*
 * Pool for all forwarders
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Utilities.RealPool.BoundedBlockingPool;

import java.util.Collections;
import java.util.HashMap;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderPools {
  private final String TAG = ForwarderPools.class.getSimpleName();
  private BoundedBlockingPool<UDPForwarder> udpForwarderPool;
  private BoundedBlockingPool<TCPForwarder> tcpForwarderPool;
  private static final int udpPoolSize = 50;
  private static final int tcpPoolSize = 100;
  private HashMap<Integer, AbsForwarder> portToForwarder;
  private MyVpnService vpnService;

  public ForwarderPools(MyVpnService vpnService) {
    this.vpnService = vpnService;

    /*
    udpForwarderPool = new BoundedBlockingPool<UDPForwarder>(
      udpPoolSize,
      new ForwarderValidator(),
      new UDPForwarderFactory(vpnService));
    tcpForwarderPool = new BoundedBlockingPool<TCPForwarder>(
      tcpPoolSize,
      new ForwarderValidator(),
      new TCPForwarderFactory(vpnService));
      */
    portToForwarder = new HashMap<Integer, AbsForwarder>();
  }

  public AbsForwarder get(int port, byte protocol) {
    //Log.d(TAG, "GET : " + udpForwarderPool.getSize() + ", " + tcpForwarderPool.getSize());
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
    /*
    switch(protocol) {
      case IPDatagram.TCP : TCPForwarder temp = tcpForwarderPool.get(); new Thread(temp).start(); return temp;
      case IPDatagram.UDP : return udpForwarderPool.get();
      default:
        return null;
    }
    */
    switch(protocol) {
      //case IPDatagram.TCP : TCPForwarder temp = new TCPForwarder(vpnService); new Thread(temp).start(); return temp;
      case IPDatagram.TCP : return new TCPForwarder(vpnService);
      case IPDatagram.UDP : return new UDPForwarder(vpnService);
      default: return null;
    }
  }

  public void release(UDPForwarder udpForwarder) {
    //Log.d(TAG, "Release : " + udpForwarderPool.getSize() + ", " + tcpForwarderPool.getSize());
    //udpForwarderPool.release(udpForwarder);
    portToForwarder.values().removeAll(Collections.singleton(udpForwarder));
  }

  public void release(TCPForwarder tcpForwarder) {
    //Log.d(TAG, "Release : " + udpForwarderPool.getSize() + ", " + tcpForwarderPool.getSize());
    //tcpForwarderPool.release(tcpForwarder);
    portToForwarder.values().removeAll(Collections.singleton(tcpForwarder));
  }
}
