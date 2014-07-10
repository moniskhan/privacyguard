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
  private final boolean DEBUG = false;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private ArrayDeque<byte[]> requests = new ArrayDeque<byte[]>();
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
    private final String TAG = "TCPSender";
    public void run() {
      if(DEBUG) Log.d(TAG, "Sender start");
      byte[] temp;
      while (!forwarder.isClosed() && !socketChannel.isConnected()) {
        try {
          if(DEBUG) Log.d(TAG, "Gonna sleep");
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
          if(DEBUG) Log.d(TAG, "Sender stop");
          return;
        }
      }
      while (!forwarder.isClosed() && socketChannel.isOpen()) {
        synchronized (requests) {
          while ((temp = requests.pollFirst()) == null) {
            try {
              requests.wait();
            } catch (InterruptedException e) {
              if(DEBUG) Log.d(TAG, "Sender stop");
              return;
            }
            if (isInterrupted()) {
              if(DEBUG) Log.d(TAG, "Sender stop");
              return;
            } else continue;
          }
        }
        try {
          socketChannel.write(ByteBuffer.wrap(temp));
        } catch (Exception e) {
          break;
        }
      }
      if(DEBUG) Log.d(TAG, "Sender stop");
    }
  }

  @Override
  public void run() {
    if(DEBUG) Log.d(TAG, "Receiver start");
    sender = new Sender();
    sender.start();
    while(!forwarder.isClosed() && selector.isOpen()) {
      try {
        selector.select(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      if(DEBUG) Log.d(TAG, "selected");
      while(iterator.hasNext() && !forwarder.isClosed()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(key.isValid() && key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(DEBUG) Log.d(TAG, "selected read : " + length + " " + forwarder.debugInfo());
            if(length <= 0) {
              sender.interrupt();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            forwarder.receive(temp);
          } catch (IOException e) {
            Log.d(TAG, socketChannel.socket().getLocalPort() + " " + socketChannel.socket().getPort());
            e.printStackTrace();
          }
        }
      }
    }
    try {
      selector.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if(DEBUG) Log.d(TAG, "Receiver stop");
    sender.interrupt();
  }

  public void close() {
    if(sender != null && sender.isAlive()) sender.interrupt();
  }
}