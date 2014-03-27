package com.y59song.Network;

import android.util.Log;
import com.y59song.Utilities.ByteOperations;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPHeader extends AbsHeader {
  private int srcPort, dstPort, offset, seq_num, ack_num;
  private static final String TAG = "TCPHeader";

  public TCPHeader(byte[] data) {
    srcPort = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
    dstPort = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
    offset = (data[12] & 0xFF) * 4;
    seq_num = ByteOperations.byteArrayToInteger(data, 4, 8);
    ack_num = ByteOperations.byteArrayToInteger(data, 8, 12);
    checkSum_pos = 16;
    checkSum_size = 2;
    Log.d(TAG, "" + (data[13] & 0x2));
    this.data = Arrays.copyOfRange(data, 0, offset);
  }

  public static TCPHeader createACK(TCPHeader header) {
    TCPHeader ret = header.reverse();
    ret.setAck_num(header.getSeq_num() + 1);
    return ret;
  }

  public static TCPHeader createSYNACK(TCPHeader header) {
    TCPHeader ret = header.reverse();
    ret.setAck_num(header.getSeq_num());
    ret.setSeq_num(200);
    return ret;
  }

  public int srcPort() {
    return srcPort;
  }

  public int dstPort() {
    return dstPort;
  }

  public int offset() {
    return offset;
  }

  @Override
  public TCPHeader reverse() {
    byte[] reverseData = Arrays.copyOfRange(data, 0, data.length);
    ByteOperations.swap(reverseData, 0, 2, 2);
    //TODO checksum
    return new TCPHeader(reverseData);
  }

  private void setAck_num(int ack) {
    byte[] bytes = ByteBuffer.allocate(4).putInt(ack).array();
    System.arraycopy(bytes, 0, data, 8, 4);
  }

  private void setSeq_num(int seq) {
    byte[] bytes = ByteBuffer.allocate(4).putInt(seq).array();
    System.arraycopy(bytes, 0, data, 8, 4);
  }

  public int getSeq_num() {
    return seq_num;
  }

  public int getAck_num() {
    return ack_num;
  }
}
