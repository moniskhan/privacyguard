package com.y59song.Forwader.Receiver;

import android.util.Log;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;

import java.io.DataInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by y59song on 03/04/14.
 */
public class TCPReceiver implements Runnable {
  private Socket socket;
  private TCPForwarder forwarder;
  private Queue<ByteBuffer> queue;
  private boolean isFirst = false;
  private IPHeader requestHeader;
  private TCPDatagram requestTCPDatagram;

  public TCPReceiver(Socket socket, TCPForwarder forwarder) {
    this.socket = socket;
    this.forwarder = forwarder;
    this.queue = new LinkedList<ByteBuffer>();
  }

  @Override
  public synchronized void run() {
    try {
      DataInputStream inputStream = new DataInputStream(socket.getInputStream());
      while(true) {
        ByteBuffer response = ByteBuffer.allocate(32767);
        int length = inputStream.read(response.array());
        Log.d("TCP", "" + length);
        if(length > 0) {
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

  private ByteBuffer getResponse() {
    ByteBuffer response = queue.element();
    byte[] temp = new byte[1024];
    if(response.remaining() < 1024) queue.remove();
    return response.get(temp, 0, Math.min(1024, response.remaining()));
  }

  public synchronized void update(IPHeader ipHeader, TCPDatagram tcpDatagram, boolean value) {
    isFirst = value;
    if(isFirst) { requestHeader = ipHeader; requestTCPDatagram = tcpDatagram; return; }
    else forwarder.forwardResponse(ipHeader, data_transfer(tcpDatagram));
  }

  private TCPDatagram data_transfer(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createDATA(tcpDatagram, true);
    return new TCPDatagram(newTCPHeader, getResponse().array());
  }
}
