/*
 * Thread for writing response to the virtual interface
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
import com.y59song.Utilities.ByteOperations;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;

/**
 * Created by y59song on 02/06/14.
 */
public class TunWriteThread extends Thread {
  private final String TAG = TunWriteThread.class.getSimpleName();
  private final boolean DEBUG = false;
  private final FileOutputStream localOut;
  private final ArrayDeque<byte[]> writeQueue = new ArrayDeque<byte[]>();
  private final MyVpnService vpnService;

  public TunWriteThread(FileDescriptor fd, MyVpnService vpnService) {
    localOut = new FileOutputStream(fd);
    this.vpnService = vpnService;
  }

  public void run() {
    byte[] temp;
    while(true) {
      synchronized(writeQueue) {
        while ((temp = writeQueue.pollFirst()) == null) {
          try {
            writeQueue.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if (isInterrupted()) {
            try {
              clean();
            } catch (IOException e) {
              e.printStackTrace();
            }
            return;
          } else continue;
        }
      }
      try {
        localOut.write(temp);
        localOut.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void write(byte[] data) {
    synchronized(writeQueue) {
      writeQueue.addLast(data);
      if(writeQueue.size() == 1)
        writeQueue.notify();
    }

  }

  private void clean() throws IOException {
    localOut.close();
    writeQueue.clear();
  }
}
