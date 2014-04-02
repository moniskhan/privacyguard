package com.y59song.Forwader;

import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IPPayLoad;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Utilities.ByteOperations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
  private Socket socket;
  private DataInputStream inputStream;
  private DataOutputStream outputStream;
  private int offset = 0, total_length = 0;
  private boolean transEnd = false;
  private byte[] response;

  public enum Status {
    END, DATA, HAND_SHAKE;
  }

  private Status status;

  public TCPForwarder(MyVpnService vpnService) {
    super(vpnService);
    response = new byte[]{};
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
  protected void process(IPDatagram ipDatagram) {
    IPHeader newIPHeader = ipDatagram.header().reverse();
    TCPDatagram tcpDatagram = (TCPDatagram) ipDatagram.payLoad();
    TCPHeader tcpHeader = (TCPHeader) tcpDatagram.header();
    byte flag = tcpHeader.getFlag();
    if((flag & TCPHeader.SYN) == TCPHeader.SYN) {
      status = Status.HAND_SHAKE;
      forwardResponse(newIPHeader, handshake(tcpDatagram));
    } else if((flag & TCPHeader.DATA) == TCPHeader.DATA) {
      dstAddress = ipDatagram.header().getDstAddress();
      transEnd = false;
      forwardResponse(newIPHeader, data_ack(tcpDatagram));
      forwardResponse(newIPHeader, data_transfer(tcpDatagram, true));
    } else if((flag & TCPHeader.ACK) == TCPHeader.ACK) {
      switch(status) {
        case HAND_SHAKE: status = Status.DATA; break;
        case END: status = Status.HAND_SHAKE; break;
        case DATA: {
          TCPDatagram temp = data_transfer(tcpDatagram, false);
          forwardResponse(newIPHeader, temp);
          break;
        }
      }
    } else if((flag & TCPHeader.FIN) != 0) {
      status = Status.END;
      forwardResponse(newIPHeader, end_ack(tcpDatagram));
      forwardResponse(newIPHeader, end_fin_ack(tcpDatagram));
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

  private TCPDatagram data_transfer(TCPDatagram tcpDatagram, boolean isFirst) {
    //Log.d(TAG, " Request : " + ByteOperations.byteArrayToString(datagram.data));
    if(isFirst) { setup(dstAddress, tcpDatagram.getDstPort()); send(tcpDatagram); }
    if(response.length <= (offset + 1024) && !transEnd) {
      response = ByteOperations.concatenate(response, receive());
    }
    //Log.d(TAG, " Response : " + ByteOperations.byteArrayToString(response));
    int begin = offset, end = Math.min(offset + 1024, response.length);
    offset = end;
    TCPHeader newTCPHeader = TCPHeader.createDATA(tcpDatagram, begin == end);
    return new TCPDatagram(newTCPHeader, response, begin, end);
  }

  private TCPDatagram end_ack(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createACKSEQ(tcpDatagram);
    return new TCPDatagram(newTCPHeader, null);
  }

  private TCPDatagram end_fin_ack(TCPDatagram tcpDatagram) {
    TCPHeader newTCPHeader = TCPHeader.createACKFIN(tcpDatagram);
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
  public byte[] receive() {
    if(transEnd) return null;
    try {
      ByteBuffer response = ByteBuffer.allocate(32767);
      //Log.d(TAG, "" + (socket == null) + " , " + (socket.getInputStream() == null));
      if (inputStream == null) inputStream = new DataInputStream(socket.getInputStream());
      int length = inputStream.read(response.array());
      if(length <= 0) throw new Exception();
      response.limit(length);
      return Arrays.copyOfRange(response.array(), 0, length);
    } catch (Exception e) {
      transEnd = true;
      return null;
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
