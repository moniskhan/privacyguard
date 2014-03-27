package com.y59song.Network;

import com.y59song.Utilities.ByteOperations;

/**
 * Created by frank on 2014-03-26.
 */

public abstract class IPPayLoad {
  protected AbsHeader header;
  protected byte[] data;
  public AbsHeader header() {
    return header;
  }
  public abstract int getSrcPort();
  public abstract int getDstPort();
  public abstract void update(IPHeader header);
  public byte[] toByteArray() {
    return ByteOperations.concatenate(header.toByteArray(), data);
  }
  public int length() {
    int a = header.headerLength();
    int b = data.length;
    return header.headerLength() + data.length;
  }
  protected byte[] getPseudoHeader(IPHeader ipHeader) {
    byte length = (byte)(length());
    byte[] pseudoHeader = ByteOperations.concatenate(ipHeader.getSrcAddressByteArray(),
      ipHeader.getDstAddressByteArray(), new byte[]{0, ipHeader.protocol(), 0, length});
    return pseudoHeader;
  }
}
