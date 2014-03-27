package com.y59song.Network;

import com.y59song.Utilities.ByteOperations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPDatagram extends IPPayLoad implements ICommunication {
  Socket socket;

  public static TCPDatagram create(byte[] data) {
    TCPHeader header = new TCPHeader(data);
    return new TCPDatagram(header, Arrays.copyOfRange(data, header.offset(), data.length));
  }

  public TCPDatagram(TCPHeader header, byte[] data) {
    this.header = header;
    this.data = data;
  }

  @Override
  public int getSrcPort() {
    return ((TCPHeader)header).srcPort();
  }

  @Override
  public int getDstPort() {
    return ((TCPHeader)header).dstPort();
  }

  @Override
  public void update(IPHeader ipHeader) {
    byte[] pseudoHeader = this.getPseudoHeader(ipHeader);
    header.setCheckSum(new byte[]{0, 0});
    byte[] toComputeCheckSum = ByteOperations.concatenate(pseudoHeader, header.toByteArray(), data);
    header.setCheckSum(ByteOperations.computeCheckSum(toComputeCheckSum));
  }

  @Override
  public Socket connect(InetAddress dstAddress, int port) {
    try {
      socket = new Socket(dstAddress, port);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return socket;
  }

  @Override
  public void send(InetAddress dstAddress, int dstPort) {
    try {
      OutputStream outputStream = socket.getOutputStream();
      outputStream.write(data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public byte[] receive() {
    try {
      ByteBuffer response = ByteBuffer.allocate(32767);
      InputStream inputStream = socket.getInputStream();
      int length = inputStream.read(response.array());
      response.limit(length);
      return response.array();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
