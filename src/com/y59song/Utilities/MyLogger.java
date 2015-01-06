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

    public static void log(String packageName, String time, String msg, ArrayList<Location> locations) {
        File f = new File(dir + "/" + packageName);
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
            out.println("Time : "  + time);
            for(Location loc : locations) {
                out.println(loc.getProvider() + " : lon=" + loc.getLongitude() + ", lat=" + loc.getLatitude());
            }
            out.println(msg);
            out.println("");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
