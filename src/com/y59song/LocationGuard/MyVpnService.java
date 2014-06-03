package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Forwader.AbsForwarder;
import com.y59song.Forwader.ForwarderBuilder;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.LocalServer;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;
import org.sandroproxy.utils.NetworkHostNameResolver;
import org.sandroproxy.utils.network.ClientResolver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.HashMap;

/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = "MyVpnService";
  private Thread mThread;
  public static HashMap<Integer, AbsForwarder> portToForwarder;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private FileInputStream localIn;
  private FileOutputStream localOut;

  //SSL stuff
  private String Dir;
  public static final String CAName = "/LocationGuard_CA";
  public static final String CertName = "/LocationGuard_Cert";
  public static final String KeyType = "PKCS12";
  public static final String Password = "";
  private SSLSocketFactoryFactory sslSocketFactoryFactory;

  //Network
  private NetworkHostNameResolver resolver;
  private ClientResolver clientResolver;
  private LocalServer localServer;

  public MyVpnService() {
    Dir = this.getExternalCacheDir().getAbsolutePath();
    resolver = new NetworkHostNameResolver(this);
    clientResolver = new ClientResolver(this);
    localServer = new LocalServer(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mThread != null) mThread.interrupt();
    mThread = new Thread(this, getClass().getSimpleName());
    mThread.start();
    portToForwarder = new HashMap<Integer, AbsForwarder>();
    return 0;
  }

  @Override
  public void run() {
    configure();
    localIn = new FileInputStream(mInterface.getFileDescriptor());
    localOut = new FileOutputStream(mInterface.getFileDescriptor());
    try {
      sslSocketFactoryFactory = new SSLSocketFactoryFactory(Dir + CAName, Dir + CertName, KeyType, Password.toCharArray());
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    ByteBuffer packet = ByteBuffer.allocate(2048);

    try {
      while (mInterface != null && mInterface.getFileDescriptor() != null && mInterface.getFileDescriptor().valid()) {
        int length = localIn.read(packet.array());
        if(length > 0) {
          packet.limit(length);
          final IPDatagram ip = IPDatagram.create(packet);
          packet.clear();
          if(ip == null) continue;
          int port = ip.payLoad().getSrcPort();
          AbsForwarder temp;
          if(!portToForwarder.containsKey(port)) {
            temp = ForwarderBuilder.build(ip.header().protocol(), this);
            portToForwarder.put(port, temp);
          } else temp = portToForwarder.get(port);
          temp.request(ip);
        } else Thread.sleep(100);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public synchronized void fetchResponse(byte[] response) {
    if(localOut == null || response == null) return;
    try {
      if(LocationGuard.debug) Log.d(TAG, "" + response.length);
      localOut.write(response);
      localOut.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SSLSocketFactoryFactory getSSlSocketFactoryFactory() {
    return sslSocketFactoryFactory;
  }

  public NetworkHostNameResolver getResolver() {
    return resolver;
  }

  public ClientResolver getClientResolver() {
    return clientResolver;
  }

  private void configure() {
    Builder b = new Builder();
    b.addAddress("10.0.0.0", 28);
    b.addRoute("0.0.0.0", 0);
    //b.addRoute("173.194.43.0", 24);
    b.setMtu(1500);
    mInterface = b.establish();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(mInterface == null) return;
    try {
      mInterface.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
