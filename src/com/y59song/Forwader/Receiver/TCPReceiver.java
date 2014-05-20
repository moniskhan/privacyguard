package com.y59song.Forwader.Receiver;

import android.util.Log;
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
        ByteBuffer response = ByteBuffer.allocate(65535);
        int length = inputStream.read(response.array());
        if(length > 0) {
          Log.d("TCP", "" + length);
          byte[] temp = new byte[length];
          response.limit(length);
          response.get(temp);
          forwarder.receive(temp);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
