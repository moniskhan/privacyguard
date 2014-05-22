package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.DataInputStream;
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
  private Socket socket;
  private SocketChannel socketChannel;
  private Selector selector;
  private TCPForwarder forwarder;
  private LinkedList<ByteBuffer> responses = new LinkedList<ByteBuffer>();
  private int count = 0;
  private final int limit = 2048;
  private ByteBuffer msg = ByteBuffer.allocate(limit);

  public TCPReceiver(Socket socket, TCPForwarder forwarder, Selector selector) {
    this.socket = socket;
    this.socketChannel = socket.getChannel();
    this.forwarder = forwarder;
    this.selector = selector;
  }

  public void run2() {
    try {
      DataInputStream inputStream = new DataInputStream(socket.getInputStream());
      while(true) {
        ByteBuffer response = ByteBuffer.allocate(65535);
        int length = inputStream.read(response.array());
        if(length > 0) {
          //Log.d("TCP", "" + length);
          count += length;
          response.limit(length);
          responses.add(response);
        }
        send();
      }
    } catch (Exception e) {
      e.printStackTrace();
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
      while(iterator.hasNext()) {
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
          }
        }
      }
    }
  }

  public void send() {
    if(responses.isEmpty()) return;
    byte[] temp = new byte[Math.min(responses.element().remaining(), limit)];
    responses.element().get(temp);
    if(responses.element().remaining() == 0) responses.remove();
    forwarder.receive(temp);
    count -= temp.length;
    Log.d("TCP", "Remain : " + count);
  }
}
