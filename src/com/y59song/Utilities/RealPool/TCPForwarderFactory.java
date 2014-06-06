package com.y59song.Utilities.RealPool;

import com.y59song.Forwader.TCPForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.Pool.ObjectFactory;

/**
 * Created by y59song on 28/05/14.
 */
public class TCPForwarderFactory implements ObjectFactory<TCPForwarder> {
  private MyVpnService vpnService;
  public TCPForwarderFactory(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }
  @Override
  public TCPForwarder createNew() {
    return new TCPForwarder(vpnService);
  }
}
