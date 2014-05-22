package com.y59song.Forwader;

import android.os.Handler;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IP.IPPayLoad;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-29.
 */
public abstract class AbsForwarder {//} extends Thread {
  public static final int FORWARD = 0, RECEIVE = 1;
  protected InetAddress dstAddress;
  protected int dstPort;
  protected MyVpnService vpnService;
  protected Handler mHandler;
  public AbsForwarder(MyVpnService vpnService) {
    this.vpnService = vpnService;
  }

  /*
  @Override
  public synchronized void run() {
    Looper.prepare();
    mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        switch(msg.what) {
          case FORWARD: forward((IPDatagram) msg.obj); break;
          //case RECEIVE: receive((ByteBuffer) msg.obj); break;
          default: break;
        }
      }
    };
    Looper.loop();
  }
  */

  protected abstract void forward (IPDatagram ip);

  protected abstract void receive (byte[] response);

  public void request(IPDatagram ip) {
    forward(ip);
/*
    if(mHandler == null) return;
    Message msg = Message.obtain();
    msg.what = FORWARD;
    msg.obj = ip;
    mHandler.sendMessage(msg);
*/
  }

  /*
  public void respond(ByteBuffer response) {
    if(mHandler == null) return;
    Message msg = Message.obtain();
    msg.what = RECEIVE;
    msg.obj = response;
    mHandler.sendMessage(msg);
  }
  */

  public int forwardResponse(IPHeader ipHeader, IPPayLoad datagram) {
    if(ipHeader == null || datagram == null) return 0;
    datagram.update(ipHeader); // set the checksum
    IPDatagram newIpDatagram = new IPDatagram(ipHeader, datagram); // set the ip datagram, will update the length and the checksum
    vpnService.fetchResponse(newIpDatagram.toByteArray());
    return datagram.virtualLength();
  }
}
