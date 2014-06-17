package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPForwarderWorker;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;
import com.y59song.Utilities.ByteOperations;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements Runnable, ICommunication {
  private final String TAG = "TCPForwarder";
  protected Socket socket;
  private SocketChannel socketChannel;
  private TCPForwarderWorker receiver;
  private TCPConnectionInfo conn_info;
  private ArrayDeque<IPDatagram> packets;
  private int lastLen = 0;

  public enum Status {
    DATA, LISTEN, SYN_ACK_SENT, HALF_CLOSE_BY_CLIENT, HALF_CLOSE_BY_SERVER, CLOSED;
  }

  protected Status status;

  public TCPForwarder(MyVpnService vpnService) {
    super(vpnService);
    status = Status.LISTEN;
    packets = new ArrayDeque<IPDatagram>();
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
    handle_packet(ipDatagram);
    /*
    synchronized (packets) {
      packets.addLast(ipDatagram);
      packets.notify();
    }
    */
  }

  @Override
  public void run() {
    IPDatagram temp;
    while(!closed) {
      synchronized (packets) {
        while ((temp = packets.pollFirst()) == null) {
          try {
            packets.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      handle_packet(temp);
    }
  }

  protected synchronized void handle_request (IPDatagram ipDatagram) {
    if(conn_info == null) conn_info = new TCPConnectionInfo(ipDatagram);
    switch(status) {
      case LISTEN: conn_info.reset(ipDatagram);
      case SYN_ACK_SENT: handle_handshake((TCPDatagram)ipDatagram.payLoad()); break;
      case DATA : handle_data((TCPDatagram)ipDatagram.payLoad()); break;
      case HALF_CLOSE_BY_CLIENT: close(); break;
      default : break;
    }
  }

  protected void handle_handshake(TCPDatagram tcpDatagram) {
    byte flag = ((TCPHeader)tcpDatagram.header()).getFlag();
    int len = tcpDatagram.virtualLength();
    if((status == Status.LISTEN && flag != TCPHeader.SYN) ||
       (status == Status.SYN_ACK_SENT && flag != TCPHeader.ACK)) {
      lastLen = forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.RST), null));
      close();
    } else if(status == Status.LISTEN) {
      conn_info.setup(this);
      lastLen = forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.SYNACK), null));
      status = Status.SYN_ACK_SENT;
    } else {
      conn_info.increaseSeq(1);
      status = Status.DATA;
    }
  }

  protected void handle_data(TCPDatagram tcpDatagram) {
    byte flag = ((TCPHeader)tcpDatagram.header()).getFlag();
    int len = tcpDatagram.virtualLength(), rlen = tcpDatagram.dataLength();
    boolean changed = conn_info.setSeq(((TCPHeader) tcpDatagram.header()).getAck_num());
    if (rlen != 0) {
      send(tcpDatagram);
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null));
    } else if(flag == TCPHeader.FINACK) {
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null));
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null));
      close();
    } else if(flag == TCPHeader.RST) {
      close();
    }

    if(changed) {
      Log.d(TAG, "" + ((TCPHeader) tcpDatagram.header()).getAck_num());
      receiver.interrupt();
    }

  }

  protected synchronized void handle_packet (IPDatagram ipDatagram) {
    if(closed) return;
    byte flag;
    int len, rlen;
    if(ipDatagram != null) {
      flag = ((TCPHeader)ipDatagram.payLoad().header()).getFlag();
      len = ipDatagram.payLoad().virtualLength();
      rlen = ipDatagram.payLoad().dataLength();
      if(conn_info == null) conn_info = new TCPConnectionInfo(ipDatagram);
    } else return;
    Log.d(TAG, "" + status + "," + closed);
    switch(status) {
      case LISTEN:
        if(flag != TCPHeader.SYN) {
          close();
          return;
        }
        conn_info.reset(ipDatagram);
        conn_info.setup(this);
        conn_info.increaseSeq(
          forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.SYNACK), null))
        );
        status = Status.SYN_ACK_SENT;
        break;
      case SYN_ACK_SENT:
        if(flag != TCPHeader.ACK) {
          close();
          return;
        }
        status = Status.DATA;
        break;
      case DATA:
        assert((flag & TCPHeader.ACK) != 0);
        if(rlen > 0) { // send data
          send(ipDatagram.payLoad());
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
        } else if(flag == TCPHeader.FINACK) { // FIN
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
          );
          close();
        } else if((flag & TCPHeader.RST) != 0) { // RST
          close();
        }
        break;
      case HALF_CLOSE_BY_CLIENT:
        assert(flag == TCPHeader.ACK);
        status = Status.CLOSED;
        close();
        break;
      case HALF_CLOSE_BY_SERVER:
        if(flag == TCPHeader.FINACK) {
          conn_info.increaseSeq(
            forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
          );
          status = Status.CLOSED;
          close();
        } // ELSE ACK for the finack sent by the server
        break;
      case CLOSED:
        status = Status.CLOSED;
      default:
        break;
    }
    //receiver.setLastAck(((TCPHeader)ipDatagram.payLoad().header()).getAck_num());
  }

  @Override
  public synchronized void receive (byte[] response) {
    if(conn_info == null) return;
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.DATA), response))
    );
    Log.d(TAG, "Receive Response : " + response.length);
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void send(IPPayLoad payLoad) {
    if(isClosed()) {
      status = Status.HALF_CLOSE_BY_SERVER;
      conn_info.increaseSeq(
        forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
      );
    }
    Log.d(TAG, "" + conn_info.getTransHeader().getSrcPort() + " " + ByteOperations.byteArrayToString(payLoad.data()));
    receiver.send(payLoad.data());
  }

  @Override
  public void open() {
    if(!closed) return;
    super.open();
    status = Status.LISTEN;
  }

  @Override
  public void close() {
    if(closed) return;
    closed = true;
    conn_info = null;
    status = Status.CLOSED;
    if(socketChannel != null) {
      try {
        socketChannel.close();
        socketChannel = null;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    /*
    Log.d(TAG, "Release");
    ForwarderPools temp = vpnService.getForwarderPools();
    temp.release(this);
    */
    Log.d(TAG, "Released");
  }

  @Override
  public void setup(InetAddress srcAddress, int src_port) {
    try {
      if(socketChannel == null) socketChannel = SocketChannel.open();
      socket = socketChannel.socket();
      //socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), src_port));
      //socketChannel.connect(new InetSocketAddress(LocalServer.port));
      vpnService.protect(socket);
      socketChannel.connect(new InetSocketAddress(srcAddress, src_port));
      socketChannel.configureBlocking(false);
      Selector selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
      receiver = new TCPForwarderWorker(socketChannel, this, selector);
      receiver.start();
      Log.d(TAG, "START");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
