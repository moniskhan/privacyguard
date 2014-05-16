package com.y59song.Network;

import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.TCP.TCPHeader;

/**
 * Created by y59song on 16/05/14.
 */
public class TCPConnectionInfo extends ConnectionInfo {
  private int seq, ack;
  public TCPConnectionInfo(IPDatagram ipDatagram) {
    super(ipDatagram);
    assert(protocol == IPDatagram.TCP);
    seq = 1;
    ack = ((TCPHeader) ipDatagram.payLoad().header()).getSeq_num();
  }

  public void setSeq(int seq) {
    this.seq = seq;
    ((TCPHeader)responseTransHeader).setSeq_num(seq);
  }

  public void setAck(int ack) {
    this.ack = ack;
    ((TCPHeader)responseTransHeader).setAck_num(ack);
  }

  public void setFlag(byte flag) {
    ((TCPHeader)responseTransHeader).setFlag(flag);
  }

  public void increaseSeq(int inc) {
    setSeq(seq + inc);
  }

  public void increaseAck(int inc) {
    setAck(ack + inc);
  }

  public TCPHeader getTransHeader(int inc, byte flag) {
    increaseAck(inc);
    setFlag(flag);
    return (TCPHeader)responseTransHeader;
  }
}
