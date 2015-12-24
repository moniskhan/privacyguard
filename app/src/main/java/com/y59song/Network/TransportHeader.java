package com.y59song.Network;

/**
 * Created by frank on 2014-03-28.
 */
public abstract class TransportHeader extends AbsHeader {
  protected int srcPort, dstPort;
  public TransportHeader(byte []data) {
    srcPort = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
    dstPort = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
  }
  @Override
  public abstract TransportHeader reverse();
  public int getSrcPort() {
    return srcPort;
  };
  public int getDstPort() {
    return dstPort;
  }
}
