package com.y59song.Forwader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IPPayLoad;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-29.
 */
public abstract class AbsForwarder extends Thread {
  protected InetAddress dstAddress;
  protected int dstPort;
  protected MyVpnService vpnService;
  protected Handler mHandler;
  public AbsForwarder(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }
  @Override
  public synchronized void run() {
    Looper.prepare();
    mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        process((IPDatagram)msg.obj);
      }
    };
    Looper.loop();
  }

  protected abstract void process(IPDatagram ip);

  public Handler getHandler() { return mHandler; }

  public void send(IPDatagram ip) {
    if(mHandler == null) return;
    Message msg = Message.obtain();
    msg.obj = ip;
    mHandler.sendMessage(msg);
  }

  protected void forwardResponse(IPHeader ipHeader, IPPayLoad datagram) {
    datagram.update(ipHeader); // set the checksum
    IPDatagram newIpDatagram = new IPDatagram(ipHeader, datagram); // set the ip datagram, will update the length and the checksum
    vpnService.fetchResponse(newIpDatagram.toByteArray());
  }
}
