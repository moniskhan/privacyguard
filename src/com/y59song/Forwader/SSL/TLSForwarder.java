package com.y59song.Forwader.SSL;

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPReceiver;
import com.y59song.Forwader.TCPForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.SSL.SSLSocketChannel;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.GeneralSecurityException;

/**
 * Created by y59song on 27/05/14.
 */
public class TLSForwarder extends TCPForwarder {
  private static final String TAG = "TLSForwarder";
  private SSLSocketChannel sslSocketChannel;
  private SSLContext sslContext;
  public TLSForwarder(MyVpnService vpnService) {
    super(vpnService);
  }

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
  public void setup(InetAddress dstAddress, int port) {
    try {
      if(socketChannel == null) socketChannel = socketChannel.open();
      socket = socketChannel.socket();
      vpnService.protect(socketChannel.socket());
      socketChannel.connect(new InetSocketAddress(dstAddress, port));
      SiteData remoteData = vpnService.getResolver().getSecureHost(socket, port, true);
      sslContext = vpnService.getSSlSocketFactoryFactory().getSSLContext(remoteData);
      sslSocketChannel = new SSLSocketChannel(socketChannel, sslContext.createSSLEngine());
      sslSocketChannel.configureBlocking(false);
      Selector selector = Selector.open();
      socketChannel.register(selector, SelectionKey.OP_READ);
      receiver = new TCPReceiver(sslSocketChannel.socket(), this, selector);
      new Thread(receiver).start();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    }
  }
}
