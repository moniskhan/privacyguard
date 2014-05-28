package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IP.IPPayLoad;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-29.
 */
public abstract class AbsForwarder {//} extends Thread {
  protected InetAddress dstAddress;
  protected int dstPort;
  protected MyVpnService vpnService;
  protected boolean closed;
  public AbsForwarder(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }

  protected abstract void forward (IPDatagram ip);

  protected abstract void receive (byte[] response);

  public void request(IPDatagram ip) {
    forward(ip);
  }

  public int forwardResponse(IPHeader ipHeader, IPPayLoad datagram) {
    if(ipHeader == null || datagram == null) return 0;
    datagram.update(ipHeader); // set the checksum
    IPDatagram newIpDatagram = new IPDatagram(ipHeader, datagram); // set the ip datagram, will update the length and the checksum
    vpnService.fetchResponse(newIpDatagram.toByteArray());
    return datagram.virtualLength();
  }

  public abstract void close();

  public boolean isClosed() {
    return closed;
  }
}
