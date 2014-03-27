package com.y59song.Network;

import com.y59song.Utilities.ByteOperations;

import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPHeader extends AbsHeader {
  private int srcPort, dstPort, offset;

  public TCPHeader(byte[] data) {
    srcPort = (data[0] & 0xFF) << 8 + (data[1] & 0xFF);
    dstPort = (data[2] & 0xFF) << 8 + (data[3] & 0xFF);
    offset = (data[12] & 0xFF) * 4;
    checkSum_pos = 16;
    checkSum_size = 2;
    this.data = Arrays.copyOfRange(data, 0, offset);
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
  public AbsHeader reverse() {
    byte[] reverseData = Arrays.copyOfRange(data, 0, data.length);
    ByteOperations.swap(reverseData, 0, 2, 2);
    //TODO checksum
    return new TCPHeader(reverseData);
  }
}
