package com.y59song.Forwader.SSL;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.DefaultTlsEncryptionCredentials;
import org.bouncycastle.crypto.tls.TlsContext;

/**
 * Created by y59song on 27/05/14.
 */
public class MyTLSCredential extends DefaultTlsEncryptionCredentials {

  public MyTLSCredential(TlsContext tlsContext, Certificate certificate, AsymmetricKeyParameter asymmetricKeyParameter) {
    super(tlsContext, certificate, asymmetricKeyParameter);
  }
}
