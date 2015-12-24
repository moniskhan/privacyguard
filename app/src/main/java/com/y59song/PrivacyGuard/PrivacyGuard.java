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

package com.y59song.PrivacyGuard;

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
import android.widget.ListView;
import javax.security.cert.CertificateEncodingException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import android.widget.AdapterView;

public class PrivacyGuard extends Activity implements View.OnClickListener {
  private Intent intent;

  public static final boolean debug = false;

  public static final String FIRST_COLUMN="First";
  public static final String SECOND_COLUMN="Second";
  public static final String THIRD_COLUMN="Third";
  public static final String FOURTH_COLUMN="Fourth";
  public static final String FIFTH_COLUMN="Fifth";

  public final static String EXTRA_DATA = "com.y59song.PrivacyGuard.DATA";
  public final static String EXTRA_APP = "com.y59song.PrivacyGuard.APP";
  public final static String EXTRA_SIZE = "com.y59song.PrivacyGuard.SIZE";
  public final static String EXTRA_DATE_FORMAT = "com.y59song.PrivacyGuard.DATE";

  private ArrayList<HashMap<String, String>> list;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    intent = new Intent(this, MyVpnService.class);
    findViewById(R.id.connect).setOnClickListener(this);

    DatabaseHandler db = new DatabaseHandler(this);
    db.monthlyReset();
    populateDb();
    initialize();
    installCertificate();
  }

  @Override
  protected void onResume() {
    super.onResume();

    populateDb();
  }

  public void populateDb () {
    // -----------------------------------------------------------------------
    // Database Fetch
    DatabaseHandler db = new DatabaseHandler(this);
    List<DataLeak> leaks = db.getAllLeaks();
    // -----------------------------------------------------------------------
    ListView lv = (ListView)findViewById(R.id.leaksList);
    list = new ArrayList<HashMap<String,String>>();

    HashMap<String,ArrayList<Integer>> groupedList = new HashMap<String, ArrayList<Integer>>();

    for (DataLeak l : leaks) {
      if (groupedList.containsKey(l.getAppName())){
        ArrayList<Integer> previousCount = groupedList.get(l.getAppName());
        if (l.getLeakType().replace("is leaking", "").equals("Android ID")){
          previousCount.set(1,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("IMEI")){
          previousCount.set(0,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("Location")){
          previousCount.set(2,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("phone_number") || l.getLeakType().replace("is leaking", "").equals("email_address")){
          previousCount.set(3,l.getFrequency());
        }

        groupedList.put(l.getAppName(), previousCount);
      } else {
        ArrayList<Integer> addCount = new ArrayList<Integer>();
        addCount.add(0);
        addCount.add(0);
        addCount.add(0);
        if (l.getLeakType().replace("is leaking", "").equals("Android ID")){
          addCount.set(1,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("IMEI")){
          addCount.set(0,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("Location")){
          addCount.set(2,l.getFrequency());
        } else if (l.getLeakType().replace("is leaking", "").equals("phone_number") || l.getLeakType().replace("is leaking", "").equals("email_address")){
          addCount.set(3,l.getFrequency());
        }

        groupedList.put(l.getAppName(), addCount);
      }
    }

    HashMap<String,String> header = new HashMap<String, String>();
    header.put(FIRST_COLUMN, "App Name");
    header.put(SECOND_COLUMN, "IMEI");
    header.put(THIRD_COLUMN, "Android ID");
    header.put(FOURTH_COLUMN, "Location");
    header.put(FIFTH_COLUMN, "Contact");
    list.add(header);

    Iterator it = groupedList.entrySet().iterator();
    while (it.hasNext()) {
      HashMap.Entry pair = (HashMap.Entry)it.next();
      System.out.println(pair.getKey() + " = " + pair.getValue());
      ArrayList<Integer> counters = (ArrayList)  pair.getValue();
      HashMap<String, String> temp = new HashMap<String, String>();
      temp.put(FIRST_COLUMN, pair.getKey().toString());
      temp.put(SECOND_COLUMN, counters.get(0).toString());
      temp.put(THIRD_COLUMN, counters.get(1).toString());
      temp.put(FOURTH_COLUMN, counters.get(2).toString());
      list.add(temp);
      it.remove(); // avoids a ConcurrentModificationException
    }

    ListViewAdapter adapter=new ListViewAdapter(this, list);
    lv.setAdapter(adapter);

    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        if (position != 0) {
          Intent intent = new Intent(PrivacyGuard.this, DetailsActivity.class);
          DatabaseHandler db = new DatabaseHandler(PrivacyGuard.this);

          String appName = list.get(position).get(FIRST_COLUMN);
          List<LocationLeak> leakList = db.getLocationLeaks(appName);

          intent.putExtra(EXTRA_APP, appName);
          intent.putExtra(EXTRA_SIZE, String.valueOf(leakList.size()));

          for (int i = 0; i < leakList.size(); i++) {
            intent.putExtra(EXTRA_DATA + i, leakList.get(i).getLocation()); // to pass values between activities
            intent.putExtra(EXTRA_DATE_FORMAT + i,  leakList.get(i).getTimeStamp());
          }
          startActivity(intent);
        }

      }

    });

  }

  public void initialize() {
    MyLogger.dir = this.getCacheDir().getAbsolutePath();
  }

  public void installCertificate() {
    String Dir = this.getCacheDir().getAbsolutePath();
    try {
      if(CertificateManager.isCACertificateInstalled(Dir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password))
        return;
      else CertificateManager.generateCACertificate(Dir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
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
