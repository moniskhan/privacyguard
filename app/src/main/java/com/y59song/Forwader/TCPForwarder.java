/*
 * TCP forwarder, implement a simple tcp protocol
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

import android.util.Log;
import com.y59song.Forwader.Receiver.TCPForwarderWorker;
import com.y59song.PrivacyGuard.MyVpnService;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Network.TCP.TCPDatagram;
import com.y59song.Network.TCP.TCPHeader;
import com.y59song.Network.TCPConnectionInfo;
import com.y59song.Utilities.MyLogger;

import java.net.InetAddress;
/**
 * Created by frank on 2014-03-27.
 */
public class TCPForwarder extends AbsForwarder implements ICommunication {
  private final String TAG = "TCPForwarder";
  private final boolean DEBUG = false;
  private TCPForwarderWorker receiver;
  private TCPConnectionInfo conn_info;

  public enum Status {
    DATA, LISTEN, SYN_ACK_SENT, HALF_CLOSE_BY_CLIENT, HALF_CLOSE_BY_SERVER, CLOSED
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

  private boolean handle_LISTEN(IPDatagram ipDatagram, byte flag, int len) {
    if(flag != TCPHeader.SYN) {
      close(true);
      return false;
    }
    MyLogger.debugInfo(TAG, "Listen " + ipDatagram.payLoad().header().getSrcPort() + " : " + ipDatagram.payLoad().header().getDstPort());
    conn_info.reset(ipDatagram);
    conn_info.setup(this);
    if(!receiver.isValid()) return false;
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.SYNACK), null))
    );
    status = Status.SYN_ACK_SENT;
    return true;
  }

  private boolean handle_SYN_ACK_SENT(byte flag) {
    if(flag != TCPHeader.ACK) {
      close(true);
      MyLogger.debugInfo(TAG, "SYN_ACK_SENT close");
      return false;
    }
    status = Status.DATA;
    return true;
  }

  private boolean handle_DATA(IPDatagram ipDatagram, byte flag, int len, int rlen) {
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
      MyLogger.debugInfo(TAG, "DATA FIN close");
      close(false);
    } else if((flag & TCPHeader.RST) != 0) { // RST
      close(false);
      MyLogger.debugInfo(TAG, "DATA RST close");
    }
    return true;
  }

  private boolean handle_HALF_CLOSE_BY_CLIENT(byte flag) {
    assert(flag == TCPHeader.ACK);
    status = Status.CLOSED;
    MyLogger.debugInfo(TAG, "HALF_CLOSE_BY_CLIENT close");
    close(false);
    return true;
  }

  private boolean handle_HALF_CLOSE_BY_SERVER(byte flag, int len) {
    if(flag == TCPHeader.FINACK) {
      conn_info.increaseSeq(
        forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(len, TCPHeader.ACK), null))
      );
      status = Status.CLOSED;
      MyLogger.debugInfo(TAG, "HALF_CLOSE_BY_SERVER close");
      close(false);
    } // ELSE ACK for the finack sent by the server
    return true;
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
    //MyLogger.debugInfo(TAG, ((TCPDatagram)ipDatagram.payLoad()).debugInfo());
    switch(status) {
      case LISTEN:
        MyLogger.debugInfo(TAG, "LISTEN");
        if(!handle_LISTEN(ipDatagram, flag, len)) return;
        else break;
      case SYN_ACK_SENT:
        MyLogger.debugInfo(TAG, "SYN_ACK_SENT");
        if(!handle_SYN_ACK_SENT(flag)) return;
        else break;
      case DATA:
        MyLogger.debugInfo(TAG, "DATA");
        if(!handle_DATA(ipDatagram, flag, len, rlen)) return;
        else break;
      case HALF_CLOSE_BY_CLIENT:
        MyLogger.debugInfo(TAG, "HALF_CLOSE_BY_CLIENT");
        if(!handle_HALF_CLOSE_BY_CLIENT(flag)) return;
        else break;
      case HALF_CLOSE_BY_SERVER:
        MyLogger.debugInfo(TAG, "HALF_CLOSE_BY_SERVER");
        if(!handle_HALF_CLOSE_BY_SERVER(flag, len)) return;
        else break;
      case CLOSED:
        //status = Status.CLOSED;
      default:
        break;
    }
  }

  /*
   *  methods for AbsForwarder
   */
  @Override
  public boolean setup(InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port) {
    vpnService.getClientAppResolver().setLocalPortToRemoteMapping(src_port, dstAddress.getHostAddress(), dst_port);
    receiver = new TCPForwarderWorker(srcAddress, src_port, dstAddress, dst_port, this);
    if(!receiver.isValid()) {
      return false;
    } else receiver.start();
    return true;
  }

  @Override
  public void open() {
    if(!closed) return;
    super.open();
    status = Status.LISTEN;
  }

  @Override
  public void close() {
    close(false);
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void forwardRequest(IPDatagram ipDatagram) {
    handle_packet(ipDatagram);
  }

  @Override
  public synchronized void forwardResponse(byte[] response) {
    if(conn_info == null) return;
    conn_info.increaseSeq(
      forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.DATA), response))
    );
  }

  /*
   * Methods for ICommunication
   */
  @Override
  public void send(IPPayLoad payLoad) {
    if(isClosed()) {
      status = Status.HALF_CLOSE_BY_SERVER;
      conn_info.increaseSeq(
        forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.FINACK), null))
      );
    } else {
      receiver.send(payLoad.data());
    }
  }

  private void close(boolean sendRST) {
    closed = true;
    if(sendRST) forwardResponse(conn_info.getIPHeader(), new TCPDatagram(conn_info.getTransHeader(0, TCPHeader.RST), null));
    status = Status.CLOSED;
    if(receiver != null) receiver.interrupt();
    /*
    try {
      if(receiver != null) {
        receiver.interrupt();
        MyLogger.debugInfo("TunReadThreadDispatcher", "INTERRUPTTEE");
        if(receiver.isAlive()) receiver.join();
        MyLogger.debugInfo("TunReadThreadDispatcher", "JOINED");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    */
    vpnService.getForwarderPools().release(this);
    if(DEBUG) Log.d(TAG, "Released");
  }
}
