package com.y59song.Network;

import com.y59song.LocationGuard.MyVpnService;

import java.net.InetAddress;

/**
 * Created by frank on 2014-03-27.
 */
public class Forwarder implements Runnable {
  private InetAddress dstAddress;
  private int dstPort;
  private IPDatagram ipDatagram;
  private MyVpnService vpnService;

  public Forwarder(IPDatagram ipDatagram, MyVpnService vpnService) {
    super();
    this.dstAddress = ipDatagram.header().getDstAddress();
    this.dstPort = ipDatagram.payLoad().getDstPort();
    this.ipDatagram = ipDatagram;
    this.vpnService = vpnService;
  }

  @Override
  public void run() {
    ICommunication datagram = (ICommunication)(ipDatagram.payLoad());
    vpnService.protect(datagram.connect(dstAddress, dstPort));
    datagram.send(dstAddress, dstPort);
    byte[] response = datagram.receive();

    // build tcp datagram
    IPHeader reverseIPHeader = ipDatagram.header().reverse();
    byte[] payLoaderHeader = ipDatagram.payLoad().header().reverse().toByteArray(); // set corrent port
    IPPayLoad newPayLoad = null;
    //if(ipDatagram.header().protocol() == IPDatagram.TCP) // TODO
      newPayLoad = new TCPDatagram(new TCPHeader(payLoaderHeader), response); // set the response
    newPayLoad.update(reverseIPHeader); // set the corrent checksum

    // tcp datagram is done, now ip datagram
    IPDatagram newIPDatagram = new IPDatagram(reverseIPHeader, newPayLoad);
    vpnService.fetchResponse(newIPDatagram.toByteArray());
  }

  public void handshake() {
    // Receive the first SYN
    TCPDatagram tcpDatagram = (TCPDatagram)ipDatagram.payLoad();
    // Respond with SYNACK
    TCPDatagram syn_ackDatagram = TCPDatagram.createSYNACK((TCPHeader)tcpDatagram.header());
    // Receive the ACK
    // Receive the SYN
    // Respond with ACK
    // Respond with DATA
    // Receive the ACK
    // Receive
  }
}
