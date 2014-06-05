package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPReceiver;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.LocalServer;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
  protected SocketChannel socketChannel;
  protected Socket socket;
  protected TCPReceiver receiver;
  protected TCPConnectionInfo conn_info;

  public enum Status {
    END_CLIENT, END_SERVER, END, DATA, LISTEN, SYN_ACK_SENT;
  }

  protected Status status;

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
    byte flag;
    int len, rlen;
    if(ipDatagram != null) {
      flag = ((TCPHeader)ipDatagram.payLoad().header()).getFlag();
      len = ipDatagram.payLoad().virtualLength();
      rlen = ipDatagram.payLoad().dataLength();
      if(conn_info == null) conn_info = new TCPConnectionInfo(ipDatagram);
      conn_info.setAck(((TCPHeader)ipDatagram.payLoad().header()).getSeq_num());
    } else return;
    Log.d(TAG, "" + status);
    switch(status) {
      case LISTEN:
        if(flag != TCPHeader.SYN) return;
        conn_info.reset(ipDatagram);
        conn_info.setup(this);
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
        }
        break;
      case DATA:
        //int ack = ((TCPHeader)ipDatagram.payLoad().header()).getAck_num();
        if(rlen > 0) {
          //conn_info.reset(ipDatagram);
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          send(ipDatagram.payLoad());
        } else if((flag & TCPHeader.FIN) != 0) {
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
          );
          close();
          status = Status.END;
        } else if((flag & TCPHeader.RST) != 0) {
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
        break;
      case END:
        status = Status.LISTEN;
      default:
        break;
    }
  }

  @Override
  public void receive (byte[] response) {
    if(conn_info == null) return;
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.DATA), response))
    );
  }

  @Override
  public void send(IPPayLoad payLoad) {
    if (socket != null && !socket.isConnected()) {
      status = Status.END_SERVER;
      forward(null);
    } else {
      try {
        socketChannel.write(ByteBuffer.wrap(payLoad.data()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void close() {
    conn_info = null;
    //status = Status.LISTEN;
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
  public void setup(InetAddress srcAddress, int src_port) {
    try {
      if(socketChannel == null) socketChannel = SocketChannel.open();
      socket = socketChannel.socket();
      socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      socketChannel.connect(new InetSocketAddress(LocalServer.port));
      socketChannel.configureBlocking(false);
      Selector selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
      receiver = new TCPReceiver(socket, this, selector);
      new Thread(receiver).start();
      Log.d(TAG, "START");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
  public static void test() {
    try {
      SocketChannel socketChannel = SocketChannel.open();
      socketChannel.connect(new InetSocketAddress(12345));
      byte[] temp = new byte[1];
      temp[0] = 'a';
      Log.d("Test", "Test");
      socketChannel.write(ByteBuffer.wrap(temp));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  */
}
