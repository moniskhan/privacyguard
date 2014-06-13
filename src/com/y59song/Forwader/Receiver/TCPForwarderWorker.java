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
public class TCPForwarderWorker extends Thread {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private final int limit = 2048;
  private Integer lastAck;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private ArrayDeque<byte[]> requests = new ArrayDeque<byte[]>();
  private ArrayDeque<byte[]> responses = new ArrayDeque<byte[]>();
  private Sender sender;

  public TCPForwarderWorker(SocketChannel socketChannel, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socketChannel;
    this.forwarder = forwarder;
    this.selector = selector;
  }

  public void send(byte[] request) {
    synchronized (requests) {
      requests.addLast(request);
      if(requests.size() == 1) requests.notify();
    }
  }

  public class Sender extends Thread {
    public void run() {
      byte[] temp;
      while (!socketChannel.isConnected()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      while (socketChannel.isOpen()) {
        synchronized (requests) {
          while ((temp = requests.pollFirst()) == null) {
            try {
              requests.wait();
            } catch (InterruptedException e) {
              return;
            }
            if (isInterrupted()) {
              return;
            } else continue;
          }
        }
        try {
          socketChannel.write(ByteBuffer.wrap(temp));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void run() {
    sender = new Sender();
    sender.start();
    while(!forwarder.isClosed()) {
      try {
        selector.select(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
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
            forwarder.receive(temp);
          } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "" + socketChannel.socket().getLocalPort());
          }
        }
      }
    }
    sender.interrupt();
  }

  /*
  public void setLastAck(int ack) {
    synchronized(lastAck) {
      if(lastAck < ack) {
        synchronized(responses) {

        }
      }
      lastAck = ack;
    }
  }
  */
}