package com.y59song.Network;

import android.util.Log;
import com.y59song.Forwader.MySocketForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.SSLSocketBuilder;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer implements Runnable {
  private static final boolean DEBUG = true;
  private static final String TAG = "LocalServer";
  public static final String addr = "127.0.0.1";
  public static final int port = 12345;
  public static final int SSLPort = 443;

  private ServerSocketChannel serverSocketChannel;
  private boolean isStop = false;
  private MyVpnService vpnService;
  public LocalServer(MyVpnService vpnService) {
    if(serverSocketChannel == null || !serverSocketChannel.isOpen())
      try {
        listen();
      } catch (IOException e) {
        if(DEBUG) Log.d(TAG, "Listen error");
        e.printStackTrace();
      }
    this.vpnService = vpnService;
  }

  private void listen() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(port));
  }

  private class ForwarderHandler implements Runnable {
    private Socket client;
    public ForwarderHandler(Socket client) {
      this.client = client;
    }
    @Override
    public void run() {
      try {
        ConnectionDescriptor descriptor = vpnService.getClientResolver().getClientDescriptorBySocket(client);
        SocketChannel targetChannel = SocketChannel.open();
        Socket target = targetChannel.socket();
        vpnService.protect(target);
        if(descriptor.getRemotePort() == SSLPort) {
          SiteData remoteData = vpnService.getResolver().getSecureHost(client, descriptor, false); // TODO
          Log.d(TAG, remoteData.tcpAddress + " " + remoteData.hostName);
          client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
          targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
          target = ((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
          Log.d(TAG, "" + target.getLocalPort() + " " + target.getInetAddress().getHostName() + " " + target.getInetAddress());
        } else {
          targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
        }
        MySocketForwarder.connect("", client, target, false, null, descriptor);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void run() {
    while(!isStop) {
      try {
        Log.d(TAG, "Accepting");
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        Log.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress());
        new Thread(new ForwarderHandler(socketChannel.socket())).start();
        Log.d(TAG, "Not blocked");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
