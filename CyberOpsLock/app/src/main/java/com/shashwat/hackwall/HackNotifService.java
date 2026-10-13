package com.shashwat.hackwall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Lock-screen controller: a public, ongoing notification with action buttons
 * (HACK / TORCH / EXIT) that shows a live, fake "hacking simulation" log.
 * Purely visual: nothing is actually hacked.
 */
public class HackNotifService extends Service {
    static final String CH = "hack_ctrl";
    static final int NID = 7;
    static final String A_START = "com.shashwat.hackwall.START";
    static final String A_HACK = "com.shashwat.hackwall.HACK";
    static final String A_TORCH = "com.shashwat.hackwall.TORCH";
    static final String A_EXIT = "com.shashwat.hackwall.EXIT";

    static final String[] LOG = {
            "Connecting to target...", "Bypassing firewall...", "Access granted.",
            "Extracting data...", "Downloading 78%...", "Downloading 91%...",
            "Downloading 100%.", "Operation completed."};
    static final int[] PCT = {0, 3, 12, 25, 40, 78, 91, 100, 100};

    final Handler handler = new Handler(Looper.getMainLooper());
    final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
    final ArrayList<String> lines = new ArrayList<String>();

    boolean fg = false;
    boolean running = false;
    boolean torchOn = false;
    int n = 0; // lines shown

    CameraManager cm;
    CameraManager.TorchCallback torchCb;

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (n < LOG.length) {
                lines.add(fmt.format(new Date()) + "  " + LOG[n]);
                n++;
                refresh();
            }
            if (n < LOG.length) {
                handler.postDelayed(this, 1500);
            } else {
                running = false;
                refresh();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel ch = new NotificationChannel(CH, "Hack terminal controls",
                NotificationManager.IMPORTANCE_LOW);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(false);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        try {
            cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            torchCb = new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    torchOn = enabled;
                    if (fg) refresh();
                }
            };
            cm.registerTorchCallback(torchCb, handler);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (A_EXIT.equals(action)) {
            handler.removeCallbacks(tick);
            running = false;
            stopForeground(true);
            fg = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        ensureForeground();
        if (A_HACK.equals(action)) {
            startSim();
        } else if (A_TORCH.equals(action)) {
            toggleTorch();
        }
        return START_STICKY;
    }

    void ensureForeground() {
        if (fg) return;
        Notification nt = build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NID, nt, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NID, nt);
        }
        fg = true;
    }

    void startSim() {
        handler.removeCallbacks(tick);
        lines.clear();
        n = 0;
        running = true;
        handler.post(tick);
    }

    void toggleTorch() {
        try {
            if (cm == null) return;
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lens = cc.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash) && lens != null
                        && lens == CameraCharacteristics.LENS_FACING_BACK) {
                    torchOn = !torchOn;
                    cm.setTorchMode(id, torchOn);
                    refresh();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    void refresh() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NID, build());
    }

    Notification.Action act(String label, String action) {
        Intent i = new Intent(this, HackNotifService.class).setAction(action);
        PendingIntent pi = PendingIntent.getService(this, action.hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action.Builder b = new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_stat), label, pi);
        if (Build.VERSION.SDK_INT >= 31) {
            b.setAuthenticationRequired(false); // works without unlocking the phone
        }
        return b.build();
    }

    Notification build() {
        StringBuilder full = new StringBuilder();
        if (lines.isEmpty()) {
            full.append("SYSTEM ONLINE\nPress HACK to start the simulation.");
        } else {
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) full.append('\n');
                full.append(lines.get(i));
            }
        }
        int pct = PCT[Math.min(n, PCT.length - 1)];
        String title = running ? "> HACKING...  [" + pct + "%]"
                : (n >= LOG.length ? "> OPERATION COMPLETED  [100%]" : "> HACK TERMINAL  [READY]");
        String summary = lines.isEmpty() ? "SYSTEM ONLINE  |  tap HACK" : lines.get(lines.size() - 1);

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent contentPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder b = new Notification.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(full.toString()))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setColor(0xFF00A83E)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentPi)
                .addAction(act(running ? "RESTART" : "HACK", A_HACK))
                .addAction(act(torchOn ? "TORCH: ON" : "TORCH: OFF", A_TORCH))
                .addAction(act("EXIT", A_EXIT));
        if (n > 0) b.setProgress(100, pct, false);
        return b.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
        try {
            if (cm != null && torchCb != null) cm.unregisterTorchCallback(torchCb);
        } catch (Exception ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
