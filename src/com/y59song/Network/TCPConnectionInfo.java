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
    setSeq(1);
    setAck(((TCPHeader) ipDatagram.payLoad().header()).getSeq_num());
  }

  private synchronized void setSeq(int seq) {
    this.seq = seq;
    ((TCPHeader)responseTransHeader).setSeq_num(seq);
  }

  private synchronized void setAck(int ack) {
    this.ack = ack;
    ((TCPHeader)responseTransHeader).setAck_num(ack);
  }

  private void setFlag(byte flag) {
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
