
package com.y59song.LocationGuard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;

import java.util.List;

public class LocationGuard extends Activity implements View.OnClickListener {
  private Intent intent;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    intent = new Intent(this, MyVpnService.class);
    findViewById(R.id.connect).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    if(!isServiceRunning(this, MyVpnService.class.getName()))
      startVPN();

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
