package com.y59song.LocationGuard;

import android.util.Log;
import com.y59song.Utilities.ByteOperations;

import java.io.*;
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
        if(DEBUG) Log.d(TAG, ByteOperations.byteArrayToHexString(temp));
        localOut.write(temp);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void write(byte[] data) {
    synchronized(writeQueue) {
      writeQueue.addLast(data);
      writeQueue.notify();
    }
  }

  private void clean() throws IOException {
    localOut.close();
    writeQueue.clear();
  }
}
