package com.y59song.LocationGuard;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * Created by y59song on 02/06/14.
 */
public class TunWriteThread extends Thread {
  private final FileOutputStream localOut;
  private final ArrayDeque<byte[]> writeQueue = new ArrayDeque<byte[]>();

  public TunWriteThread(FileDescriptor fd) {
    localOut = new FileOutputStream(fd);
  }

  public void run() {
    while(true) {
      synchronized(writeQueue) {
        Log.d("Write", "Begin of Loop");
        byte[] temp;
        while((temp = writeQueue.pollFirst()) == null) {
          try {
            writeQueue.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          Log.d("Write", "Notified");
          if(isInterrupted()) {
            try {
              localOut.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
            return;
          } else continue;
        }
        Log.d("Write", "poll first");
        try {
          Log.d("WriteThread", "Length : " + temp.length);
          //localOut.write(temp, 0, temp.length);
          localOut.getChannel().write(ByteBuffer.wrap(temp));
          localOut.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void write(byte[] data) {
    synchronized(writeQueue) {
      writeQueue.addLast(data);
      writeQueue.notify();
    }
  }
}
