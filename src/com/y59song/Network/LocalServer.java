package com.y59song.Network;

import android.util.Log;
import com.y59song.Forwader.MySocketForwarder;
import com.y59song.LocationGuard.MyVpnService;
import com.y59song.Utilities.SSLSocketBuilder;
import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer extends Thread {
  private static final boolean DEBUG = true;
  private static final String TAG = LocalServer.class.getSimpleName();
  public static int port = 12345;
  public static final int SSLPort = 443;

  private ServerSocketChannel serverSocketChannel;
  private MyVpnService vpnService;
  private Set<String> sslPinning = new HashSet<String>();
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
    serverSocketChannel.socket().setReuseAddress(true);
    //serverSocketChannel.socket().bind(new InetSocketAddress(port));
    serverSocketChannel.socket().bind(null);
    port = serverSocketChannel.socket().getLocalPort();
  }

  private class ForwarderHandler implements Runnable {
    private final String TAG = ForwarderHandler.class.getSimpleName();
    private Socket client;
    public ForwarderHandler(Socket client) {
      this.client = client;
    }
    @Override
    public void run() {
      try {
        ConnectionDescriptor descriptor = vpnService.getClientResolver().getClientDescriptorByPort(client.getPort());
        SocketChannel targetChannel = SocketChannel.open();
        Socket target = targetChannel.socket();
        vpnService.protect(target);
        targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
        if(descriptor != null && descriptor.getRemotePort() == SSLPort && sslPinning.contains(descriptor.getRemoteAddress())) {
          SiteData remoteData = vpnService.getResolver().getSecureHost(client, descriptor, true);
          if(DEBUG) Log.d(TAG, "Begin Handshake : " + remoteData.tcpAddress + " " + remoteData.hostName);
          SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
          SSLSession session = ssl_client.getSession();
          if(DEBUG) Log.d(TAG, "After Handshake : " + session.isValid());
          if(session.isValid()) {
            client = ssl_client;
            target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
          } else {
            sslPinning.add(descriptor.getRemoteAddress());
            ssl_client.close();
            assert(!client.isClosed());
          }
        }
        MySocketForwarder.connect(client, target, vpnService.getNewPlugin());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void run() {
    while(!isInterrupted()) {
      try {
        if(DEBUG) Log.d(TAG, "Accepting");
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        if(DEBUG) Log.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress());
        new Thread(new ForwarderHandler(socketChannel.socket())).start();
        if(DEBUG) Log.d(TAG, "Not blocked");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Log.d(TAG, "Stop Listening");
  }
}
