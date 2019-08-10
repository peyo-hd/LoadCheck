package com.peyo.loadcheck;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.CpuUsageInfo;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckService extends Service {
    private static final String TAG = "CheckService";
    private Thread mThread;
    private boolean mContinue;
    private Notification mNotification;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");

        mContinue = false;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mContinue) {
                    checkLoad();
                    send();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted");
                    }
                }
            }
        });

        createNotificationChannel();
        mNotification = new Notification.Builder(this, CHANNEL_ID)
                .build();
    }

    String CHANNEL_ID = "LoadCheck";
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "System Load Checker",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        startForeground(1, mNotification);

        if (!mContinue) {
            mContinue = true;
            mThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(0);
        mContinue = false;
        try {
            mThread.interrupt();
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "onDestroy()");
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

    private void checkLoad() {
        checkHwProp();

        Log.i(TAG, "RxBytes : " + (TrafficStats.getTotalRxBytes() - totalRx));
        totalRx = TrafficStats.getTotalRxBytes();

        Log.i(TAG, "TxBytes : " + (TrafficStats.getTotalTxBytes() - totalTx));
        totalTx = TrafficStats.getTotalTxBytes();

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        Log.i(TAG, "MemoryUsage : " + (float)(mi.totalMem - mi.availMem)/mi.totalMem);
    }


    private void send() {
        String url = "http://<ip-addr>/api/log/RemoteControl?deviceid=<deviceid>";
        Log.i(TAG, "send() URL :" + url);
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url).openConnection());
            conn.setConnectTimeout(1000);
            int resp = conn.getResponseCode();
            Log.i(TAG, "Response code : " + resp);

            if (resp == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                String content = in.readLine();
                Log.i(TAG, "Content : " + content);

                JSONObject json = new JSONObject(content);
                Log.i(TAG, "actionid : " + json.getString("actionid"));
            }

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
