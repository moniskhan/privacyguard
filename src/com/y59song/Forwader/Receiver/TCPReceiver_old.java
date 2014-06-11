package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver_old extends Thread {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private final int limit = 2048, resend_duration = 1000, maxCount = 5;
  private int counter = 0;
  private ByteBuffer msg = ByteBuffer.allocate(limit);

  public TCPReceiver_old(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
    this.forwarder = forwarder;
    this.selector = selector;
  }

  @Override
  public void run() {
    while(!forwarder.isClosed()) {
      try {
        selector.select(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while(iterator.hasNext() && !forwarder.isClosed()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(key.isValid() && key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0) continue;
            msg.flip();
            Log.d(TAG, "" + msg.remaining() + ", " + length);
            byte[] temp = new byte[length];
            msg.get(temp);
            while(!forwarder.isClosed() && counter < maxCount) {
              forwarder.receive(temp);
              counter ++;
              Log.d(TAG, "Send : " + temp.length);
              try {
                Thread.sleep(resend_duration);
              } catch (InterruptedException e) {
                Log.d(TAG, "Interrupted");
                counter = 0;
                break;
              }
              Log.d(TAG, "Wake up");
              if(isInterrupted()) {
                counter = 0;
                break;
              }
              Log.d(TAG, "Not interrupted");
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}