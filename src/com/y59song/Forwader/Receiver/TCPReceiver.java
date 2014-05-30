package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private ConcurrentLinkedQueue<byte[]> responses = new ConcurrentLinkedQueue<byte[]>();
  private int lastAck = 1, start = 1, seq = 1, counter = 0;
  private final int maxlength = 1292, maxCounter = 2;
  private ByteBuffer msg = ByteBuffer.allocate(maxlength);

  public TCPReceiver(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
    this.forwarder = forwarder;
    this.selector = selector;
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
        if(key.isValid() && key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length < 0) {
              forwarder.close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            responses.add(temp);
            Log.d(TAG, "" + seq + " , " + lastAck + " , " + length);
            if(seq == lastAck) {
              fetch(seq);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    Log.d(TAG, "Thread exit");
  }


  public synchronized void fetch(int ack) {
    Log.d(TAG, "ACK : " + ack + ", " + start);
    if(ack == lastAck && seq > ack) counter ++;
    if(counter > maxCounter) { // lost too much, just leave them
      counter = 0;
      while(seq > ack && !responses.isEmpty()) {
        seq -= responses.element().length;
        responses.remove();
      }
    }
    lastAck = ack;
    seq = lastAck;
    while(start < ack && !responses.isEmpty()) {
      start += responses.element().length;
      responses.remove();
    }
    if(start < ack) {
      Log.e(TAG, "ERROR : " + ack + ", " + start);
      return;
    }
    assert(start == ack);
    if(!responses.isEmpty()) {
      seq += responses.element().length;
      forwarder.receive(responses.element());
    }
  }
}
