package com.y59song.Utilities;

import android.util.Log;
import com.y59song.Network.SSL.SSLData;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.crypto.tls.Certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by y59song on 26/05/14.
 */
public class SSLParser {
  private static final String TAG = "SSL Parser";
  public static SSLData handshake(byte[] packet, SSLData data) {
    if(data == null) data = new SSLData();
    
    return data;
  }

  public static byte[] certificate(byte[] sslpacket) {

  }

  public static Certificate getCertificate(byte[] sslpacket){
    ByteArrayInputStream bis = new ByteArrayInputStream(sslpacket);
    Certificate cert = null;
    try {
      cert = Certificate.parse(bis);
      if(cert.getCertificateList().length <= 0) cert = null;
      else {
        Log.d(TAG, IETFUtils.valueToString(cert.getCertificateAt(0).getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return cert;
  }
}
