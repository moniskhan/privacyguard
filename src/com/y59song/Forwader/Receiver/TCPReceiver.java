package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private ArrayDeque<byte[]> responses;
  private int current = 1, lastAck = 0;

  private final int maxlength = 2500;
  private ByteBuffer msg = ByteBuffer.allocate(maxlength);

  public TCPReceiver(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
    this.forwarder = forwarder;
    this.selector = selector;
    responses = new ArrayDeque<byte[]>();
  }

  @Override
  public void run() {
    while(!forwarder.isClosed() && selector.isOpen()) {
      try {
        if(selector.select() == 0) break;
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "Selected");
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while(iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(!key.isValid()) continue;
        else if(key.isReadable()) {
          try {
            Log.d(TAG, "Readable");
            msg.clear();
            int length = socketChannel.read(msg);
            if(length < 0) {
              forwarder.close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            if(lastAck == current) {
              forwarder.receive(temp);
              current += temp.length;
            } else {
              synchronized (responses) {
                responses.add(temp);
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    Log.d(TAG, "Thread exit");
  }

  public void fetch(int ack) {
    byte[] toSend;
    lastAck = ack;
    synchronized(responses) {
      if(responses.isEmpty()) return;
      else toSend = responses.remove();
    }
    forwarder.receive(toSend);
    current += toSend.length;
  }
}
