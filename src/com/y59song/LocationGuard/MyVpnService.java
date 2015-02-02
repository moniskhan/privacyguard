/*
 * Vpnservice, build the virtual network interface
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.y59song.LocationGuard;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.security.KeyChain;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.y59song.Forwader.ForwarderPools;
import com.y59song.Network.LocalServer;
import com.y59song.Plugin.ContactDetection;
import com.y59song.Plugin.IPlugin;
import com.y59song.Plugin.LocationDetection;
import com.y59song.Plugin.PhoneStateDetection;
import com.y59song.Utilities.Certificate.CertificateManager;
import com.y59song.Utilities.Resolver.MyClientResolver;
import com.y59song.Utilities.Resolver.MyNetworkHostNameResolver;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import javax.security.cert.CertificateEncodingException;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable {
  private static final String TAG = MyVpnService.class.getSimpleName();
  private static final boolean DEBUG = true;
  private static int mId = 0;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private TunWriteThread writeThread;
  private TunReadThread readThread;
  private Thread uiThread;

  //Pools
  private ForwarderPools forwarderPools;

  //SSL stuff
  private SSLSocketFactoryFactory sslSocketFactoryFactory;
  protected static final String CAName = "LocationGuard_CA";
  protected static final String CertName = "LocationGuard_Cert";
  protected static final String KeyType = "PKCS12";
  protected static final String Password = "";

  //Network
  private MyNetworkHostNameResolver hostNameResolver;
  private MyClientResolver clientAppResolver;
  private LocalServer localServer;

  // Plugin
  private Class pluginClass[] = {LocationDetection.class, PhoneStateDetection.class, ContactDetection.class};

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    uiThread = new Thread(this);
    uiThread.start();
    return 0;
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

  @Override
  public void run() {
    setup_network();
    setup_workers();
    wait_to_close();
  }



  private void setup_network() {
    Builder b = new Builder();
    b.addAddress("10.8.0.1", 32);
    b.addDnsServer("8.8.8.8");
    b.addRoute("0.0.0.0", 0);
    //b.addRoute("129.97.171.63", 32);
    b.setMtu(1500);
    mInterface = b.establish();
    forwarderPools = new ForwarderPools(this);
    sslSocketFactoryFactory = CertificateManager.generateCACertificate(this.getCacheDir().getAbsolutePath(), CAName,
      CertName, KeyType, Password.toCharArray());
  }

  private void setup_workers() {
    hostNameResolver = new MyNetworkHostNameResolver(this);
    clientAppResolver = new MyClientResolver(this);

    localServer = new LocalServer(this);
    localServer.start();
    readThread = new TunReadThread(mInterface.getFileDescriptor(), this);
    readThread.start();
    writeThread = new TunWriteThread(mInterface.getFileDescriptor(), this);
    writeThread.start();
  }

  private void wait_to_close() {
    // wait until all threads stop
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

  public void fetchResponse(byte[] response) {
    writeThread.write(response);
  }

  public SSLSocketFactoryFactory getSSlSocketFactoryFactory() {
    return sslSocketFactoryFactory;
  }

  public MyNetworkHostNameResolver getHostNameResolver() {
    return hostNameResolver;
  }

  public MyClientResolver getClientAppResolver() {
    return clientAppResolver;
  }

  public ForwarderPools getForwarderPools() {
    return forwarderPools;
  }

  public ArrayList<IPlugin> getNewPlugins() {
    ArrayList<IPlugin> ret = new ArrayList<IPlugin>();
    try {
      for(Class c : pluginClass) {
        IPlugin temp = (IPlugin)c.newInstance();
        temp.setContext(this);
        ret.add(temp);
      }
      return ret;
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void notify(String msg) {
    NotificationCompat.Builder mBuilder =
      new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Location Guard")
        .setContentText(msg);
    if(DEBUG) Log.i(TAG, msg);
    // Creates an explicit intent for an Activity in your app
    Intent resultIntent = new Intent(this, LocationGuard.class);

    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of
    // your application to the Home screen.
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(LocationGuard.class);
    // Adds the Intent that starts the Activity to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
      stackBuilder.getPendingIntent(
        0,
        PendingIntent.FLAG_UPDATE_CURRENT
      );
    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    // mId allows you to update the notification later on.
    mNotificationManager.notify(mId ++, mBuilder.build());
  }
}
