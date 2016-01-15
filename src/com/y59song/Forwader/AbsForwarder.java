/*
 * Abstract class for all forwarders
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
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IP.IPPayLoad;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-29.
 */

public abstract class AbsForwarder {
  protected MyVpnService vpnService;
  protected boolean closed = true;
  public AbsForwarder(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }

  public abstract boolean setup(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort);

  public void open() {
    closed = false;
  }

  public void close() {
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }



  public abstract void forwardRequest(IPDatagram ip);

  public abstract void forwardResponse(byte[] response);

  public int forwardResponse(IPHeader ipHeader, IPPayLoad datagram) {
    if(ipHeader == null || datagram == null) return 0;
    datagram.update(ipHeader); // set the checksum
    IPDatagram newIpDatagram = new IPDatagram(ipHeader, datagram); // set the ip datagram, will update the length and the checksum
    vpnService.fetchResponse(newIpDatagram.toByteArray());
    return datagram.virtualLength();
  }

}
