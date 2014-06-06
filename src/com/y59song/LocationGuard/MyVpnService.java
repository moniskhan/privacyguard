package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Forwader.AbsForwarder;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.IP.IPDatagram;
import com.y59song.Network.LocalServer;
import com.y59song.Utilities.MyClientResolver;
import com.y59song.Utilities.MyNetworkHostNameResolver;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;


/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable{
  private static final String TAG = "MyVpnService";
  private Thread mThread;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private FileInputStream localIn;
  private FileOutputStream localOut;
  private FileChannel inChannel, outChannel;
  private TunWriteThread writeThread;

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
    writeThread = new TunWriteThread(mInterface.getFileDescriptor());
    writeThread.start();


    try {
      while (mInterface != null && mInterface.getFileDescriptor() != null && mInterface.getFileDescriptor().valid()) {
        packet.clear();
        int length = localIn.read(packet.array());
        if(length > 0) {
          //packet.flip();
          packet.limit(length);
          Log.d(TAG, "Length : " + length);
          final IPDatagram ip = IPDatagram.create(packet);
          //packet.clear();
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

  public void fetchResponse(byte[] response) {
    writeThread.write(response);
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

  private void configure() {
    Builder b = new Builder();
    b.addAddress("10.0.0.0", 28);
    b.addRoute("173.194.43.116", 32);
    b.setMtu(1500);
    mInterface = b.establish();

    resolver = new MyNetworkHostNameResolver(this);
    clientResolver = new MyClientResolver(this);
    localServer = new LocalServer(this);
    new Thread(localServer).start();
    Dir = this.getExternalCacheDir().getAbsolutePath();

    localIn = new FileInputStream(mInterface.getFileDescriptor());
    localOut = new FileOutputStream(mInterface.getFileDescriptor());
    inChannel = localIn.getChannel();
    outChannel = localOut.getChannel();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(mInterface == null) return;
    try {
      inChannel.close();
      outChannel.close();
      mInterface.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
