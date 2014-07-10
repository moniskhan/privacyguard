package com.y59song.Forwader;

import android.util.Log;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Plugin.IPlugin;
import com.y59song.Utilities.ByteOperations;
import org.sandrop.webscarab.model.ConnectionDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class MySocketForwarder extends Thread {
  private static String TAG = MySocketForwarder.class.getSimpleName();
  private static boolean DEBUG = false;
  private static boolean PROTECT = false;
  private boolean outgoing = false;
  private IPlugin plugin;
  private MyVpnService vpnService;
  private String appName = null;

  private Socket inSocket;
  private InputStream in;
  private OutputStream out;

  public static void connect(Socket clientSocket, Socket serverSocket, MyVpnService vpnService) throws Exception {
    if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()){
      clientSocket.setSoTimeout(0);
      serverSocket.setSoTimeout(0);
      MySocketForwarder clientServer = new MySocketForwarder(clientSocket, serverSocket.getOutputStream(), true, vpnService);
      MySocketForwarder serverClient = new MySocketForwarder(serverSocket, clientSocket.getOutputStream(), false, vpnService);
      clientServer.start();
      serverClient.start();

      if(DEBUG) Log.d(TAG, "Start forwarding");
      while (clientServer.isAlive())
        clientServer.join();
      while (serverClient.isAlive())
        serverClient.join();
      clientSocket.close();
      serverSocket.close();
      if(DEBUG) Log.d(TAG, "Stop forwarding");
    }else{
      if (DEBUG) Log.d(TAG, "skipping socket forwarding because of invalid sockets");
      if (clientSocket != null && clientSocket.isConnected()){
        clientSocket.close();
      }
      if (serverSocket != null && serverSocket.isConnected()){
        serverSocket.close();
      }
    }
  }

  public MySocketForwarder(Socket inSocket, OutputStream out, boolean isOutgoing, MyVpnService vpnService) {
    this.inSocket = inSocket;
    try {
      this.in = inSocket.getInputStream();
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.out = out;
    this.outgoing = isOutgoing;
    this.vpnService = vpnService;
    this.plugin = vpnService.getNewPlugin();
    setDaemon(true);
  }

  public void run() {
    try {
      byte[] buff = new byte[4096];
      int got;
      while ((got = in.read(buff)) > -1){
        String msg = new String(Arrays.copyOfRange(buff, 0, got));
        boolean ret = outgoing ? plugin.handleRequest(msg) : plugin.handleResponse(msg);
        if(DEBUG) Log.i(TAG, "" + (outgoing) + " " + got + " " + msg);
        if(ret && outgoing) {
          if(appName == null) {
            ConnectionDescriptor des = vpnService.getClientResolver().getClientDescriptorBySocket(inSocket);
            if(des != null) appName = des.getName();
          }
          Log.i("Leak Location", appName == null ? "Unknown" : appName);
          vpnService.notify(appName == null ? "Unknown" : appName);
        }
        out.write(buff, 0, got);
        out.flush();
      }
      if(DEBUG) Log.i(TAG, "" + got);
    } catch (Exception ignore) {
      ignore.printStackTrace();
    } finally {
      try {
        in.close();
        out.close();
      } catch (IOException ignore) {
        ignore.printStackTrace();
      }
    }
  }
}
