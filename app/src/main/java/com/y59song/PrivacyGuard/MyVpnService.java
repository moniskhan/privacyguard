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

package com.y59song.PrivacyGuard;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable, Notifications {
  private static final String TAG = MyVpnService.class.getSimpleName();
  private static final boolean DEBUG = true;
  private static int mId = 0;
  private static HashMap<String, Integer[]> notificationMap = new HashMap<String, Integer[]>();

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private TunWriteThread writeThread;
  private TunReadThread readThread;
  private Thread uiThread;

  //Pools
  private ForwarderPools forwarderPools;

  //SSL stuff
  private SSLSocketFactoryFactory sslSocketFactoryFactory;
  protected static final String CAName = "PrivacyGuard_CA";
  protected static final String CertName = "PrivacyGuard_Cert";
  protected static final String KeyType = "PKCS12";
  protected static final String Password = "";

  public final static String EXTRA_DATA = "com.y59song.PrivacyGuard.DATA";
  public final static String EXTRA_APP = "com.y59song.PrivacyGuard.APP";
  public final static String EXTRA_SIZE = "com.y59song.PrivacyGuard.SIZE";

  //Network
  private MyNetworkHostNameResolver hostNameResolver;
  private MyClientResolver clientAppResolver;
  private LocalServer localServer;

  // Plugin
  private Class pluginClass[] = {LocationDetection.class, PhoneStateDetection.class, ContactDetection.class};

  // Other
  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

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

  ////////////////////////////////////////////////////
  // Notification Methods
  ///////////////////////////////////////////////////

  public int findNotificationId(String appName, String msg){
    DatabaseHandler db = new DatabaseHandler(this);
    List<DataLeak> leakList = db.getAppLeaks(appName);
    msg = msg.replace("is leaking", "");
    int endIndex = msg.lastIndexOf(":");
    if (endIndex != -1) {
      msg = msg.substring(0, endIndex-1);
    }
    msg = msg.trim();
    for (int i =0; i < leakList.size(); i++) {
      if (leakList.get(i).getLeakType().equals(msg)) {
        return leakList.get(i).getID();
      }
    }

    return -1;
  }

  public int findGeneralNotificationId(String appName){
    DatabaseHandler db = new DatabaseHandler(this);
    List<DataLeak> leakList = db.getAppLeaks(appName);

    for (int i = 0; i < leakList.size(); i++) {
      if (leakList.get(i) != null) {
        return leakList.get(i).getID();
      }
    }

    return -1;
  }

  public int findNotificationCounter(String appName, String msg){
    DatabaseHandler db = new DatabaseHandler(this);
    List<DataLeak> leakList = db.getAppLeaks(appName);
    msg = msg.replace("is leaking", "");
    int endIndex = msg.lastIndexOf(":");
    if (endIndex != -1) {
      msg = msg.substring(0, endIndex-1);
    }
    msg = msg.trim();
    for (int i =0; i < leakList.size(); i++) {
      if (leakList.get(i).getLeakType().equals(msg)) {
        return leakList.get(i).getFrequency();
      }
    }

    return -1;
  }

  public boolean isIgnored(String appName, String msg){

    DatabaseHandler db = new DatabaseHandler(this);
    List<DataLeak> leakList = db.getAppLeaks(appName);
    msg = msg.replace("is leaking", "");
    int endIndex = msg.lastIndexOf(":");
    if (endIndex != -1) {
      msg = msg.substring(0, endIndex-1);
    }
    msg = msg.trim();
    for (int i =0; i < leakList.size(); i++) {
      if (leakList.get(i).getLeakType().equals(msg) && leakList.get(i).getIgnore() != 0) {
        return true;
      }
    }

    return false;
  }

  public boolean isLocation(String appName, String msg){

    DatabaseHandler db = new DatabaseHandler(this);

    msg = msg.replace("is leaking", "");
    int endIndex = msg.lastIndexOf(":");
    if (endIndex != -1) {
      String location = msg.substring(endIndex + 1);
      msg = msg.substring(0, endIndex);
      msg = msg.trim();

      if (msg.equals("Location")) {
        db.addLocationLeak(new LocationLeak(mId, appName, location, dateFormat.format(new Date())));
        return true;
      }

    }
    return false;
  }

  @Override
  public void notify(String appName, String msg) {
    isLocation(appName, msg);

    boolean ignored = false;

    if (isIgnored(appName, msg)) {
      ignored = true;
    }

    if (findNotificationId(appName, msg) >= 0){
      updateNotification(appName, msg);
      return;
    }

    // -----------------------------------------------------------------------
    // Database Entry
    DatabaseHandler db = new DatabaseHandler(this);
    db.addLeak(new DataLeak(mId, appName, msg, 1, dateFormat.format(new Date())));

    // -----------------------------------------------------------------------

    if (ignored) {
      return;
    }

    int notifyId = findNotificationId(appName, msg);
    int generalNotifyId = findGeneralNotificationId(appName);

    NotificationCompat.Builder mBuilder =
      new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.notify)
        .setContentTitle("Privacy Guard")
        .setTicker(appName + " " + msg)
        .setContentText(appName + " " + msg);
    if(DEBUG) Log.i(TAG, msg);

    // Creates an explicit intent for an Activity in your app
    Intent resultIntent = new Intent(this, DetailsActivity.class);

    List<LocationLeak> leakList = db.getLocationLeaks(appName);
    resultIntent.putExtra(EXTRA_APP, appName);
    resultIntent.putExtra(EXTRA_SIZE, String.valueOf(leakList.size()));
    for (int i = 0; i < leakList.size(); i++) {
      resultIntent.putExtra(EXTRA_DATA + i, leakList.get(i).getLocation()); // to pass values between activities
    }

    // ------------------------------------------------
    Intent intent = new Intent(this, ActionReceiver.class);
    intent.setAction("Ignore");
    intent.putExtra("appName", appName);
    intent.putExtra("notificationId", notifyId);

    // use System.currentTimeMillis() to have a unique ID for the pending intent
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.addAction(R.drawable.ignore, "Ignore", pendingIntent);
    // ------------------------------------------------

    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of home screen
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(DetailsActivity.class);
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

    // mId updates the notification later on.
    mNotificationManager.notify(generalNotifyId, mBuilder.build());
    mId++;
  }

  @Override
  public void updateNotification(String appName, String msg){
    DatabaseHandler db = new DatabaseHandler(this);

    boolean ignored = false;

    if (isIgnored(appName, msg)) {
      ignored = true;
    }

    NotificationManager mNotificationManager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    int notifyId = findNotificationId(appName, msg);
    int generalNotifyId = findGeneralNotificationId(appName);

    NotificationCompat.Builder mNotifyBuilder =
      new NotificationCompat.Builder(this)
        .setContentText("Number of leaks: " + findNotificationCounter(appName, msg))
        .setContentTitle(appName + " " + msg)
        .setTicker(appName + " " + msg)
        .setSmallIcon(R.drawable.notify);

    // ------------------------------------------------------------------------------------
    // Currently initiates a new activity instance each time, should recycle if already open
    // Creates an explicit intent for an Activity in your app
    Intent resultIntent = new Intent(this, DetailsActivity.class); // should actually be DetailsActivity.class

    List<LocationLeak> leakList = db.getLocationLeaks(appName);
    resultIntent.putExtra(EXTRA_APP, appName);
    resultIntent.putExtra(EXTRA_SIZE, String.valueOf(leakList.size()));
    for (int i = 0; i < leakList.size(); i++) {
      resultIntent.putExtra(EXTRA_DATA + i, leakList.get(i).getLocation()); // to pass values between activities
    }


    // The stack builder object will contain an artificial back stack for the
    // started Activity.
    // This ensures that navigating backward from the Activity leads out of
    // your application to the Home screen.
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack for the Intent (but not the Intent itself)
    stackBuilder.addParentStack(DetailsActivity.class);
    // Adds the Intent that starts the Activity to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
    mNotifyBuilder.setContentIntent(resultPendingIntent);

    // ------------------------------------------------------------------------------------
    // Creates an explicit intent for an Activity in your app
    Intent intent = new Intent(this, ActionReceiver.class);
    intent.setAction("Ignore");
    intent.putExtra("appName", appName);
    intent.putExtra("notificationId", notifyId);

    // use System.currentTimeMillis() to have a unique ID for the pending intent
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mNotifyBuilder.addAction(R.drawable.ignore, "Ignore", pendingIntent);

    // ------------------------------------------------------------------------------------
    // Set Notification Style to Expanded
    NotificationCompat.InboxStyle inboxStyle =
            new NotificationCompat.InboxStyle();

    String[] events = new String[findNotificationCounter(appName, msg)];
    if (generalNotifyId >= 0 && !ignored){
      // Because the ID remains unchanged, the existing notification is updated.
      Log.i(TAG, "NOTIFYID IS SUCCESSFULL" + notifyId);
      mNotificationManager.notify(
              generalNotifyId,
              mNotifyBuilder.build());
    } else {
      Log.i(TAG, "NOTIFYID IS FAILING" + notifyId);
    }

    // -----------------------------------------------------------------------
    // Database Entry

    db.addLeak(new DataLeak(notifyId, appName, msg, events.length, dateFormat.format(new Date())));

    // -----------------------------------------------------------------------
  }

  @Override
  public void deleteNotification(int id){
    NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    mNotificationManager.cancel(id);
  }
}
