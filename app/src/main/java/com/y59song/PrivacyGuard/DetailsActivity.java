package com.y59song.PrivacyGuard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class DetailsActivity extends Activity{
    private String name;
    GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Get the message from the intent
        Intent intent = getIntent();

        final String appName = intent.getStringExtra(PrivacyGuard.EXTRA_APP);
        name = appName;

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        DatabaseHandler db = new DatabaseHandler(DetailsActivity.this);
        List<DataLeak> leakList = db.getAppLeaks(appName);

        ((TextView) findViewById(R.id.textView2)).setText("Toggle notifications for " + appName + ":");

        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i).getIgnore() == 0) {
                toggle.setChecked(true);
            }
        }

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DatabaseHandler db = new DatabaseHandler(DetailsActivity.this);
                List<DataLeak> leakList = db.getAppLeaks(appName);
                if (isChecked) {
                    // The toggle is enabled
                    for (int i = 0; i < leakList.size(); i++) {
                        if (leakList.get(i).getIgnore() != 0) {
                            leakList.get(i).setIgnore(0);
                            db.updateLeak(leakList.get(i));
                        }
                    }
                } else {
                    // The toggle is disabled
                    for (int i = 0; i < leakList.size(); i++) {
                        if (leakList.get(i).getIgnore() != 0) {
                            leakList.get(i).setIgnore(1);
                            db.updateLeak(leakList.get(i));
                        }
                    }
                }



            }
        });

        int size = Integer.parseInt(intent.getStringExtra(PrivacyGuard.EXTRA_SIZE));

        String message = "";

        googleMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        googleMap.setMyLocationEnabled(true);

        // Drawing marker on the map
        for (int i = 0; i < size; i++) {
            message = intent.getStringExtra(PrivacyGuard.EXTRA_DATA + i);
            String[] point = message.split(";");
            drawMarker(new LatLng(Double.parseDouble(point[0]), Double.parseDouble(point[1])), intent.getStringExtra(PrivacyGuard.EXTRA_DATE_FORMAT + i));

            // Moving CameraPosition to last point position
            if (i == size-1){
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(point[0]), Double.parseDouble(point[1]))));

                // Setting the zoom level in the map on last point
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat("5")));
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        DatabaseHandler db = new DatabaseHandler(DetailsActivity.this);
        List<DataLeak> leakList = db.getAppLeaks(name);

        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i).getIgnore() == 0) {
                toggle.setChecked(true);
            }
        }
    }

    private void drawMarker(LatLng point, String date){
        // Creating an instance of MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting latitude and longitude for the marker
        markerOptions.position(point);
        markerOptions.title("Timestamp");
        markerOptions.snippet(date);

        // Adding marker on the Google Map
        googleMap.addMarker(markerOptions);
    }
}
