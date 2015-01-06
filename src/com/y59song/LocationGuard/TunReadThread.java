/*
 * Thread for handling and dispatching all ip packets (only tcp and udp)
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
        packet.clear();
        length = localIn.getChannel().read(packet);
        if(length > 0) {
          packet.flip();
          final IPDatagram ip = IPDatagram.create(packet);
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
