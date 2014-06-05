package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

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
public class TCPReceiver implements Runnable {
  private final String TAG = "TCPReceiver";
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private final int limit = 2048;
  private ByteBuffer msg = ByteBuffer.allocate(limit);
  private LinkedList<byte[]> request;

  public TCPReceiver(Socket socket, TCPForwarder forwarder, Selector selector) {
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
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "Selected");
      while(iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if(!key.isValid()) continue;
        else if(key.isReadable()) {
          try {
            Log.d(TAG, "Readable");
            msg.clear();
            int length = socketChannel.read(msg);
            if(length <= 0) continue;
            msg.flip();
            byte[] temp = new byte[length];
            msg.get(temp);
            forwarder.receive(temp);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
