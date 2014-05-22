package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;

import java.io.DataInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private Socket socket;
  private TCPForwarder forwarder;
  private LinkedList<ByteBuffer> responses = new LinkedList<ByteBuffer>();
  private int count = 0;
  private final int limit = 2048;

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
