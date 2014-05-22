package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Forwader.AbsForwarder;
import com.y59song.Forwader.ForwarderBuilder;
import com.y59song.Network.IP.IPDatagram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = "MyVpnService";
  private Thread mThread;
  public static HashMap<Integer, AbsForwarder> portToForwarder;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private FileInputStream localIn;
  private FileOutputStream localOut;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mThread != null) mThread.interrupt();
    mThread = new Thread(this, getClass().getSimpleName());
    mThread.start();
    portToForwarder = new HashMap<Integer, AbsForwarder>();
    return 0;
  }

  @Override
  public void run() {
    configure();
    localIn = new FileInputStream(mInterface.getFileDescriptor());
    localOut = new FileOutputStream(mInterface.getFileDescriptor());
    ByteBuffer packet = ByteBuffer.allocate(32767);

    try {
      while (mInterface != null && mInterface.getFileDescriptor() != null && mInterface.getFileDescriptor().valid()) {
        packet.clear();
        int length = localIn.read(packet.array());
        if(length > 0) {
          packet.limit(length);
          final IPDatagram ip = IPDatagram.create(packet);
          packet.clear();
          if(ip == null) continue;
          int port = ip.payLoad().getSrcPort();
          Log.d(TAG, "Port : " + ip.payLoad().getDstPort());
          AbsForwarder temp;
          if(!portToForwarder.containsKey(port)) {
            temp = ForwarderBuilder.build(ip.header().protocol(), this);
            //temp.start();
            portToForwarder.put(port, temp);
          } else temp = portToForwarder.get(port);
          temp.request(ip);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void fetchResponse(byte[] response) {
    if(localOut == null || response == null) return;
    try {
      localOut.write(response);
      localOut.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private InetAddress getLocalAddress() {
    try {
      for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface netInterface = en.nextElement();
        for(Enumeration<InetAddress> enumIpAddr = netInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if(!inetAddress.isLoopbackAddress()) {
            return inetAddress;
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void configure() {
    Builder b = new Builder();
    //b.addAddress("10.0.0.0", 28);
    b.addAddress(getLocalAddress(), 28);
    //b.addRoute("0.0.0.0", 0);
    //b.addRoute("8.8.8.8", 32);
    b.addDnsServer("8.8.8.8");
    b.addRoute("123.125.114.0", 24);
    b.addRoute("173.194.43.116", 32);
    //b.addRoute("71.19.173.0", 24);
    b.setMtu(1500);
    mInterface = b.establish();
  }
}
