package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Network.Forwarder;
import com.y59song.Network.IPDatagram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = "MyVpnService";
  private Thread mThread;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private FileInputStream localIn;
  private FileOutputStream localOut;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mThread != null) mThread.interrupt();
    mThread = new Thread(this, getClass().getSimpleName());
    mThread.start();
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
        // Read from the interface
        int length = localIn.read(packet.array());
        if(length > 0) {
          Log.d(TAG, "*** new packet *** length : " + length);
          packet.limit(length);
          final IPDatagram ip = IPDatagram.create(packet);
          if(ip == null) continue;
          // Send to the appropriate destination
          // Get the response
          // Write to the interface
          new Thread(new Forwarder(ip, this), "Forwarder_Thread").start();
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private InetAddress getLocalAddress() {
    try {
      for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          Log.d(TAG, "**** INET address *****");
          Log.d(TAG, "address : " + inetAddress.getHostAddress());
          Log.d(TAG, "interface name : " + intf.getDisplayName());
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
    b.addRoute("0.0.0.0", 0);
    b.setMtu(1500);
    mInterface = b.establish();
  }
}
