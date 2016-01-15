package com.y59song.Forwader.Receiver;

import android.text.Selection;
import android.util.Log;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.LocationGuard.LocationGuard;
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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPForwarderWorker extends Thread {
  private final String TAG = "TCPForwarderWorker";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private final int limit = 1368;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  //private ArrayDeque<byte[]> requests = new ArrayDeque<byte[]>();
  private ConcurrentLinkedQueue<byte[]> requests = new ConcurrentLinkedQueue<byte[]>();
  private Sender sender;

  public TCPForwarderWorker(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port, TCPForwarder forwarder) {
    this.forwarder = forwarder;
    try {
      socketChannel = SocketChannel.open();
      Socket socket = socketChannel.socket();
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      try {
        socketChannel.connect(new InetSocketAddress(LocalServer.port));
        while(!socketChannel.finishConnect()) ;
      } catch (ConnectException e) {
        e.printStackTrace();
        return;
      }
      socketChannel.configureBlocking(false);
      selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isValid() {
    return selector != null;
  }

  public void send(byte[] request) {
    requests.offer(request);
  }

  public class Sender extends Thread {
    public void run() {
      try {
        byte[] temp;
        while(!isInterrupted() && !socketChannel.socket().isClosed()) {
          while((temp = requests.poll()) == null) {
            Thread.sleep(10);
          }
          ByteBuffer tempBuf = ByteBuffer.wrap(temp);
          while(true) {
            LocationGuard.tcpForwarderWorkerWrite += socketChannel.write(tempBuf);
            if(tempBuf.hasRemaining()) {
              Thread.sleep(10);
            } else break;
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      } catch (IOException e) {
        e.printStackTrace();
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
        if(!key.isValid()) continue;
        else if(key.isReadable()) {
          try {
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0 || isInterrupted()) {
              close();
              return;
            }
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            LocationGuard.tcpForwarderWorkerRead += length;
            forwarder.forwardResponse(temp);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    close();
  }

  public void close() {
    try {
      if(selector != null) selector.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if(sender != null && sender.isAlive()) {
      sender.interrupt();
    }
    try {
      if(socketChannel.isConnected()) {
        socketChannel.socket().close();
        socketChannel.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}