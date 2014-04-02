package com.y59song.Network.TCP;

import android.util.Log;
import com.y59song.Network.IPPayLoad;

import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPDatagram extends IPPayLoad {
  private static final String TAG = "TCPDatagram";

  public static TCPDatagram create(byte[] data) {
    TCPHeader header = new TCPHeader(data);
    return new TCPDatagram(header, Arrays.copyOfRange(data, header.offset(), data.length));
  }

  public TCPDatagram(TCPHeader header, byte[] data) {
    this.header = header;
    this.data = data;
    if(header.dstPort == 80 || header.srcPort == 80)
      Log.d(TAG, "Flag : " + (header.getFlag() & 0xFF) + " SrcPort : "
        + header.srcPort + " DstPort : " + header.dstPort + " Seq : " + header.getSeq_num()
        + " Ack : " + header.getAck_num()
        + " Data Length : " + dataLength());
  }

  public TCPDatagram(TCPHeader header, byte[] data, int start, int end) {
    this.header = header;
    this.data = Arrays.copyOfRange(data, start, end);
    if(header.dstPort == 80 || header.srcPort == 80)
      Log.d(TAG, "Flag : " + (header.getFlag() & 0xFF) + " SrcPort : "
        + header.srcPort + " DstPort : " + header.dstPort + " Seq : " + header.getSeq_num()
        + " Ack : " + header.getAck_num()
        + " Data Length : " + dataLength());
  }
}
