package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Utilities.ByteOperations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MySocketForwarder extends Thread {
  private static String TAG = MySocketForwarder.class.getSimpleName();
  private static boolean DEBUG = false;

  private InputStream in;
  private OutputStream out;

  public static void connect(String name, Socket clientSocket, Socket serverSocket) throws Exception {
    if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()){
      clientSocket.setSoTimeout(0);
      serverSocket.setSoTimeout(0);
      MySocketForwarder clientServer = new MySocketForwarder(name + "_clientServer", clientSocket.getInputStream(), serverSocket.getOutputStream());
      MySocketForwarder serverClient = new MySocketForwarder(name + "_serverClient", serverSocket.getInputStream(), clientSocket.getOutputStream());
      clientServer.start();
      serverClient.start();

      Log.d(TAG, "Start forwarding");
      while (clientServer.isAlive())
        clientServer.join();
      while (serverClient.isAlive())
        serverClient.join();
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

  public MySocketForwarder(String name, InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
    setName(name);
    setDaemon(true);
  }

  public void run() {
    try {
      byte[] buff = new byte[4096];
      int got;
      Log.d(TAG, "Running");
      while ((got = in.read(buff)) > -1){
        if(DEBUG) Log.d(TAG + getName(), ByteOperations.byteArrayToString(buff, 0, got));
        out.write(buff, 0, got);
        out.flush();
      }
    } catch (Exception ignore) {
    } finally {
      try {
        in.close();
      } catch (IOException ignore) {
        ignore.printStackTrace();
      }
      try {
        out.close();
      } catch (IOException ignore) {
        ignore.printStackTrace();
      }
    }
  }

}
