package com.y59song.Network;

import com.y59song.Utilities.ByteOperations;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class IPHeader extends AbsHeader {
  private int headerLength, length;
  private InetAddress srcAddress, dstAddress;
  private byte protocol = 0;

  public IPHeader(byte[] data) {
    headerLength = (data[0] & 0xFF) % 16 * 4;
    length = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
    protocol = data[9];
    try {
      srcAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, 12, 16));
      dstAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, 16, 20));
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    checkSum_pos = 10;
    checkSum_size = 2;
    this.data = Arrays.copyOfRange(data, 0, headerLength);
  }

  public int length() {
    return length;
  }

  public void setLength(int l) {
    length = l;
    data[2] = (byte)(l >> 8);
    data[3] = (byte)(l % 256);
  }

  public byte protocol() {
    return protocol;
  }

  public InetAddress getSrcAddress() {
    return srcAddress;
  }
  public InetAddress getDstAddress() {
    return dstAddress;
  }

  public byte[] getSrcAddressByteArray() {
    return Arrays.copyOfRange(data, 12, 4);
  }

  public byte[] getDstAddressByteArray() {
    return Arrays.copyOfRange(data, 16, 4);
  }

  @Override
  public IPHeader reverse() {
    byte[] reverseData = Arrays.copyOfRange(data, 0, data.length);
    ByteOperations.swap(reverseData, 12, 16, 4);
    return null;
  }
}
