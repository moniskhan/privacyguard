package com.y59song.Network.UDP;

import android.util.Log;
import com.y59song.Network.IP.IPPayLoad;
import com.y59song.Utilities.ByteOperations;

import java.util.Arrays;

/**
 * Created by frank on 2014-03-28.
 */
public class UDPDatagram extends IPPayLoad {
  private final String TAG = "UDPDatagram";
  public static UDPDatagram create(byte[] data) {
    UDPHeader header = new UDPHeader(data);
    return new UDPDatagram(header, Arrays.copyOfRange(data, 8, header.getTotal_length()));
  }

  public UDPDatagram(UDPHeader header, byte[] data) {
    this.header = header;
    this.data = data;
    if(header.getTotal_length() != data.length + header.headerLength()) {
      header.setTotal_length(data.length + header.headerLength());
    }
  }

  public void debugInfo() {
    Log.d(TAG, "Src Port : " + header.getSrcPort());
    Log.d(TAG, "Dst Port : " + header.getDstPort());
    Log.d(TAG, "Total Length : " + ((UDPHeader)header).getTotal_length());
    Log.d(TAG, "Data Length : " + this.dataLength());
    Log.d(TAG, "Data : " + ByteOperations.byteArrayToString(this.data));
  }
}
