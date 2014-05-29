package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private LinkedList<ByteBuffer> responses = new LinkedList<ByteBuffer>();
  private int count = 0, lastAck = 1, start = 1, seq = 1;
  private final int limit = 1368, maxlength = 1292;
  private ByteBuffer msg = ByteBuffer.allocate(maxlength);

  public TCPReceiver(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
    this.forwarder = forwarder;
    this.selector = selector;
  }

  @Override
  public void run() {
    /*
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(!forwarder.isClosed()) {
          send();
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
    */
    while(!forwarder.isClosed()) {
      try {
        selector.select();
      } catch (IOException e) {
        e.printStackTrace();
      }
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while(iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(key.isValid() && key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0) continue;
            msg.flip();

            //responses.add(msg.duplicate());
            count += msg.limit();
            Log.d(TAG, "" + length + ", " + count);

            ///*
            Log.d(TAG, "" + msg.remaining() + ", " + length);
            byte[] temp = new byte[length];
            msg.get(temp);
            forwarder.receive(temp);
            Thread.sleep(100);
            //*/
            /*
            if(seq == lastAck) {
              fetch(lastAck);
            }
            */
          } catch (IOException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    Log.d(TAG, "Thread exit");
  }

  public void send() {
    if(responses.isEmpty()) return;
    byte[] temp = new byte[Math.min(responses.element().remaining(), limit)];
    responses.element().get(temp);
    if(responses.element().remaining() == 0) responses.remove();
    forwarder.receive(temp);
    count -= temp.length;
    Log.d("TCP", "Remain : " + count + "," + responses.size());
  }

  public void clear(int ack) {
    start = ack;
    lastAck = ack;
    seq = ack;
    responses.clear();
  }

  public void fetch(int ack) {
    Log.d(TAG, "ACK : " + ack + ", " + start);
    lastAck = ack;
    seq = lastAck;
    while(start < ack && !responses.isEmpty()) {
      start += responses.element().limit();
      responses.remove();
    }
    if(start < ack) {
      Log.e(TAG, "ERROR : " + ack + ", " + start);
      return;
    }
    assert(start == ack);
    if(!responses.isEmpty()) {
      responses.element().position(0);
      byte[] temp = new byte[responses.element().limit()];
      seq += temp.length;
      responses.element().get(temp);
      forwarder.receive(temp);
    }
  }
}
