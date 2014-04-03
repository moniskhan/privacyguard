package com.y59song.Forwader.Receiver;

import com.y59song.Forwader.TCPForwarder;

import java.io.DataInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private Socket socket;
  private TCPForwarder forwarder;

  public TCPReceiver(Socket socket, TCPForwarder forwarder) {
    this.socket = socket;
    this.forwarder = forwarder;
  }

  @Override
  public void run() {
    try {
      DataInputStream inputStream = new DataInputStream(socket.getInputStream());
      while(true) {
        ByteBuffer response = ByteBuffer.allocate(32767);
        //Log.d(TAG, "" + (socket == null) + " , " + (socket.getInputStream() == null));
        if (inputStream == null) inputStream = new DataInputStream(socket.getInputStream());
        int length = inputStream.read(response.array());
        if(length <= 0) throw new Exception();
        else {
          response.limit(length);
          forwarder.send(response.array(), length);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
