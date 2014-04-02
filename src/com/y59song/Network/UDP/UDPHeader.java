package com.y59song.Network.UDP;

import com.y59song.Network.TransportHeader;
import com.y59song.Utilities.ByteOperations;

import java.util.Arrays;

/**
 * Created by frank on 2014-03-28.
 */
public class UDPHeader extends TransportHeader {
  public UDPHeader(byte[] data) {
    super(data);
    this.data = Arrays.copyOfRange(data, 0, 8);
    this.checkSum_size = 2;
    this.checkSum_pos = 6;
  }

  @Override
  public UDPHeader reverse() {
    byte[] reverseData = Arrays.copyOfRange(data, 0, data.length);
    ByteOperations.swap(reverseData, 0, 2, 2);
    return new UDPHeader(reverseData);
  }
}
