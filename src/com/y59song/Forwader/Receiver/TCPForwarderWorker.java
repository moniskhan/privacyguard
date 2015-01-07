package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.Network.LocalServer;
import com.y59song.Utilities.MyLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private ArrayDeque<byte[]> requests = new ArrayDeque<byte[]>();
  private Sender sender;

  public TCPForwarderWorker(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port, TCPForwarder forwarder) {
    this.forwarder = forwarder;
    try {
      if(socketChannel == null) socketChannel = SocketChannel.open();
      Socket socket = socketChannel.socket();
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      socketChannel.connect(new InetSocketAddress(LocalServer.port));
      socketChannel.configureBlocking(false);
      selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
    } catch (IOException e) {
      try {
        Log.d(TAG, InetAddress.getLocalHost().getHostAddress() + ":" + src_port);
      } catch (UnknownHostException e1) {
        e1.printStackTrace();
      }
      e.printStackTrace();
    }
  }

  public void send(byte[] request) {
    synchronized (requests) {
      requests.addLast(request);
      if(requests.size() == 1) requests.notify();
    }
  }

  public class Sender extends Thread {
    public void run() {
      try {
        byte[] temp;
        while(!isInterrupted() && !socketChannel.isConnected()) {
          Thread.sleep(100);
        }
        while (!isInterrupted() && socketChannel.isOpen()) {
          synchronized (requests) {
            if ((temp = requests.pollFirst()) == null) {
              requests.wait();
              continue;
            }
          }
          try {
            socketChannel.write(ByteBuffer.wrap(temp));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  @Override
  public void run() {
    sender = new Sender();
    sender.start();
    while(!isInterrupted() && selector.isOpen()) {
      try {
        selector.select(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while(!isInterrupted() && iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(key.isValid() && key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0 || isInterrupted()) {
              MyLogger.debugInfo("TCPForwarderWorker", "Length from socket channel is " + length);
              close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            forwarder.forwardResponse(temp);
          } catch (IOException e) {
            Log.d(TAG, socketChannel.socket().getLocalPort() + " " + socketChannel.socket().getPort());
            e.printStackTrace();
          }
        }
      }
    }
    close();
  }

  public void close() {
    try {
      selector.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      socketChannel.socket().close();
      socketChannel.close();
      socketChannel = null;
    } catch (IOException e) {
      e.printStackTrace();
    }
    MyLogger.debugInfo(TAG, "Receiver stop");
    if(sender != null && sender.isAlive()) {
      try {
        Thread.interrupted();
        sender.interrupt();
        sender.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}