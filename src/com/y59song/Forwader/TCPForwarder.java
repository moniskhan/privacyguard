package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPReceiver;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPHeader;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
  private Socket socket;
  private IPHeader newIPHeader;
  private DataOutputStream outputStream;
  private TCPReceiver receiver;
  private TCPConnectionInfo conn_info;

  public enum Status {
    END_CLIENT, END_SERVER, END, DATA, LISTEN, SYN_ACK_SENT;
  }

  private Status status;

  public TCPForwarder(MyVpnService vpnService) {
    super(vpnService);
    status = Status.LISTEN;
  }

  /*
   * step 1 : reverse the IP header
   * step 2 : create a new TCP header, set the syn, ack right
   * step 3 : get the response if necessary
   * step 4 : combine the response and create a new tcp datagram
   * step 5 : update the datagram's checksum
   * step 6 : combine the tcp datagram and the ip datagram, update the ip header
   */
  protected void forward (IPDatagram ipDatagram) {
    byte flag = 0;
    int len = 0, rlen = 0;
    if(ipDatagram != null) {
      flag = ((TCPHeader)ipDatagram.payLoad().header()).getFlag();
      len = ipDatagram.payLoad().virtualLength();
      rlen = ipDatagram.payLoad().dataLength();
      if(conn_info == null) conn_info = new TCPConnectionInfo(ipDatagram);
    }
    Log.d(TAG, "" + status);
    switch(status) {
      case LISTEN:
        assert(flag == TCPHeader.SYN);
        conn_info.increaseSeq(
          forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.SYNACK), null))
        );
        status = Status.SYN_ACK_SENT;
        break;
      case SYN_ACK_SENT:
        if(flag == TCPHeader.SYN) {
          status = Status.LISTEN;
          forward(ipDatagram);
        } else {
          assert(flag == TCPHeader.ACK);
          status = Status.DATA;
          conn_info.setup(this);
        }
        break;
      case DATA:
        if(rlen > 0) {
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          send(ipDatagram.payLoad());
        } else if((flag & TCPHeader.FIN) != 0) {
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
          );
          close();
          status = Status.END;
        }
        break;
      case END_CLIENT:
        assert(flag == TCPHeader.FINACK);
        conn_info.increaseSeq(
          forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
        );
        status = Status.END;
        break;
      case END_SERVER:
        conn_info.increaseSeq(
          forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
        );
        close();
        status = Status.END_CLIENT;
      default:
        break;
    }
  }

  @Override
  public void receive (byte[] response) {
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.DATA), response))
    );
  }

  @Override
  public void send(IPPayLoad payLoad) {
    if(socket != null && !socket.isConnected()) {
      status = Status.END_SERVER;
      forward(null);
    }
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
    conn_info = null;
    if(socket == null) return;
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
