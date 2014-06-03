package com.y59song.LocationGuard;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.IP.IPDatagram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
    ByteBuffer packet = ByteBuffer.allocate(2048);
    writeThread = new TunWriteThread(mInterface.getFileDescriptor());
    writeThread.start();


    try {
      while (mInterface != null && mInterface.getFileDescriptor() != null && mInterface.getFileDescriptor().valid()) {
      //while(inChannel != null && inChannel.isOpen()) {
        packet.clear();
        //int length = inChannel.read(packet);
        int length = localIn.read(packet.array());
        if(length > 0) {
          //packet.flip();
          packet.limit(length);
          final IPDatagram ip = IPDatagram.create(packet);
          //packet.clear();
          if(ip == null) continue;
          int port = ip.payLoad().getSrcPort();
          Log.d(TAG, "Getting forwarder : " + ip.header().protocol());
          forwarderPools.get(port, ip.header().protocol()).request(ip);
          Log.d(TAG, "Got it : " + ip.header().protocol());
        } else Thread.sleep(100);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void fetchResponse(byte[] response) {
    /*
    if(outChannel == null || !outChannel.isOpen() || response == null) return;
    try {
      synchronized(localOut) {
        //Log.d(TAG, "" + response.length);
        //Log.d(TAG, ByteOperations.byteArrayToHexString(response));
        outChannel.write(ByteBuffer.wrap(response));
        //localOut.write(response);
        localOut.flush();
        //localOut.getFD().sync();
        //Log.d(TAG, "" + response.length);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    */
    writeThread.write(response);
  }

  public ForwarderPools getForwarderPools() {
    return forwarderPools;
  }

  private void configure() {
    Builder b = new Builder();
    b.addAddress("10.0.0.0", 28);
    //b.addAddress(getLocalAddress(), 28);
    //b.addRoute("0.0.0.0", 0);
    //b.addRoute("8.8.8.8", 32);
    //b.addDnsServer("8.8.8.8");
    //b.addRoute("220.181.37.55", 32);
    b.addRoute("173.194.43.116", 32);
    //b.addRoute("71.19.173.0", 24);
    b.setMtu(3000);
    mInterface = b.establish();
    localIn = new FileInputStream(mInterface.getFileDescriptor());
    localOut = new FileOutputStream(mInterface.getFileDescriptor());
    inChannel = localIn.getChannel();
    outChannel = localOut.getChannel();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "destroy");
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
