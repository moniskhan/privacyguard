package com.y59song.Utilities;

/**
 * Created by frank on 2014-03-27.
 */
public class ByteOperations {
  public static byte[] concatenate(byte[]...arrays) {
    int totalLength = 0;
    for(byte[] array : arrays) totalLength += array.length;
    byte[] result = new byte[totalLength];

    int currentIndex = 0;
    for(byte[] array : arrays) {
      System.arraycopy(array, 0, result, currentIndex, array.length);
      currentIndex += array.length;
    }
    return result;
  }

  public static void swap(byte[] array, int pos1, int pos2, int length) {
    for(int i = 0; i < length; i ++) {
      byte temp = array[pos1 + i];
      array[pos1 + i] = array[pos2 + i];
      array[pos2 + i] = temp;
    }
  }

  public static byte[] computeCheckSum(byte[] data) {
    int result = 0;
    if(data.length % 2 != 0) result = data[data.length - 1] << 8;
    for(int i = 0; i < data.length / 2; i ++) {
      result += ((data[2 * i] & 0xFF) << 8) + (data[2 * i + 1] & 0xFF);
      int carry = result >> 16;
      result &= 0xFFFF;
      result += carry;
      //System.out.println(Integer.toHexString(result));
    }
    result = ~result;
    return new byte[]{(byte)((result >> 8) & 0xFF), (byte)(result & 0xFF)};
  }
}
