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

/**
 * Created by y59song on 03/04/14.
 */
public class TCPForwarderWorker extends Thread {
  private final String TAG = "TCPForwarderWorker";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private final int limit = 1200;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private ArrayDeque<byte[]> requests = new ArrayDeque<byte[]>();
  private Sender sender;

  public TCPForwarderWorker(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port, TCPForwarder forwarder) {
    this.forwarder = forwarder;
    try {
      socketChannel = SocketChannel.open();
      Socket socket = socketChannel.socket();
      socket.setReuseAddress(true);
      MyLogger.debugInfo(TAG, srcAddress.getHostAddress() + ":" + src_port + " " + LocalServer.port);
      MyLogger.debugInfo(TAG, dstAddress.getHostAddress() + ":" + dst_port);
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      try {
        socketChannel.connect(new InetSocketAddress(LocalServer.port));
        while(!socketChannel.finishConnect()) ;
        MyLogger.debugInfo(TAG, "Connected");
      } catch (ConnectException e) {
        MyLogger.debugInfo(TAG, "Connect exception !!! : " + srcAddress.getHostAddress() + ":" + src_port + " " + LocalServer.port);
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
    synchronized (requests) {
      requests.addLast(request);
      requests.notify();
    }
  }

  public class Sender extends Thread {
    public void run() {
      try {
        byte[] temp;
        while(!isInterrupted() && !socketChannel.socket().isClosed()) {
          synchronized(requests) {
            while((temp = requests.pollFirst()) == null) {
              requests.wait();
            }
          }
          ByteBuffer tempBuf = ByteBuffer.wrap(temp);
          while(true) {
            LocationGuard.tcpForwarderWorkerWrite += socketChannel.write(tempBuf);
            MyLogger.debugInfo(TAG, "Write " + LocationGuard.tcpForwarderWorkerWrite);
            if(tempBuf.hasRemaining()) {
              MyLogger.debugInfo(TAG, "looping");
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
    int total = 0;
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
              MyLogger.debugInfo("TCPForwarderWorker", "Length from socket channel is " + length + " : " + socketChannel.socket().getPort());
              close();
              return;
            }
            MyLogger.debugInfo("TCPForwarderWorker", "" + socketChannel.socket().getLocalPort() + ":" + socketChannel.socket().getPort() + " " + total + " : " + length);
            total += length;
            MyLogger.debugInfo("TCPForwarderWorker", "" + socketChannel.socket().getLocalPort() + ":" + socketChannel.socket().getPort() + " " + total + " : " + length);
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            LocationGuard.tcpForwarderWorkerRead += length;
            MyLogger.debugInfo("TCPForwarderWorker", "Read " + LocationGuard.tcpForwarderWorkerRead + ":" + LocationGuard.socketForwarderRead);
            forwarder.forwardResponse(temp);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        /*
         else if(key.isWritable()) {
          byte[] temp;
          synchronized (requests) {
            if ((temp = requests.pollFirst()) == null) {
              try {
                MyLogger.debugInfo("TCPForwarderWorker", "Waiting");
                requests.wait(100);
              } catch (InterruptedException e) {
                e.printStackTrace();
                close();
                return;
              }
              continue;
            }
          }
          try {
            MyLogger.debugInfo("TCPForwarderWorker", "Writing");
            socketChannel.write(ByteBuffer.wrap(temp));
          } catch (IOException e) {
            e.printStackTrace();
            close();
            return;
          }
        }
         */
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