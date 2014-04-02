package com.y59song.Network.UDP;

import com.y59song.Network.IPPayLoad;

import java.util.Arrays;

/**
 * Created by frank on 2014-03-28.
 */
public class UDPDatagram extends IPPayLoad {
  public static UDPDatagram create(byte[] data) {
    UDPHeader header = new UDPHeader(data);
    return new UDPDatagram(header, Arrays.copyOfRange(data, 8, data.length));
  }

  public UDPDatagram(UDPHeader header, byte[] data) {
    this.header = header;
    this.data = data;
  }
}
