package com.y59song.Forwader.Receiver;

import com.y59song.Forwader.TCPForwarder_old;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPSender implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder_old forwarder;
  private LinkedList<byte[]> request;
  private final int limit = 2048;
  private ByteBuffer msg = ByteBuffer.allocate(limit);

  public TCPSender(Socket socket, TCPForwarder_old forwarder, Selector selector) {
    this.socketChannel = socket.getChannel();
    this.forwarder = forwarder;
    this.selector = selector;
    request = new LinkedList<byte[]>();
  }

  public void send(byte[] data) {
    synchronized(request) {
      request.add(data);
    }
  }

  @Override
  public void run() {
    while(true) {
      try {
        selector.select(0);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while(iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(key.isValid() && key.isWritable()) {
          try {
            synchronized(request) {
              if(request.isEmpty()) {
                break;
              }
              socketChannel.write(ByteBuffer.wrap(request.remove()));
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      if(iterator.hasNext()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
