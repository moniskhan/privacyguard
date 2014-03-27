package com.y59song.Network;

import android.util.Log;
import com.y59song.Utilities.ByteOperations;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */

public class IPDatagram {
  public final static String TAG = "IPDatagram";
  IPHeader header;
  IPPayLoad data;

  public static final int TCP = 6, UDP = 17;
  public static IPDatagram create(ByteBuffer packet) {
    byte[] data = packet.array();
    IPHeader header = new IPHeader(Arrays.copyOfRange(data, 0, 60));
    IPPayLoad payLoad = null;
    Log.d(TAG, "protocol : " + (int)header.protocol());
    Log.d(TAG, "length : " + header.length());
    Log.d(TAG, "source address : " + header.getSrcAddress().getHostAddress());
    Log.d(TAG, "destination address : " + header.getDstAddress().getHostAddress());
    Log.d(TAG, "data : " + (data == null));
    if(header.protocol() == TCP)
      payLoad = TCPDatagram.create(Arrays.copyOfRange(data, header.headerLength(), data.length));
    else return null;
    return new IPDatagram(header, payLoad);
  }

  public IPDatagram(IPHeader header, IPPayLoad data) {
    this.header = header;
    this.data = data;
    int totalLength = header.headerLength() + data.length();
    if(this.header.length() != totalLength) {
      this.header.setLength(totalLength);
      this.header.setCheckSum(new byte[] {0, 0});
      byte[] toComputeCheckSum = this.header.toByteArray();
      this.header.setCheckSum(ByteOperations.computeCheckSum(toComputeCheckSum));
    }
  }

  public IPHeader header() {
    return header;
  }

  public IPPayLoad payLoad() {
    return data;
  }

  public byte[] toByteArray() {
    return ByteOperations.concatenate(header.toByteArray(), data.toByteArray());
  }
}
