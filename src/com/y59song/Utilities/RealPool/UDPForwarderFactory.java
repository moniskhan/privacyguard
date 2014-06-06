package com.y59song.Utilities.RealPool;

import com.y59song.Forwader.UDPForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.Pool.ObjectFactory;

/**
 * Created by y59song on 28/05/14.
 */
public class UDPForwarderFactory implements ObjectFactory<UDPForwarder> {
  private MyVpnService vpnService;
  public UDPForwarderFactory(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }
  @Override
  public UDPForwarder createNew() {
    return new UDPForwarder(vpnService);
  }
}
