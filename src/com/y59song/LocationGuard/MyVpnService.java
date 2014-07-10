package com.y59song.LocationGuard;

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
import com.y59song.Plugin.IPlugin;
import com.y59song.Plugin.LocationDetection;
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
  private static final boolean DEBUG = false;
  private static final int mId = 2009;
  private Thread mThread;

  //The virtual network interface, get and return packets to it
  private ParcelFileDescriptor mInterface;
  private TunWriteThread writeThread;
  private TunReadThread readThread;

  //Pools
  private ForwarderPools forwarderPools;

  //SSL stuff

  private SSLSocketFactoryFactory sslSocketFactoryFactory;

  //Network
  private MyNetworkHostNameResolver resolver;
  private MyClientResolver clientResolver;
  private LocalServer localServer;

  // Plugin
  private Class pluginClass = LocationDetection.class;

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
    b.addAddress("10.8.0.1", 32);
    b.addDnsServer("8.8.8.8");
    b.addRoute("8.8.8.8", 32);
    /*
    b.addRoute("71.19.173.0", 24);
    b.addRoute("74.216.233.0", 24);
    b.addRoute("74.125.226.0", 24);
    b.addRoute("173.194.43.0", 24);
    */
    //b.addRoute("173.194.43.116", 32);
    b.addRoute("0.0.0.0", 0);
    b.setMtu(1500);
    mInterface = b.establish();
  }

  private void setup_workers() {
    resolver = new MyNetworkHostNameResolver(this);
    clientResolver = new MyClientResolver(this);
    String Dir = this.getExternalFilesDir(null).getAbsolutePath();
    try {
      sslSocketFactoryFactory = new SSLSocketFactoryFactory(Dir + LocationGuard.CAName,
        Dir + LocationGuard.CertName,
        LocationGuard.KeyType,
        LocationGuard.Password.toCharArray());
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

  public IPlugin getNewPlugin() {
    try {
      IPlugin plugin = (IPlugin)pluginClass.newInstance();
      plugin.setContext(this);
      return plugin;
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void notify(String name) {
    NotificationCompat.Builder mBuilder =
      new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Location Guard")
        .setContentText(name + " is leaking location");
    if(DEBUG) Log.i(TAG, name);
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
    mNotificationManager.notify(mId, mBuilder.build());
  }
}
