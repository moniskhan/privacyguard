package com.y59song.Forwader;

import android.util.Log;
import com.y59song.Utilities.ByteOperations;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.PcapWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by y59song on 05/06/14.
 */
public class MySocketForwarder extends Thread {

  private static String TAG = MySocketForwarder.class.getSimpleName();
  private static boolean LOGD = false;

  private InputStream in;
  private OutputStream out;
  private PcapWriter pcapWriter;
  private boolean flip;


  public static void connect(String name, Socket clientSocket, Socket serverSocket, boolean captureAsPcap, File storageDir, ConnectionDescriptor connDesc) throws Exception {
    if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()){
      clientSocket.setSoTimeout(0);
      serverSocket.setSoTimeout(0);
      // we create pcapWriter and pass it to threads to write on
      PcapWriter pcapWriter = null;
      if (captureAsPcap && storageDir != null){
        String storageFile = storageDir.getAbsolutePath();
        String uid = "";
        if (connDesc != null){
          uid = connDesc.getId() + "_" + connDesc.getNamespace();
        }
        String pcapFileName = storageFile + "/" + name + "_" + uid + "_" + System.currentTimeMillis() +  ".pcap";
        pcapFileName = pcapFileName.replace("*", "_").replace(":", "_");
        pcapWriter = new PcapWriter(clientSocket, serverSocket, pcapFileName);
      }
      // we could also pass OutputStream on which wireshark listens
      MySocketForwarder clientServer = new MySocketForwarder(name + "_clientServer", clientSocket.getInputStream(), serverSocket.getOutputStream(), pcapWriter, false);
      MySocketForwarder serverClient = new MySocketForwarder(name + "_serverClient", serverSocket.getInputStream(), clientSocket.getOutputStream(), pcapWriter, true);
      clientServer.start();
      serverClient.start();

      Log.d(TAG, "Start forwarding");

      while (clientServer.isAlive()) {
        try {
          clientServer.join();
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
      }
      while (serverClient.isAlive()) {
        try {
          serverClient.join();
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
      }
    }else{
      if (LOGD) Log.d(TAG, "skipping socket forwarding because of invalid sockets");
      if (clientSocket != null && clientSocket.isConnected()){
        clientSocket.close();
      }
      if (serverSocket != null && serverSocket.isConnected()){
        serverSocket.close();
      }
    }

  }

  public MySocketForwarder(String name, InputStream in, OutputStream out, PcapWriter pcapWriter, boolean flip) {
    this.in = in;
    this.out = out;
    this.pcapWriter = pcapWriter;
    this.flip = flip;
    setName(name);
    setDaemon(true);
  }

  public void run() {
    try {
      byte[] buff = new byte[4096];
      int got;
      Log.d(TAG, "Running");
      while ((got = in.read(buff)) > -1){
        Log.d(TAG + getName(), ByteOperations.byteArrayToString(buff, 0, got));
        out.write(buff, 0, got);
        out.flush();
        if (pcapWriter != null){
          byte[] readData = new byte[got];
          System.arraycopy(buff, 0, readData, 0, got);
          pcapWriter.writeData(readData, System.currentTimeMillis() * 1000, flip);
        }
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
