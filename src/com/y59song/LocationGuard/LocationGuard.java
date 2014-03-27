
package com.y59song.LocationGuard;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;

public class LocationGuard extends Activity implements View.OnClickListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    findViewById(R.id.connect).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    Intent intent = VpnService.prepare(this);
    if (intent != null) {
      startActivityForResult(intent, 0);
    } else {
      onActivityResult(0, RESULT_OK, null);
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    if (result == RESULT_OK) {
      Intent intent = new Intent(this, MyVpnService.class);
      startService(intent);
    }
  }
}
