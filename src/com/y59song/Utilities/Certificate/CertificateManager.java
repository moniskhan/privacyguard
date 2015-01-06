package com.y59song.Utilities.Certificate;

import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by Near on 15-01-06.
 */
public class CertificateManager {
  // generate CA certificate but return a ssl socket factory factory which use this certificate
  public static SSLSocketFactoryFactory generateCACertificate(String dir, String caName, String certName, String KeyType, char[] password) {
    try {
      return new SSLSocketFactoryFactory(dir + "/" + caName, dir + "/" + certName, KeyType, password);
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // get the CA certificate by the path
  public static X509Certificate getCACertificate(String dir, String caName) {
    String CERT_FILE = dir + "/" + caName + "_export.crt";
    File certFile = new File(CERT_FILE);
    FileInputStream certIs = null;
    try {
      certIs = new FileInputStream(CERT_FILE);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    byte [] cert = new byte[(int)certFile.length()];
    try {
      certIs.read(cert);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      return X509Certificate.getInstance(cert);
    } catch (CertificateException e) {
      e.printStackTrace();
      return null;
    }
  }
}
