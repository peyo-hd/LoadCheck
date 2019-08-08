package com.peyo.loadcheck;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CpuUsageInfo;
import android.os.HardwarePropertiesManager;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private long total[] = {0, 0, 0, 0};
    private long active[] = {0, 0, 0, 0};

    private void checkHwProp() {
        HardwarePropertiesManager hm = (HardwarePropertiesManager) getSystemService(
                Context.HARDWARE_PROPERTIES_SERVICE);

        CpuUsageInfo[] infos = hm.getCpuUsages();
        for(int i = 0; i < infos.length ; i++) {
            long t = infos[i].getTotal() - total[i];
            long a = infos[i].getActive() - active[i];
            Log.i(TAG, "CpuUsage[" + i + "] = " + ((float)a)/t);
            total[i]= infos[i].getTotal();
            active[i]= infos[i].getActive();
        }

        float[] temps = hm.getDeviceTemperatures(
                HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                HardwarePropertiesManager.TEMPERATURE_CURRENT);
        for (int i = 0; i < temps.length; i++) {
            Log.i(TAG, "CpuTemp[" + i + "] = " + temps[i]);
        }
    }

    long totalRx = 0;
    long totalTx = 0;

    public void onCheckClicked(View v) {
        Log.i(TAG, "onCheckClicked()");

        //checkHwProp();

        Log.i(TAG, "RxBytes : " + (TrafficStats.getTotalRxBytes() - totalRx));
        totalRx = TrafficStats.getTotalRxBytes();

        Log.i(TAG, "TxBytes : " + (TrafficStats.getTotalTxBytes() - totalTx));
        totalTx = TrafficStats.getTotalTxBytes();

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        Log.i(TAG, "MemoryUsage : " + (float)(mi.totalMem - mi.availMem)/mi.totalMem);
    }
}
