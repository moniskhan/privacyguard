package com.y59song.Network;

import android.util.Log;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.SSLSocketBuilder;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;
import org.sandrop.webscarab.plugin.proxy.SocketForwarder;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer implements Runnable {
  private static final boolean DEBUG = true;
  private static final String TAG = "LocalServer";
  public static final String addr = "10.0.0.1";
  public static final int port = 8010;
  public static final int SSLPort = 443;

  private ServerSocket serverSocket;
  private boolean isStop = false;
  private MyVpnService vpnService;
  public LocalServer(MyVpnService vpnService) {
    if(serverSocket == null || serverSocket.isClosed())
      try {
        listen();
      } catch (IOException e) {
        if(DEBUG) Log.d(TAG, "Listen error");
        e.printStackTrace();
      }
    this.vpnService = vpnService;
  }

  private void listen() throws IOException {
    serverSocket = new ServerSocket(port, 5, InetAddress.getByName(addr));
  }

  @Override
  public void run() {
    while(!isStop) {
      try {
        Socket socket = serverSocket.accept();
        ConnectionDescriptor descriptor = vpnService.getClientResolver().getClientDescriptorBySocket(socket);
        Socket target;
        if(descriptor.getRemotePort() == SSLPort) {
          SiteData remoteData = vpnService.getResolver().getSecureHost(socket, descriptor.getRemotePort(), true);
          socket = SSLSocketBuilder.negotiateSSL(socket, remoteData, false, vpnService.getSSlSocketFactoryFactory());
          target = SSLSocketFactory.getDefault().createSocket(descriptor.getRemoteHostName(), descriptor.getRemotePort());
        } else {
          target = new Socket(descriptor.getRemoteAddress(), descriptor.getRemotePort());
        }
        SocketForwarder.connect("", socket, target, false, null, descriptor);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
