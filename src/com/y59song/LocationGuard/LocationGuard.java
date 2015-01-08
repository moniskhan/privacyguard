/*
 * Main activity
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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.security.KeyChain;
import android.view.View;
import com.y59song.Utilities.Certificate.CertificateManager;
import com.y59song.Utilities.MyLogger;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStoreException;
import java.util.List;

public class LocationGuard extends Activity implements View.OnClickListener {
  private Intent intent;

  public static final boolean debug = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    intent = new Intent(this, MyVpnService.class);
    findViewById(R.id.connect).setOnClickListener(this);
    initialize();
    installCertificate();
  }

  public void initialize() {
    MyLogger.dir = this.getCacheDir().getAbsolutePath();
  }

  public void installCertificate() {
    String Dir = this.getCacheDir().getAbsolutePath();
    try {
      if(CertificateManager.isCACertificateInstalled(Dir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password))
        return;
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }
    Intent intent = KeyChain.createInstallIntent();
    try {
      intent.putExtra(KeyChain.EXTRA_CERTIFICATE, CertificateManager.getCACertificate(Dir, MyVpnService.CAName).getEncoded());
    } catch (CertificateEncodingException e) {
      e.printStackTrace();
    }
    intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
    startActivity(intent);
  }

  @Override
  public void onClick(View v) {
    if(!isServiceRunning(this, MyVpnService.class.getName())) {
      startVPN();
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    if (result == RESULT_OK) {
      startService(intent);
    }
  }

  private void startVPN() {
    Intent intent = VpnService.prepare(this);
    if (intent != null) {
      startActivityForResult(intent, 0);
    } else {
      onActivityResult(0, RESULT_OK, null);
    }
  }

  public static boolean isServiceRunning(Context mContext,String className) {
    ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);

    for (ActivityManager.RunningServiceInfo serviceInfo : serviceList) {
      if (serviceInfo.service.getClassName().equals(className))
        return true;
    }

    return false;
  }
}
