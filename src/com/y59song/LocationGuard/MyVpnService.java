package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.LocalServer;
import com.y59song.Utilities.MyClientResolver;
import com.y59song.Utilities.MyNetworkHostNameResolver;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;


/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = MyVpnService.class.getSimpleName();
  private Thread mThread;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private TunWriteThread writeThread;
  private TunReadThread readThread;

  //Pools
  private ForwarderPools forwarderPools;

  //SSL stuff
  private String Dir;
  public static final String CAName = "/LocationGuard_CA";
  public static final String CertName = "/LocationGuard_Cert";
  public static final String KeyType = "PKCS12";
  public static final String Password = "";
  private SSLSocketFactoryFactory sslSocketFactoryFactory;

  //Network
  private MyNetworkHostNameResolver resolver;
  private MyClientResolver clientResolver;
  private LocalServer localServer;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mThread != null) mThread.interrupt();
    mThread = new Thread(this, getClass().getSimpleName());
    mThread.start();
    forwarderPools = new ForwarderPools(this);
    return 0;
  }

  @Override
  public void run() {
    setup_network();
    setup_workers();
    wait_to_close();
  }

  public void fetchResponse(byte[] response) {
    writeThread.write(response);
  }

  private void setup_network() {
    Builder b = new Builder();
    b.addAddress("10.0.0.0", 28);
    //b.addRoute("173.194.43.116", 32);
    //b.addRoute("192.30.252.130", 32);
    b.addRoute("72.21.215.233", 32);
    //b.addRoute("0.0.0.0", 0);
    b.setMtu(1500);
    mInterface = b.establish();
  }

  private void setup_workers() {
    resolver = new MyNetworkHostNameResolver(this);
    clientResolver = new MyClientResolver(this);
    Dir = this.getExternalCacheDir().getAbsolutePath();
    try {
      sslSocketFactoryFactory = new SSLSocketFactoryFactory(Dir + CAName, Dir + CertName, KeyType, Password.toCharArray());
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    localServer = new LocalServer(this);
    localServer.start();
    readThread = new TunReadThread(mInterface.getFileDescriptor(), this);
    readThread.start();
    writeThread = new TunWriteThread(mInterface.getFileDescriptor(), this);
    writeThread.start();
  }

  private void wait_to_close() {
    try {
      while(writeThread.isAlive())
        writeThread.join();

      while(readThread.isAlive())
        readThread.join();

      while(localServer.isAlive())
        localServer.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(mInterface == null) return;
    try {
      readThread.interrupt();
      writeThread.interrupt();
      localServer.interrupt();
      mInterface.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SSLSocketFactoryFactory getSSlSocketFactoryFactory() {
    return sslSocketFactoryFactory;
  }

  public MyNetworkHostNameResolver getResolver() {
    return resolver;
  }

  public MyClientResolver getClientResolver() {
    return clientResolver;
  }

  public ForwarderPools getForwarderPools() {
    return forwarderPools;
  }
}
