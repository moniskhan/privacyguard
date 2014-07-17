/*
 * Implement a simple udp protocol
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.UDP.UDPDatagram;
import com.y59song.Network.UDP.UDPHeader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-29.
 */
public class UDPForwarder extends AbsForwarder implements ICommunication {
  private static final String TAG = "UDPForwarder";
  private DatagramSocket socket;
  private ByteBuffer packet;
  private DatagramPacket response;

  public UDPForwarder(MyVpnService vpnService) {
    super(vpnService);
    packet = ByteBuffer.allocate(32767);
    response = new DatagramPacket(packet.array(), 32767);
  }

  @Override
  protected void forward(IPDatagram ipDatagram) {
    if(closed) return;
    UDPDatagram udpDatagram = (UDPDatagram)ipDatagram.payLoad();
    setup(null, -1, ipDatagram.header().getDstAddress(), ipDatagram.payLoad().getDstPort());
    send(udpDatagram);

    IPHeader newIPHeader = ipDatagram.header().reverse();
    UDPHeader newUDPHeader = (UDPHeader)udpDatagram.header().reverse();
    byte[] received = receive();
    if(received != null) {
      forwardResponse(newIPHeader, new UDPDatagram(newUDPHeader, received));
    }
    close();
  }

  @Override
  protected void receive(byte[] response) {
  }

  @Override
  public void setup(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) {
    try {
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

  public byte[] receive() {
    try {
      packet.clear();
      socket.setSoTimeout(10000);
      socket.receive(response);
    } catch (SocketTimeoutException e) {
      close();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Arrays.copyOfRange(response.getData(), 0, response.getLength());
  }

  @Override
  public void close() {
    if(socket != null && !socket.isClosed()) socket.close();
    closed = true;
    vpnService.getForwarderPools().release(this);
  }
}
