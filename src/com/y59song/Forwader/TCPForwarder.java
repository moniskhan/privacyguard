package com.y59song.Forwader;

import com.y59song.Forwader.Receiver.TCPReceiver;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IPPayLoad;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
  private Socket socket;
  private IPHeader newIPHeader;
  private DataInputStream inputStream;
  private DataOutputStream outputStream;
  private int expected_ack, exptected_seq;
  private boolean transEnd = false;
  private ByteBuffer response;
  private TCPReceiver receiver;

  public enum Status {
    END, DATA, HAND_SHAKE;
  }

  private Status status;

  public TCPForwarder(MyVpnService vpnService) {
    super(vpnService);
    response = ByteBuffer.allocate(32767);
    status = Status.HAND_SHAKE;
  }

  /*
   * step 1 : reverse the IP header
   * step 2 : create a new TCP header, set the syn, ack right
   * step 3 : get the response if necessary
   * step 4 : combine the response and create a new tcp datagram
   * step 5 : update the datagram's checksum
   * step 6 : combine the tcp datagram and the ip datagram, update the ip header
   */
  @Override
  protected void forward (IPDatagram ipDatagram) {
    newIPHeader = ipDatagram.header().reverse();
    TCPDatagram tcpDatagram = (TCPDatagram) ipDatagram.payLoad(), newTCPDatagram = null;
    TCPHeader tcpHeader = (TCPHeader) tcpDatagram.header();
    byte flag = tcpHeader.getFlag();
    if((flag & TCPHeader.SYN) != 0) {
      status = Status.HAND_SHAKE;
      newTCPDatagram = handshake(tcpDatagram);
      forwardResponse(newIPHeader, newTCPDatagram);
      expected_ack = ((TCPHeader) newTCPDatagram.header()).getSeq_num() + 1;
    } else if((flag & TCPHeader.FIN) != 0) {
      status = Status.END;
      newTCPDatagram = end_ack(newTCPDatagram);
      forwardResponse(newIPHeader, newTCPDatagram);
      newTCPDatagram = end_fin_ack(tcpDatagram);
      forwardResponse(newIPHeader, newTCPDatagram);
      close();
    } else if((flag & TCPHeader.PSH) != 0) {
      newTCPDatagram = data_ack(tcpDatagram);
      forwardResponse(newIPHeader, newTCPDatagram);
      send(tcpDatagram);
      receiver.update(newIPHeader, tcpDatagram, true);
    } else { // ACK
      if(status == Status.HAND_SHAKE) {
        setup(ipDatagram.header().getDstAddress(), tcpDatagram.getDstPort());
        status = Status.DATA;
      } else if(status == Status.DATA) {
        receiver.update(newIPHeader, tcpDatagram, false);
      }
    }
  }

  @Override
  protected void receive (ByteBuffer response) {
    if(this.response.position() == this.response.limit())
      this.response = response;
    else {
      this.response.put(response);
      // TODO
    }
  }

  private TCPDatagram handshake(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createSYNACK(tcpDatagram); // set syn, ack, syn_num, ack_num; reversed
    return new TCPDatagram(newTCPHeader, null);
  }

  private TCPDatagram data_ack(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createACK(tcpDatagram);
    return new TCPDatagram(newTCPHeader, null);
  }

  private TCPDatagram end_ack(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createACK(tcpDatagram);
    return new TCPDatagram(newTCPHeader, null);
  }

  private TCPDatagram end_fin_ack(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createFINACK(tcpDatagram);
    return new TCPDatagram(newTCPHeader, null);
  }


  @Override
  public void send(IPPayLoad payLoad) {
    try {
      if(outputStream == null) outputStream = new DataOutputStream(socket.getOutputStream());
      outputStream.write(payLoad.data());
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {
    try{
      if(!socket.isInputShutdown()) socket.shutdownInput();
      if(!socket.isOutputShutdown()) socket.shutdownOutput();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setup(InetAddress dstAddress, int port) {
    try {
      socket = SocketChannel.open().socket();
      vpnService.protect(socket);
      socket.connect(new InetSocketAddress(dstAddress, port));
      socket.setSoTimeout(1000);
      receiver = new TCPReceiver(socket, this);
      new Thread(receiver).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
