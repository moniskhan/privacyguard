package com.y59song.Utilities;

import android.location.Location;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by frank on 9/29/14.
 */
public class MyLogger {
    public static String dir;
    private static boolean DEBUG = false;
    public static void log(String packageName, String msg, ArrayList<Location> locations) {
        if(DEBUG) Log.i("MyLogger", packageName + " " + msg);
        File f = new File(dir + "/" + packageName);
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
            for(Location loc : locations) {
                out.println(loc.getProvider() + " : lon=" + loc.getLongitude() + ", lat=" + loc.getLatitude());
            }
            out.println(msg);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
