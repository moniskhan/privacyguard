package com.y59song.Forwader.Receiver;

import android.text.Selection;
import android.util.Log;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.LocalServer;
import com.y59song.Utilities.MyLogger;

import java.io.IOException;
import java.net.*;
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

  public TCPForwarderWorker(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port, TCPForwarder forwarder) {
    this.forwarder = forwarder;
    try {
      socketChannel = SocketChannel.open();
      Socket socket = socketChannel.socket();
      socket.setReuseAddress(true);
      MyLogger.debugInfo(TAG, srcAddress.getHostAddress() + ":" + src_port + " " + LocalServer.port);
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      try {
        socketChannel.connect(new InetSocketAddress(LocalServer.port));
      } catch (ConnectException e) {
        MyLogger.debugInfo(TAG, "Connect exception !!! : " + srcAddress.getHostAddress() + ":" + src_port + " " + LocalServer.port);
        e.printStackTrace();
        return;
      }
      socketChannel.configureBlocking(false);
      selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isValid() {
    return selector != null;
  }

  public void send(byte[] request) {
    synchronized (requests) {
      requests.addLast(request);
      if(requests.size() == 1) requests.notify();
    }
  }

  @Override
  public void run() {
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
        if(!key.isValid()) continue;
        else if(key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0 || isInterrupted()) {
              MyLogger.debugInfo("TCPForwarderWorker", "Length from socket channel is " + length + " : " + socketChannel.socket().getPort());
              close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            forwarder.forwardResponse(temp);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else if(key.isWritable()) {
          byte[] temp;
          synchronized (requests) {
            if ((temp = requests.pollFirst()) == null) {
              try {
                requests.wait(10);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              continue;
            }
          }
          try {
            socketChannel.write(ByteBuffer.wrap(temp));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    close();
  }

  public void close() {
    MyLogger.debugInfo(TAG, "Receiver stop " + socketChannel.socket().getLocalPort());
    try {
      if(selector != null) selector.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      if(socketChannel.isConnected()) {
        socketChannel.socket().close();
        socketChannel.close();
        socketChannel = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}