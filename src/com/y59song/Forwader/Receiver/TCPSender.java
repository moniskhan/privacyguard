package com.y59song.Forwader.Receiver;

import com.y59song.Forwader.TCPForwarder_old;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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

  }
}
