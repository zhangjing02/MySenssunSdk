//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.util;

import android.util.Log;

public class LOG {
    public static boolean viewLog = true;

    public LOG() {
    }

    public static void logE(String tag, String info) {
        if(viewLog) {
            Log.e(tag, info);
        }

    }

    public static void logW(String tag, String info) {
        if(viewLog) {
            Log.w(tag, info);
        }

    }

    public static void logI(String tag, String info) {
        if(viewLog) {
            Log.i(tag, info);
        }

    }

    public static void logD(String tag, String info) {
        if(viewLog) {
            Log.d(tag, info);
        }

    }
}
