
package com.y59song.LocationGuard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.security.KeyChain;
import android.view.View;
import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import javax.security.cert.X509Certificate;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class LocationGuard extends Activity implements View.OnClickListener {
  private Intent intent;
  public static final String CAName = "/LocationGuard_CA";
  public static final String CertName = "/LocationGuard_Cert";
  public static final String KeyType = "PKCS12";
  public static final String Password = "";
  public static final boolean debug = true;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    intent = new Intent(this, MyVpnService.class);
    findViewById(R.id.connect).setOnClickListener(this);
    installCertificate();
  }

  public void installCertificate() {
    Intent intent = KeyChain.createInstallIntent();
    try {
      String Dir = this.getExternalFilesDir(null).getAbsolutePath();
      new SSLSocketFactoryFactory(Dir + CAName, Dir + CertName, KeyType, Password.toCharArray());
      String CERT_FILE = Dir + CAName + "_export.crt";
      File certFile = new File(CERT_FILE);
      FileInputStream certIs = new FileInputStream(CERT_FILE);
      byte [] cert = new byte[(int)certFile.length()];
      certIs.read(cert);
      X509Certificate x509 = X509Certificate.getInstance(cert);
      intent.putExtra(KeyChain.EXTRA_CERTIFICATE, x509.getEncoded());
      intent.putExtra(KeyChain.EXTRA_NAME, "Test");
    } catch (Exception e) {
      e.printStackTrace();
    }
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
    ActivityManager activityManager = (ActivityManager)
      mContext.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);

    if (!(serviceList.size()>0)) return false;
    for (int i=0; i<serviceList.size(); i++)
      if (serviceList.get(i).service.getClassName().equals(className) == true)
        return true;

    return false;
  }
}
