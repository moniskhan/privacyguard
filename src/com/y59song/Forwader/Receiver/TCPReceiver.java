package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.Utilities.SSLParser;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

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

  public TCPReceiver(Socket socket, TCPForwarder forwarder, Selector selector) {
    this.socketChannel = socket.getChannel();
    this.forwarder = forwarder;
    this.selector = selector;
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
            SSLParser.getCertificate(temp);
            forwarder.receive(temp);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
