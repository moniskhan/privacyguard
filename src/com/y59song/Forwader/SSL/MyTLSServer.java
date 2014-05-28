package com.y59song.Forwader.SSL;

import org.bouncycastle.crypto.tls.DefaultTlsEncryptionCredentials;
import org.bouncycastle.crypto.tls.DefaultTlsServer;

import javax.net.ssl.SSLContext;
j
/**
 * Created by y59song on 27/05/14.
 */
public class MyTLSServer extends DefaultTlsServer {
  private DefaultTlsEncryptionCredentials credentials;

  public MyTLSServer(SSLContext contect) {
    super();
  }
}
