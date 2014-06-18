package com.y59song.LocationGuard;

import android.util.Log;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Utilities.ByteOperations;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * Created by y59song on 06/06/14.
 */
public class TunReadThread extends Thread {
  private final String TAG = TunReadThread.class.getSimpleName();
  private final boolean DEBUG = false;
  private final FileInputStream localIn;
  private final int limit = 2048;
  private final MyVpnService vpnService;
  private final ArrayDeque<IPDatagram> readQueue = new ArrayDeque<IPDatagram>();
  private final ForwarderPools forwarderPools;
  private final Dispatcher dispatcher;

  public TunReadThread(FileDescriptor fd, MyVpnService vpnService) {
    localIn = new FileInputStream(fd);
    this.vpnService = vpnService;
    this.forwarderPools = new ForwarderPools(this.vpnService);
    dispatcher = new Dispatcher();
  }

  public void run() {
    try {
      ByteBuffer packet = ByteBuffer.allocate(limit);
      int length;
      dispatcher.start();
      while (!isInterrupted()) {
        //if(DEBUG) Log.d(TAG, "Receiving");
        packet.clear();
        length = localIn.getChannel().read(packet);
        if(length > 0) {
          packet.flip();
          if(DEBUG) Log.d(TAG, "Length : " + length);
          final IPDatagram ip = IPDatagram.create(packet);
          if(DEBUG) Log.d(TAG, ByteOperations.byteArrayToHexString(ip.toByteArray()));
          if(ip != null) {
            synchronized (readQueue) {
              readQueue.addLast(ip);
              readQueue.notify();
            }
          }
        } else Thread.sleep(100);
      }
      clean();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private class Dispatcher extends Thread {
    public void run() {
      IPDatagram temp;
      while(true) {
        synchronized (readQueue) {
          while ((temp = readQueue.pollFirst()) == null) {
            try {
              readQueue.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
        if (isInterrupted()) return;
        int port = temp.payLoad().getSrcPort();
        forwarderPools.get(port, temp.header().protocol()).request(temp);
      }
    }
  }

  private void clean() throws IOException {
    dispatcher.interrupt();
    localIn.close();
  }
}
