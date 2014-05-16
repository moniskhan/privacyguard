package com.y59song.Network;

import com.y59song.Forwader.ICommunication;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;

import java.net.InetAddress;

/**
 * Created by y59song on 16/05/14.
 */
public class ConnectionInfo {
  protected InetAddress clientAddress, serverAddress;
  protected int clientPort, serverPort;
  protected int protocol;

  protected IPHeader responseIPHeader;
  protected TransportHeader responseTransHeader;

  public ConnectionInfo(IPDatagram ipDatagram) {
    this.clientAddress = ipDatagram.header().getSrcAddress();
    this.serverAddress = ipDatagram.header().getDstAddress();
    this.clientPort = ipDatagram.payLoad().getSrcPort();
    this.serverPort = ipDatagram.payLoad().getDstPort();
    this.protocol = ipDatagram.header().protocol();
    this.responseIPHeader = ipDatagram.header().reverse();
    this.responseTransHeader = ipDatagram.payLoad().header().reverse();
  }

  public IPHeader getIPHeader() {
    return responseIPHeader;
  }

  public TransportHeader getTransHeader() {
    return responseTransHeader;
  }

  public void setup(ICommunication forwarder) {
    forwarder.setup(serverAddress, serverPort);
  }
}
