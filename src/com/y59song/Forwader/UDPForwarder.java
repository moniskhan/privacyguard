package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IPPayLoad;
import com.y59song.Network.UDP.UDPDatagram;
import com.y59song.Network.UDP.UDPHeader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-29.
 */
public class UDPForwarder extends AbsForwarder implements ICommunication {
  private static final String TAG = "UDPForwarder";
  private DatagramSocket socket;

  public UDPForwarder(MyVpnService vpnService) {
    super(vpnService);
  }

  @Override
  protected void process(IPDatagram ipDatagram) {
    UDPDatagram udpDatagram = (UDPDatagram)ipDatagram.payLoad();
    //Log.d(TAG, "Request : " + ByteOperations.byteArrayToString(udpDatagram.data()));
    setup(ipDatagram.header().getDstAddress(), ipDatagram.payLoad().getDstPort());
    send(udpDatagram);

    byte[] response = receive();
    //Log.d(TAG, "Response " + ByteOperations.byteArrayToString(response));
    //Log.d(TAG, "Response : " + ByteOperations.byteArrayToString(response));
    IPHeader newIPHeader = ipDatagram.header().reverse();
    UDPHeader newUDPHeader = (UDPHeader)udpDatagram.header().reverse();
    UDPDatagram newUDPDatagram = new UDPDatagram(newUDPHeader, response);
    forwardResponse(newIPHeader, newUDPDatagram);
  }


  @Override
  public void setup(InetAddress dstAddress, int dstPort) {
    try {
      //socket = DatagramChannel.open().socket();
      socket = new DatagramSocket();
    } catch (IOException e) {
      e.printStackTrace();
    }
    vpnService.protect(socket);
    this.dstAddress = dstAddress;
    this.dstPort = dstPort;
  }

  @Override
  public void send(IPPayLoad payLoad) {
    try {
      socket.send(new DatagramPacket(payLoad.data(), payLoad.dataLength(), dstAddress, dstPort));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public byte[] receive() {
    ByteBuffer packet = ByteBuffer.allocate(32767);
    DatagramPacket response = new DatagramPacket(packet.array(), 32767);
    try {
      socket.receive(response);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Arrays.copyOfRange(response.getData(), 0, response.getLength());
  }

  @Override
  public void close() {
    socket.close();
  }
}
