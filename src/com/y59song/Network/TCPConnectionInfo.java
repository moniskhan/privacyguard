package com.y59song.Network;

import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.TCP.TCPHeader;

/**
 * Created by y59song on 16/05/14.
 */
public class TCPConnectionInfo extends ConnectionInfo {
  private static final String TAG = "TCPConnectionInfo";
  public int seq, ack;
  public TCPConnectionInfo(IPDatagram ipDatagram) {
    super(ipDatagram);
    reset(ipDatagram);
  }

  public synchronized boolean setSeq(int seq) {
    //if(this.seq == seq) return false;
    this.seq = seq;
    ((TCPHeader)responseTransHeader).setSeq_num(seq);
    //Log.d("TCPConnectionInfo setSeq", "" + this.seq + "," + ((TCPHeader)responseTransHeader).getSeq_num());
    return true;
  }

  public synchronized boolean setAck(int ack) {
    //if(this.ack == ack) return false;
    this.ack = ack;
    ((TCPHeader)responseTransHeader).setAck_num(ack);
    return true;
  }

  @Override
  public void reset(IPDatagram ipDatagram) {
    super.reset(ipDatagram);
    assert(protocol == IPDatagram.TCP);
    setSeq(((TCPHeader) ipDatagram.payLoad().header()).getAck_num());
    setAck(((TCPHeader) ipDatagram.payLoad().header()).getSeq_num());
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
