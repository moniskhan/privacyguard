package com.y59song.Network.TCP;

import com.y59song.Network.TransportHeader;
import com.y59song.Utilities.ByteOperations;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPHeader extends TransportHeader {
  private int offset, seq_num, ack_num;
  private static final String TAG = "TCPHeader";
  public static final byte FIN = 0x01;
  public static final byte ACK = 0x10;
  public static final byte SYN = 0x02;
  public static final byte DATA = 0x18;

  public TCPHeader(byte[] data) {
    super(data);
    offset = (data[12] & 0xF0) / 4;
    seq_num = ByteOperations.byteArrayToInteger(data, 4, 8);
    ack_num = ByteOperations.byteArrayToInteger(data, 8, 12);
    checkSum_pos = 16;
    checkSum_size = 2;
    this.data = Arrays.copyOfRange(data, 0, offset);
  }

  public static TCPHeader createACK(TCPDatagram tcpDatagram) {
    // set ACK
    TCPHeader header = (TCPHeader) tcpDatagram.header();
    TCPHeader ret = header.reverse();
    ret.setAck_num(header.getSeq_num() + tcpDatagram.dataLength());
    ret.setSeq_num(header.getAck_num());
    ret.set_FLAG(ACK);
    return ret;
  }

  public static TCPHeader createSYNACK(TCPDatagram tcpDatagram) {
    // set SYN
    TCPHeader header = (TCPHeader) tcpDatagram.header();
    TCPHeader ret = header.reverse();
    ret.setAck_num(header.getSeq_num() + 1);
    ret.setSeq_num(20000);
    ret.set_FLAG((byte)(ACK | SYN));
    return ret;
  }

  public static TCPHeader createDATA(TCPDatagram tcpDatagram, boolean last) {
    // set DATA
    TCPHeader header = (TCPHeader) tcpDatagram.header();
    TCPHeader ret = header.reverse();
    ret.setSeq_num(header.getAck_num());
    ret.setAck_num(header.getSeq_num() + tcpDatagram.dataLength());
    if(!last) ret.set_FLAG(ACK);
    else ret.set_FLAG(DATA);
    return ret;
  }

  public static TCPHeader createACKSEQ(TCPDatagram tcpDatagram) {
    // set ACK, SEQ
    TCPHeader ret = createACK(tcpDatagram);
    TCPHeader header = (TCPHeader) tcpDatagram.header();
    ret.setSeq_num(header.getAck_num());
    return ret;
  }

  public static TCPHeader createACKFIN(TCPDatagram tcpDatagram) {
    TCPHeader ret = createACK(tcpDatagram);
    ret.set_FLAG((byte)(ACK | FIN));
    return ret;
  }

  public int offset() {
    return offset;
  }

  @Override
  public TCPHeader reverse() {
    byte[] reverseData = Arrays.copyOfRange(data, 0, data.length);
    ByteOperations.swap(reverseData, 0, 2, 2);
    return new TCPHeader(reverseData);
  }

  private void setAck_num(int ack) {
    byte[] bytes = ByteBuffer.allocate(4).putInt(ack).array();
    System.arraycopy(bytes, 0, data, 8, 4);
  }

  private void setSeq_num(int seq) {
    byte[] bytes = ByteBuffer.allocate(4).putInt(seq).array();
    System.arraycopy(bytes, 0, data, 4, 4);
  }

  private void set_FLAG(byte flag) {
    data[13] = flag;
  }

  public int getSeq_num() {
    seq_num = ByteOperations.byteArrayToInteger(data, 4, 8);
    return seq_num;
  }

  public int getAck_num() {
    ack_num = ByteOperations.byteArrayToInteger(data, 8, 12);
    return ack_num;
  }

  public byte getFlag() {
    return data[13];
  }
}
