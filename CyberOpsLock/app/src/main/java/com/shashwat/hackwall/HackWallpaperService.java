package com.shashwat.hackwall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Hacker-style live wallpaper (pure simulation / visuals only).
 * Design space is 844 x 1855 units, scaled to the phone screen.
 */
public class HackWallpaperService extends WallpaperService {

    // ---- easy to edit ----
    static final String TARGET_CITY = "RANCHI";
    static final String LAT = "23.344";
    static final String LON = "85.309";
    // ----------------------

    static final int G = 0xFF00FF66;
    static final int GD = 0xFF00A83E;
    static final int W = 0xFFE6FFEE;
    static final int GREY = 0xFFC8C8C2;

    static final Typeface MONO = Typeface.MONOSPACE;
    static final Typeface MONOB = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    static final Typeface MONOBI = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC);

    static final String RAIN = "01アイウエオカキクケコサシスセソ0123456789ABCDEF<>/\\";

    static final String[] LOG = {
            "Connecting to target...", "Bypassing firewall...", "Access granted.",
            "Extracting data...", "Downloading 78%...", "Downloading 91%...",
            "Downloading 100%.", "Operation completed."};
    static final float[] AT = {0.5f, 2.5f, 4.5f, 6.5f, 9f, 11f, 13f, 15f};
    static final float[] PCT = {0, 3, 12, 25, 40, 78, 91, 100, 100};

    static final String HEXC = "0123456789ABCDEF";
    static final String[] POOL = {
            "> root access...  OK", "> firewall...  bypassed", "> tracking...  active", "> 0xDEADBEEF...",
            "> port scan 1-65535...", "> handshake captured", "> injecting payload...", "> decrypting hash...",
            "> spoofing MAC address...", "> tunnel established", "> cleaning logs...", "> escalating privileges...",
            "> proxy chain: 7 hops", "> bypassing 2FA...", "> kernel module loaded"};
    static final String[] ALOG = {
            "IP traced", "Firewall bypass", "Data exfiltrated", "Connection stable", "Port 443 open",
            "Packet injected", "Hash cracked", "Proxy rotated", "Logs wiped", "Session opened"};

    static final float[] BX = {126, 264, 421, 579, 717};
    static final float BY = 1688;
    static final float[] BR = {50, 50, 76, 50, 50};
    static final String[] BL = {"CALL", "FLASHLIGHT", "UNLOCK", "CAMERA", "APPS"};

    static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    static int a(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public Engine onCreateEngine() {
        return new HackEngine();
    }

    class HackEngine extends Engine {
        static final int COLS = 28;

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable loop = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final RectF rect = new RectF();
        final Random rnd = new Random();
        final Date date = new Date();
        final SimpleDateFormat fmtDate = new SimpleDateFormat("EEE dd MMM", Locale.ENGLISH);
        final SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        final SimpleDateFormat fmtHms = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        final char[] buf = new char[1];

        boolean visible = false;
        float sc = 1f, ox = 0f, oy = 0f;
        long start = SystemClock.uptimeMillis();
        long simBase = start;
        int battery = 78;
        boolean torchOn = false;
        int pressed = -1;
        long grantedAt = -100000;
        float rippleX, rippleY;
        long rippleAt = -100000;

        float[] colOff = new float[COLS];
        float[] colSpeed = new float[COLS];
        int[] colLen = new int[COLS];
        float[] globePts = new float[180];
        float[] barcode = new float[26];
        float[] scan;
        int[] bin = {0x48, 0x4F, 0x50, 0x45};
        float lastBin = 0;
        float simPct = 0;
        Shader beam;
        Shader sweep;
        float rainT = 0;
        long lastUp = SystemClock.uptimeMillis();
        long tapAt = -100000;

        BroadcastReceiver batRx;
        CameraManager camMgr;
        CameraManager.TorchCallback torchCb;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(true);
            for (int i = 0; i < COLS; i++) {
                colOff[i] = rnd.nextFloat() * 2000f;
                colSpeed[i] = 55f + rnd.nextFloat() * 110f;
                colLen[i] = 8 + rnd.nextInt(14);
            }
            for (int i = 0; i < 90; i++) {
                globePts[i * 2] = (float) ((rnd.nextFloat() - 0.5f) * Math.PI * 0.9f); // lat
                globePts[i * 2 + 1] = (float) (rnd.nextFloat() * Math.PI * 2f);       // lon
            }
            for (int i = 0; i < barcode.length; i++) barcode[i] = 1f + rnd.nextInt(4);
            scan = new float[(1855 / 4) * 4];
            for (int i = 0; i < 1855 / 4; i++) {
                scan[i * 4] = 0;
                scan[i * 4 + 1] = i * 4f;
                scan[i * 4 + 2] = 844;
                scan[i * 4 + 3] = i * 4f;
            }
            beam = new LinearGradient(0, 0, 0, 140,
                    new int[]{0x0000FF66, 0x3000FF66, 0x0000FF66}, null, Shader.TileMode.CLAMP);
            sweep = new SweepGradient(0, 0, new int[]{0x0000FF66, 0x5000FF66}, new float[]{0.6f, 1f});

            final Context ctx = HackWallpaperService.this;
            batRx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    int l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    if (l >= 0 && s > 0) battery = l * 100 / s;
                }
            };
            try {
                Intent st = ctx.registerReceiver(batRx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (st != null) batRx.onReceive(ctx, st);
            } catch (Exception ignored) {
            }
            try {
                camMgr = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
                torchCb = new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                        torchOn = enabled;
                    }
                };
                camMgr.registerTorchCallback(torchCb, handler);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(loop);
            try {
                HackWallpaperService.this.unregisterReceiver(batRx);
            } catch (Exception ignored) {
            }
            try {
                if (camMgr != null && torchCb != null) camMgr.unregisterTorchCallback(torchCb);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            sc = Math.min(w / 844f, h / 1855f);
            ox = (w - 844f * sc) / 2f;
            oy = (h - 1855f * sc) / 2f;
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(loop);
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(loop);
            if (v) drawFrame();
        }

        // ---------------- touch ----------------

        int hit(float x, float y) {
            for (int i = 0; i < 5; i++) {
                float dx = x - BX[i], dy = y - BY;
                float r = BR[i] + 14;
                if (dx * dx + dy * dy <= r * r) return i;
            }
            return -1;
        }

        @Override
        public void onTouchEvent(MotionEvent e) {
            float x = (e.getX() - ox) / sc, y = (e.getY() - oy) / sc;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pressed = hit(x, y);
                    tapAt = SystemClock.uptimeMillis();
                    rippleX = x;
                    rippleY = y;
                    rippleAt = SystemClock.uptimeMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    int h = hit(x, y);
                    if (h >= 0 && h == pressed) act(h);
                    pressed = -1;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    pressed = -1;
                    break;
                default:
                    break;
            }
            super.onTouchEvent(e);
        }

        void launch(Intent i) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                HackWallpaperService.this.startActivity(i);
            } catch (Exception ignored) {
            }
        }

        void buzz() {
            try {
                Vibrator v = (Vibrator) HackWallpaperService.this.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception ignored) {
            }
        }

        void toggleTorch() {
            try {
                if (camMgr == null) return;
                for (String id : camMgr.getCameraIdList()) {
                    CameraCharacteristics cc = camMgr.getCameraCharacteristics(id);
                    Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lens = cc.get(CameraCharacteristics.LENS_FACING);
                    if (Boolean.TRUE.equals(flash) && lens != null
                            && lens == CameraCharacteristics.LENS_FACING_BACK) {
                        torchOn = !torchOn;
                        camMgr.setTorchMode(id, torchOn);
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        void act(int idx) {
            buzz();
            switch (idx) {
                case 0:
                    launch(new Intent(Intent.ACTION_DIAL));
                    break;
                case 1:
                    toggleTorch();
                    break;
                case 2:
                    grantedAt = SystemClock.uptimeMillis();
                    simBase = grantedAt; // restart the hacking simulation
                    break;
                case 3:
                    launch(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                    break;
                case 4:
                    launch(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
                    break;
                default:
                    break;
            }
        }

        // ---------------- frame loop ----------------

        void drawFrame() {
            handler.removeCallbacks(loop);
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                try {
                    c = holder.lockHardwareCanvas();
                } catch (Exception ex) {
                    c = holder.lockCanvas();
                }
                if (c != null) render(c);
            } catch (Exception ignored) {
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (visible) handler.postDelayed(loop, 33);
        }

        void render(Canvas c) {
            long upNow = SystemClock.uptimeMillis();
            float t = (upNow - start) / 1000f;
            long wall = System.currentTimeMillis();
            float cyc = ((upNow - simBase) / 1000f) % 19f;
            float dt = Math.min(0.1f, (upNow - lastUp) / 1000f);
            lastUp = upNow;
            float boost = Math.max(0f, 1f - (upNow - tapAt) / 1500f);
            rainT += dt * (1f + 4f * boost);

            c.drawColor(Color.BLACK);
            c.save();
            c.translate(ox, oy);
            c.scale(sc, sc);

            rain(c, rainT);
            hood(c, t);
            hexStrip(c, t);
            header(c, t, wall);
            globe(c, t);
            breachBox(c, t);
            exfilBox(c, t);
            coordBox(c, t);
            slogans(c, t);
            emblem(c, t);
            scanList(c, t);
            netBox(c);
            leftBlock(c, t);
            sim(c, wall, cyc);
            somewhere(c, t);
            cracker(c, t);
            binary(c, t);
            mapBox(c, t);
            targetBox(c, t);
            accessLog(c, wall, t);
            buttons(c, t);
            footer(c, t);
            overlay(c, t, upNow);

            c.restore();
        }

        // ---------------- helpers ----------------

        void txt(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(bold ? MONOB : MONO);
            p.setTextSize(size);
            p.setColor(color);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText(s, x, y, p);
        }

        void ctxt(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(bold ? MONOB : MONO);
            p.setTextSize(size);
            p.setColor(color);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, x, y, p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        void stroke(int color, float w) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(color);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setPathEffect(null);
        }

        void box(Canvas c, float l, float t, float r, float b) {
            float k = 14;
            path.reset();
            path.moveTo(l, t);
            path.lineTo(r - k, t);
            path.lineTo(r, t + k);
            path.lineTo(r, b);
            path.lineTo(l + k, b);
            path.lineTo(l, b - k);
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xB3001208);
            c.drawPath(path, p);
            stroke(G, 2f);
            c.drawPath(path, p);
        }

        void bar(Canvas c, float l, float t, float r, float b, float frac) {
            stroke(G, 1.5f);
            c.drawRect(l, t, r, b, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(G);
            float w = (r - l - 4) * Math.max(0f, Math.min(1f, frac));
            c.drawRect(l + 2, t + 2, l + 2 + w, b - 2, p);
        }

        // ---------------- sections ----------------

        void hood(Canvas c, float t) {
            path.reset();
            path.moveTo(432, 366);
            path.cubicTo(404, 370, 394, 408, 391, 440);
            path.lineTo(372, 520);
            path.lineTo(492, 520);
            path.lineTo(473, 440);
            path.cubicTo(470, 408, 460, 370, 432, 366);
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(a(G, 20));
            c.drawPath(path, p);
            stroke(a(G, 80), 1.2f);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xCC000000);
            rect.set(417, 392, 447, 440);
            c.drawOval(rect, p);
            if (((int) (t * 2)) % 7 != 0) {
                p.setColor(a(G, 230));
                c.drawCircle(425, 414, 2.2f, p);
                c.drawCircle(439, 414, 2.2f, p);
            }
        }

        void hexStrip(Canvas c, float t) {
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(MONO);
            p.setTextSize(9);
            p.setTextAlign(Paint.Align.LEFT);
            float sp = t * 7f;
            int base = (int) sp;
            float frac = sp - base;
            char[] hx = new char[4];
            for (int r = 0; r < 60; r++) {
                int v = hash(base + r);
                for (int k = 0; k < 4; k++) hx[k] = HEXC.charAt((v >>> (k * 4)) & 15);
                p.setColor(a(G, 50 + ((r + base) % 4) * 22));
                c.drawText(hx, 0, 4, 6, 380 + r * 12f - frac * 12f, p);
            }
        }

        void cracker(Canvas c, float t) {
            box(c, 232, 1398, 476, 1480);
            txt(c, "> DECRYPTING KEY", 246, 1420, 13, W, false);
            float cy = t % 8f;
            String target = "7F3A-91C0-DE42-B8E5";
            int resolved = (int) (Math.min(cy, 6f) / 6f * target.length());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < target.length(); i++) {
                char ch = target.charAt(i);
                if (ch == '-' || i < resolved) sb.append(ch);
                else sb.append(HEXC.charAt(hash(i * 13 + (int) (t * 20f)) & 15));
            }
            boolean found = cy >= 6f;
            txt(c, sb.toString(), 246, 1448, 19, found ? G : W, true);
            String st = found ? "KEY FOUND  >>  ACCESS" : "BRUTE-FORCING " + (int) (cy / 6f * 100f) + "%";
            txt(c, st, 246, 1470, 12.5f, found ? G : GD, false);
        }

        String hex8(int x) {
            String h = Integer.toHexString(x).toUpperCase(Locale.ENGLISH);
            while (h.length() < 8) h = "0" + h;
            return h;
        }

        String feedLine(int k) {
            if (k > 3 && k % 7 == 3) return "> 0x" + hex8(hash(k * 31)) + "...";
            return POOL[k % POOL.length];
        }

        void rain(Canvas c, float t) {
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(MONO);
            p.setTextSize(19);
            p.setTextAlign(Paint.Align.CENTER);
            float step = 21f;
            float colW = 844f / COLS;
            for (int i = 0; i < COLS; i++) {
                float x = colW * i + colW / 2f;
                int len = colLen[i];
                float span = 1855f + len * step;
                float head = (t * colSpeed[i] + colOff[i]) % span;
                int row = (int) (head / step);
                for (int k = 0; k < len; k++) {
                    int r = row - k;
                    float y = r * step;
                    if (y < 10 || y > 1850) continue;
                    float f = 1f - (float) k / len;
                    int al = (k == 0) ? 150 : (int) (f * f * 75);
                    int hs = hash(i * 977 + r * 131 + (k == 0 ? (int) (t * 8) : 0));
                    buf[0] = RAIN.charAt((hs >>> 3) % RAIN.length());
                    p.setColor(a(k == 0 ? 0xFFB8FFD0 : G, al));
                    c.drawText(buf, 0, 1, x, y, p);
                }
            }
            p.setTextAlign(Paint.Align.LEFT);
        }

        void drawTime(Canvas c, String tm, int color, boolean colonDim) {
            float cw = 85.2f, x = 64, y = 275;
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(MONOB);
            p.setTextSize(142);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(color);
            c.drawText(tm.substring(0, 2), x, y, p);
            p.setColor(colonDim ? a(color, 70) : color);
            c.drawText(":", x + 2 * cw, y, p);
            p.setColor(color);
            c.drawText(tm.substring(3, 5), x + 3 * cw, y, p);
        }

        void header(Canvas c, float t, long wall) {
            txt(c, "// SYSTEM ONLINE", 34, 36, 17, G, false);
            if (((int) (t * 2)) % 2 == 0) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(G);
                c.drawCircle(20, 30, 3.5f, p);
            }
            txt(c, "PWR:" + battery + "%", 250, 36, 15, GD, false);

            date.setTime(wall);
            txt(c, fmtDate.format(date).toUpperCase(Locale.ENGLISH), 68, 142, 32, W, false);
            String tm = fmtTime.format(date);
            boolean dim = ((wall / 500) % 2) == 1;
            c.save();
            c.translate(5, 0);
            drawTime(c, tm, a(G, 110), dim);
            c.restore();
            drawTime(c, tm, W, dim);

            // glitch slices
            if (t % 3.4f < 0.22f) {
                for (int b = 0; b < 3; b++) {
                    float y0 = 165 + rnd.nextInt(100);
                    c.save();
                    c.clipRect(40, y0, 520, y0 + 14 + rnd.nextInt(16));
                    c.translate(rnd.nextInt(30) - 15, 0);
                    drawTime(c, tm, G, false);
                    c.restore();
                }
            }
            // scanline cuts through the digits
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xB0000000);
            for (float y = 170; y < 282; y += 10) c.drawRect(60, y, 500, y + 2, p);

            txt(c, "NO SIGNALS  ·  ONLY PROGRESS", 68, 318, 22, 0xFFB8F5CC, false);
        }

        void globe(Canvas c, float t) {
            float cx = 690, cy = 215, r = 128;
            float rot = t * 0.5f;
            stroke(a(GD, 220), 2f);
            c.drawCircle(cx, cy, r, p);
            for (int k = -2; k <= 2; k++) {
                double lat = k * Math.PI / 6.0;
                float yy = cy - r * (float) Math.sin(lat);
                float rr = r * (float) Math.cos(lat);
                rect.set(cx - rr, yy - rr * 0.13f, cx + rr, yy + rr * 0.13f);
                stroke(a(GD, 150), 1.2f);
                c.drawOval(rect, p);
            }
            for (int k = 0; k < 6; k++) {
                double ang = rot + k * Math.PI / 6.0;
                float w = r * (float) Math.abs(Math.cos(ang));
                rect.set(cx - w, cy - r, cx + w, cy + r);
                stroke(a(GD, 150), 1.2f);
                c.drawOval(rect, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(G);
            for (int i = 0; i < 90; i++) {
                float lat = globePts[i * 2];
                float lon = globePts[i * 2 + 1] + rot;
                float cl = (float) Math.cos(lon);
                if (cl < 0) continue;
                float x = cx + r * (float) Math.cos(lat) * (float) Math.sin(lon);
                float y = cy - r * (float) Math.sin(lat);
                c.drawCircle(x, y, 2.4f, p);
            }
            float pph = (t * 1.2f) % 1f;
            for (int k = 0; k < 3; k++) {
                int idx = (((int) (t * 1.2f)) * 7 + k * 29) % 90;
                float lat = globePts[idx * 2];
                float lon = globePts[idx * 2 + 1] + rot;
                if (Math.cos(lon) < 0) continue;
                float x = cx + r * (float) Math.cos(lat) * (float) Math.sin(lon);
                float y = cy - r * (float) Math.sin(lat);
                stroke(a(W, (int) ((1 - pph) * 230)), 1.8f);
                c.drawCircle(x, y, 3 + pph * 16, p);
            }
            rect.set(cx - r - 10, cy - r - 10, cx + r + 10, cy + r + 10);
            stroke(a(G, 220), 3f);
            c.drawArc(rect, t * 70, 60, false, p);
            c.drawArc(rect, t * 70 + 180, 40, false, p);
        }

        void breachBox(Canvas c, float t) {
            box(c, 44, 362, 332, 514);
            txt(c, "> SYSTEM BREACH INITIATED...", 58, 388, 15, W, false);
            int idx = (int) (t / 1.1f);
            for (int j = 0; j < 4; j++) {
                int k = idx - 3 + j;
                if (k < 0) continue;
                String ln = feedLine(k);
                float y = 412 + j * 20f;
                boolean newest = (j == 3);
                String shown = ln;
                if (newest) {
                    int n2 = Math.max(0, Math.min(ln.length(), (int) ((t - k * 1.1f) * 40f)));
                    shown = ln.substring(0, n2);
                    if (((int) (t * 4)) % 2 == 0) {
                        p.setStyle(Paint.Style.FILL);
                        p.setColor(G);
                        c.drawRect(58 + n2 * 8.4f, y - 11, 58 + n2 * 8.4f + 7, y + 2, p);
                    }
                }
                txt(c, shown, 58, y, 14, newest ? W : G, false);
            }
            bar(c, 68, 487, 200, 500, (t * 0.2f) % 1f);
        }

        void exfilBox(Canvas c, float t) {
            box(c, 44, 532, 285, 660);
            p.setStyle(Paint.Style.FILL);
            p.setColor(G);
            c.drawRect(60, 548, 78, 560, p);
            txt(c, "/data/exfiltration", 88, 558, 15, W, false);
            txt(c, "> files found: 1,342", 72, 585, 14, G, false);
            txt(c, "> secret: true", 72, 605, 14, G, false);
            int pct = (int) ((t * 9f) % 101);
            txt(c, "> transfer: " + pct + "%", 72, 625, 14, G, false);
            bar(c, 73, 634, 267, 648, pct / 100f);
        }

        void coordBox(Canvas c, float t) {
            box(c, 578, 358, 820, 472);
            stroke(G, 1.5f);
            c.drawRect(598, 384, 614, 400, p);
            c.drawLine(606, 378, 606, 406, p);
            c.drawLine(592, 392, 620, 392, p);
            int d = ((int) (t * 3)) % 10;
            txt(c, "LAT " + LAT + d + "° N", 632, 386, 15, W, false);
            txt(c, "LON " + LON + (9 - d) + "° E", 632, 406, 15, W, false);
            txt(c, "TARGET :", 632, 436, 15, 0xFFB8F5CC, false);
            float ph2 = t % 12f;
            if (ph2 < 5f) txt(c, "UNKNOWN", 716, 436, 15, G, false);
            else if (ph2 < 9f) {
                int dots = ((int) (t * 3)) % 4;
                txt(c, "LOCATING" + "...".substring(0, dots), 716, 436, 15, 0xFFFFCC33, false);
            } else txt(c, "ACQUIRED", 716, 436, 15, 0xFFFF5555, true);
            txt(c, "STATUS :", 632, 456, 15, 0xFFB8F5CC, false);
            txt(c, "ONLINE", 716, 456, 15, G, false);
        }

        void slogans(Canvas c, float t) {
            // vertical japanese line
            String jp = "深く隠された真実";
            for (int i = 0; i < jp.length(); i++) {
                ctxt(c, jp.substring(i, i + 1), 497, 412 + i * 27f, 25, W, false);
            }
            stroke(a(W, 200), 1.5f);
            c.drawRect(530, 596, 566, 628, p);
            c.drawLine(530, 596, 566, 628, p);
            c.drawLine(566, 596, 530, 628, p);

            stroke(G, 2f);
            c.drawLine(640, 505, 640, 615, p);
            String[] ln = {"NO", "HUMAN", "IS", "ILLEGAL"};
            for (int i = 0; i < 4; i++) txt(c, ln[i], 662, 532 + i * 23f, 20, W, false);

            // crosshair
            float cx = 781, cy = 647;
            stroke(W, 1.6f);
            c.drawCircle(cx, cy, 28, p);
            c.drawLine(cx - 40, cy, cx + 40, cy, p);
            c.drawLine(cx, cy - 40, cx, cy + 40, p);
            float ph = (t / 2f) % 1f;
            stroke(a(G, (int) ((1 - ph) * 200)), 2f);
            c.drawCircle(cx, cy, 6 + ph * 30, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(W);
            c.drawCircle(cx, cy, 4, p);
        }

        void emblem(Canvas c, float t) {
            float cx = 421, cy = 845;
            stroke(a(G, 150), 1.5f);
            c.drawLine(cx, 560, cx, 1105, p);
            c.drawLine(190, cy, 672, cy, p);

            for (int k = 0; k < 2; k++) {
                float ph = ((t / 3f) + k * 0.5f) % 1f;
                stroke(a(G, (int) ((1 - ph) * 140)), 2f);
                c.drawCircle(cx, cy, 60 + ph * 190, p);
            }

            // dashed outer ring
            stroke(a(G, 210), 3f);
            p.setPathEffect(new DashPathEffect(new float[]{22, 10}, t * 40f));
            c.drawCircle(cx, cy, 238, p);
            p.setPathEffect(null);

            // grey ring
            stroke(a(GREY, 235), 22f);
            c.drawCircle(cx, cy, 212, p);
            stroke(0xFF101010, 2f);
            c.drawCircle(cx, cy, 199, p);
            rect.set(cx - 212, cy - 212, cx + 212, cy + 212);
            stroke(G, 6f);
            c.drawArc(rect, t * 60f, 45, false, p);
            c.drawArc(rect, t * 60f + 180f, 45, false, p);

            // rays
            stroke(a(GREY, 80), 1.2f);
            for (int k = 0; k < 90; k++) {
                double ang = k * Math.PI / 45.0;
                float r1 = 75, r2 = 150 + (hash(k) & 31);
                c.drawLine(cx + r1 * (float) Math.cos(ang), cy + r1 * (float) Math.sin(ang),
                        cx + r2 * (float) Math.cos(ang), cy + r2 * (float) Math.sin(ang), p);
            }

            // radar sweep
            c.save();
            c.translate(cx, cy);
            c.rotate((t * 100f) % 360f);
            p.setStyle(Paint.Style.FILL);
            p.setShader(sweep);
            c.drawCircle(0, 0, 196, p);
            p.setShader(null);
            c.restore();

            // triangle
            path.reset();
            path.moveTo(421, 705);
            path.lineTo(577, 940);
            path.lineTo(266, 940);
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(a(GREY, 240));
            c.drawPath(path, p);
            stroke(0xFF101010, 3f);
            c.drawPath(path, p);

            // eye (with blink)
            float bp = t % 5f;
            float open = bp < 0.15f ? Math.max(0.06f, Math.abs(bp - 0.075f) / 0.075f) : 1f;
            c.save();
            c.translate(421, 860);
            c.scale(1f, open);
            Path eye = new Path();
            eye.moveTo(-82, 0);
            eye.quadTo(0, -64, 82, 0);
            eye.quadTo(0, 64, -82, 0);
            eye.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFF2F2EE);
            c.drawPath(eye, p);
            c.save();
            c.clipPath(eye);
            float ex = (float) Math.sin(t * 0.8f) * 10f;
            float ey = (float) Math.cos(t * 0.6f) * 4f;
            p.setColor(0xFF1B1B1B);
            c.drawCircle(ex, ey, 36, p);
            stroke(0xFF555555, 3f);
            c.drawCircle(ex, ey, 26, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF000000);
            c.drawCircle(ex, ey, 14, p);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(ex + 11, ey - 11, 5, p);
            c.restore();
            stroke(0xFF101010, 4f);
            c.drawPath(eye, p);
            c.restore();

            // target lock brackets
            float lp = t % 7f;
            if (lp < 2.6f) {
                float sz = 150f - (lp / 2.6f) * 78f;
                float ex2 = 421, ey2 = 860, ln = 26f;
                stroke(G, 3f);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        float px = ex2 + sx * sz, py = ey2 + sy * sz * 0.7f;
                        c.drawLine(px, py, px - sx * ln, py, p);
                        c.drawLine(px, py, px, py - sy * ln * 0.8f, p);
                    }
                }
                if (lp > 1.6f && ((int) (t * 6)) % 2 == 0) {
                    ctxt(c, "TARGET LOCKED", 421, ey2 + sz * 0.7f + 28, 15, G, true);
                }
            }
        }

        void scanList(Canvas c, float t) {
            box(c, 684, 740, 820, 880);
            String[] ln = {"SCAN", "ANALYZE", "EXPLOIT", "REPEAT"};
            int act = ((int) (t / 1.2f)) % 4;
            for (int i = 0; i < 4; i++) {
                float y = 782 + i * 24f;
                if (i == act) txt(c, ">", 694, y, 20, G, true);
                txt(c, ln[i], 712, y, 20, i == act ? G : W, i == act);
            }
        }

        void netBox(Canvas c) {
            box(c, 666, 925, 820, 1060);
            String[] ln = {"IP: 192.168.1.1", "PORT: 443", "PROTOCOL: TLS", "ENCRYPTED: YES", "STATUS: CONNECTED"};
            for (int i = 0; i < ln.length; i++) txt(c, ln[i], 676, 968 + i * 21f, 13, i == 4 ? G : W, false);
        }

        void leftBlock(Canvas c, float t) {
            String[] ln = {"// life.exe", "// work.exe", "// money.exe", "// freedom.exe", "// loading..."};
            for (int i = 0; i < ln.length; i++) {
                txt(c, ln[i], 42, 915 + i * 20.5f, 16, i == 4 ? G : W, i == 4);
            }
            float f = (t * 0.15f) % 1f;
            stroke(G, 1.5f);
            c.drawRect(42, 1010, 172, 1022, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(G);
            int segs = (int) (f * 16);
            for (int i = 0; i < segs; i++) c.drawRect(44 + i * 8f, 1012, 49 + i * 8f, 1020, p);

            String[] r = {"REALITY", "IS JUST", "ANOTHER", "FILE"};
            for (int i = 0; i < r.length; i++) txt(c, r[i], 42, 1054 + i * 22f, 19, W, false);
        }

        void sim(Canvas c, long wall, float cyc) {
            box(c, 38, 1148, 460, 1372);
            txt(c, "> LIVE HACKING SIMULATION", 58, 1172, 14, G, false);
            txt(c, ">>X", 418, 1172, 13, GD, false);
            int shown = 0;
            for (int i = 0; i < AT.length; i++) if (AT[i] <= cyc) shown = i + 1;
            long base = wall - (long) (cyc * 1000f);
            for (int i = 0; i < shown; i++) {
                date.setTime(base + (long) (AT[i] * 1000f));
                float y = 1200 + i * 19.5f;
                txt(c, fmtHms.format(date), 58, y, 13.5f, W, false);
                txt(c, LOG[i], 148, y, 13.5f, (i == 2 || i == 7) ? G : W, i == 2 || i == 7);
            }
            float pct;
            if (shown == 0) pct = 0;
            else if (shown >= 8) pct = 100;
            else {
                float seg = (cyc - AT[shown - 1]) / (AT[shown] - AT[shown - 1]);
                pct = PCT[shown] + (PCT[shown + 1] - PCT[shown]) * Math.max(0f, Math.min(1f, seg));
            }
            simPct = pct;
            bar(c, 58, 1346, 385, 1358, pct / 100f);
            txt(c, "[" + (int) pct + "%]", 394, 1357, 13, G, false);
        }

        void somewhere(Canvas c, float t) {
            box(c, 38, 1400, 214, 1465);
            txt(c, "SOMEWHERE", 54, 1426, 15, W, false);
            txt(c, "IN THE WORLD...", 54, 1448, 15, W, false);
            float nudge = (float) Math.sin(t * 4f) * 3f;
            txt(c, ">", 192 + nudge, 1448, 16, G, true);
        }

        void binary(Canvas c, float t) {
            if (t - lastBin > 0.25f) {
                lastBin = t;
                bin[rnd.nextInt(4)] = rnd.nextInt(256);
                bin[rnd.nextInt(4)] = rnd.nextInt(256);
            }
            for (int i = 0; i < 4; i++) {
                String s = Integer.toBinaryString(256 | (bin[i] & 255)).substring(1);
                txt(c, s, 136, 1523 + i * 18f, 17, G, false);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(a(W, 220));
            float x = 44;
            for (int i = 0; i < barcode.length; i++) {
                if (i % 2 == 0) c.drawRect(x, 1487, x + barcode[i], 1600, p);
                x += barcode[i];
            }
        }

        void mapBox(Canvas c, float t) {
            box(c, 480, 1082, 820, 1262);
            float mx = 492, my = 1092, mw = 316, mh = 160;
            float[][] land = {{0.22f, 0.35f, 0.14f, 0.20f}, {0.30f, 0.68f, 0.07f, 0.17f},
                    {0.50f, 0.27f, 0.07f, 0.10f}, {0.52f, 0.55f, 0.08f, 0.18f},
                    {0.70f, 0.32f, 0.17f, 0.17f}, {0.84f, 0.74f, 0.06f, 0.07f}};
            p.setStyle(Paint.Style.FILL);
            p.setColor(a(G, 110));
            for (float y = 0; y < 1f; y += 0.045f) {
                for (float x = 0; x < 1f; x += 0.018f) {
                    for (float[] l : land) {
                        float dx = (x - l[0]) / l[2], dy = (y - l[1]) / l[3];
                        if (dx * dx + dy * dy <= 1f) {
                            c.drawCircle(mx + x * mw, my + y * mh, 1.3f, p);
                            break;
                        }
                    }
                }
            }
            float sx0 = mx + ((t * 0.25f) % 1f) * mw;
            for (int k = 0; k < 8; k++) {
                stroke(a(G, 190 - k * 24), 2f);
                c.drawLine(sx0 - k * 4, my, sx0 - k * 4, my + mh, p);
            }
            float[][] nodes = {{0.22f, 0.38f}, {0.50f, 0.28f}, {0.70f, 0.45f}, {0.84f, 0.74f},
                    {0.30f, 0.70f}, {0.52f, 0.58f}, {0.78f, 0.30f}};
            float tx = mx + nodes[2][0] * mw, ty = my + nodes[2][1] * mh;
            for (int i = 0; i < nodes.length; i++) {
                if (i == 2) continue;
                float sx = mx + nodes[i][0] * mw, sy = my + nodes[i][1] * mh;
                float qx = (sx + tx) / 2f, qy = (sy + ty) / 2f - 0.35f * Math.abs(tx - sx) - 8;
                path.reset();
                path.moveTo(sx, sy);
                path.quadTo(qx, qy, tx, ty);
                stroke(a(G, 160), 1.3f);
                c.drawPath(path, p);
                float u = (t * 0.3f + i * 0.17f) % 1f;
                float bx = (1 - u) * (1 - u) * sx + 2 * (1 - u) * u * qx + u * u * tx;
                float by = (1 - u) * (1 - u) * sy + 2 * (1 - u) * u * qy + u * u * ty;
                p.setStyle(Paint.Style.FILL);
                p.setColor(W);
                c.drawCircle(bx, by, 2.6f, p);
                p.setColor(G);
                c.drawCircle(sx, sy, 3f, p);
            }
            float ph = (t / 1.5f) % 1f;
            stroke(a(G, (int) ((1 - ph) * 220)), 2f);
            c.drawCircle(tx, ty, 4 + ph * 16, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(tx, ty, 4, p);
        }

        void targetBox(Canvas c, float t) {
            box(c, 480, 1280, 820, 1432);
            txt(c, "TARGET DATA", 496, 1304, 15, W, false);
            int files = (int) (simPct * 3.42f);
            String[] ln = {"> Location  : " + TARGET_CITY, "> Device    : ONLINE", "> Network   : WIFI",
                    "> Security  : WEAK", "> Files     : " + files + " / 342"};
            for (int i = 0; i < ln.length; i++) txt(c, ln[i], 496, 1326 + i * 17.5f, 13, i == 3 ? 0xFFFF5555 : G, false);
            path.reset();
            for (int i = 0; i < 62; i++) {
                float x = 496 + i * 4f;
                int hs = hash(i + (int) (t * 18f));
                float amp = (0.25f + 0.75f * ((hs & 7) / 7f)) * 15f * (float) Math.abs(Math.sin((i + t * 18f) * 0.8f));
                float y = 1424 - amp;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            stroke(G, 1.5f);
            c.drawPath(path, p);
        }

        void accessLog(Canvas c, long wall, float t) {
            box(c, 487, 1450, 788, 1572);
            txt(c, "ACCESS LOG:", 500, 1473, 14, W, false);
            int idx = (int) (t / 1.5f);
            for (int j = 0; j < 4; j++) {
                int k = idx - 3 + j;
                if (k < 0) continue;
                boolean warn = (k % 6 == 4);
                date.setTime(wall - (3 - j) * 1500L);
                float y = 1493 + j * 17f;
                txt(c, "> " + fmtHms.format(date), 500, y, 12, W, false);
                txt(c, ALOG[k % ALOG.length], 592, y, 12, W, false);
                txt(c, warn ? "WARN" : "OK", 746, y, 12, warn ? 0xFFFFAA33 : G, false);
            }
        }

        void buttons(Canvas c, float t) {
            ctxt(c, "—  HACK THE SYSTEM  —", 421, 1597, 19, G, false);
            for (int i = 0; i < 5; i++) {
                float cx = BX[i], r = BR[i];
                boolean pr = pressed == i;
                boolean on = (i == 1 && torchOn);
                p.setStyle(Paint.Style.FILL);
                p.setColor(pr ? a(G, 110) : on ? a(G, 70) : 0xCC000000);
                c.drawCircle(cx, BY, r, p);
                int ringCol = (i == 2 || on || pr) ? G : a(W, 230);
                stroke(ringCol, i == 2 ? 3f : 2.5f);
                c.drawCircle(cx, BY, r, p);
                if (i == 2) {
                    stroke(a(G, 200), 2f);
                    p.setPathEffect(new DashPathEffect(new float[]{14, 9}, -t * 30f));
                    c.drawCircle(cx, BY, r + 12, p);
                    p.setPathEffect(null);
                }
                icon(c, i, cx, BY, (i == 2 || on || pr) ? G : W);
                ctxt(c, BL[i], cx, 1772, 14, i == 2 ? G : W, false);
            }
        }

        void icon(Canvas c, int i, float x, float y, int col) {
            p.setPathEffect(null);
            p.setStrokeCap(Paint.Cap.ROUND);
            switch (i) {
                case 0: { // handset
                    stroke(col, 9f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    c.save();
                    c.rotate(-20, x, y);
                    rect.set(x - 4, y - 24, x + 34, y + 24);
                    c.drawArc(rect, 105, 150, false, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(col);
                    c.drawRoundRect(x - 22, y - 30, x - 4, y - 16, 4, 4, p);
                    c.drawRoundRect(x - 22, y + 16, x - 4, y + 30, 4, 4, p);
                    c.restore();
                    break;
                }
                case 1: { // flashlight
                    stroke(col, 3.5f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    path.reset();
                    path.moveTo(x - 15, y - 28);
                    path.lineTo(x + 15, y - 28);
                    path.lineTo(x + 8, y - 12);
                    path.lineTo(x - 8, y - 12);
                    path.close();
                    c.drawPath(path, p);
                    c.drawRoundRect(x - 8, y - 12, x + 8, y + 28, 5, 5, p);
                    c.drawLine(x, y - 2, x, y + 8, p);
                    if (torchOn) {
                        c.drawLine(x - 22, y - 34, x - 30, y - 42, p);
                        c.drawLine(x + 22, y - 34, x + 30, y - 42, p);
                        c.drawLine(x, y - 38, x, y - 48, p);
                    }
                    break;
                }
                case 2: { // fingerprint
                    stroke(col, 3f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    for (int k = 1; k <= 5; k++) {
                        float r = 8f * k;
                        rect.set(x - r, y - r, x + r, y + r);
                        c.drawArc(rect, 195 + k * 6, 260 - k * 6, false, p);
                    }
                    c.drawLine(x, y - 4, x, y + 12, p);
                    break;
                }
                case 3: { // camera
                    stroke(col, 3.5f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    c.drawRoundRect(x - 28, y - 14, x + 28, y + 22, 7, 7, p);
                    c.drawRect(x - 10, y - 23, x + 10, y - 14, p);
                    c.drawCircle(x, y + 4, 11, p);
                    break;
                }
                default: { // apps grid
                    stroke(col, 3.5f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    for (int gx = 0; gx < 2; gx++)
                        for (int gy = 0; gy < 2; gy++) {
                            float l = x - 21 + gx * 24, tp = y - 21 + gy * 24;
                            c.drawRoundRect(l, tp, l + 18, tp + 18, 4, 4, p);
                        }
                    break;
                }
            }
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        void footer(Canvas c, float t) {
            c.save();
            c.rotate(-13, 790, 1800);
            p.setTypeface(MONOBI);
            String[] ln = {"NO", "SYSTEM", "IS SAFE"};
            for (int i = 0; i < 3; i++) {
                p.setStyle(Paint.Style.FILL);
                p.setTypeface(MONOBI);
                p.setTextSize(28);
                p.setColor(G);
                p.setTextAlign(Paint.Align.LEFT);
                c.drawText(ln[i], 742, 1776 + i * 28f, p);
            }
            c.restore();

            p.setStyle(Paint.Style.FILL);
            p.setColor(a(GD, 120));
            c.drawRoundRect(312, 1830, 532, 1838, 4, 4, p);
            float seg = 70;
            float x = 312 + ((t * 120f) % (220f + seg)) - seg;
            c.save();
            c.clipRect(312, 1828, 532, 1840);
            p.setColor(G);
            c.drawRoundRect(x, 1830, x + seg, 1838, 4, 4, p);
            c.restore();
        }

        void overlay(Canvas c, float t, long upNow) {
            // scanlines
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(0x22000000);
            c.drawLines(scan, p);
            // moving beam
            c.save();
            c.translate(0, ((t * 110f) % 2000f) - 140f);
            p.setStyle(Paint.Style.FILL);
            p.setShader(beam);
            c.drawRect(0, 0, 844, 140, p);
            p.setShader(null);
            c.restore();

            // CRT flicker
            p.setStyle(Paint.Style.FILL);
            p.setColor(a(Color.BLACK, rnd.nextInt(16)));
            c.drawRect(0, 0, 844, 1855, p);

            // glitch bars
            if (t % 4.7f < 0.18f) {
                for (int k = 0; k < 7; k++) {
                    p.setColor(a(k == 0 ? 0xFFFFFFFF : G, 50 + rnd.nextInt(120)));
                    float gx = rnd.nextInt(600), gy = rnd.nextInt(1855);
                    c.drawRect(gx, gy, gx + 60 + rnd.nextInt(240), gy + 2 + rnd.nextInt(6), p);
                }
            }

            // intrusion alert / trace evaded
            float at = t % 30f;
            if (at > 22f && at < 25.5f) {
                boolean blink = ((int) (at * 4)) % 2 == 0;
                stroke(a(0xFFFF2222, blink ? 210 : 90), 6f);
                c.drawRect(3, 3, 841, 1852, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xDD250000);
                c.drawRect(110, 930, 734, 1012, p);
                stroke(0xFFFF2222, 3f);
                c.drawRect(110, 930, 734, 1012, p);
                ctxt(c, "!! INTRUSION DETECTED !!", 422, 970, 28, blink ? 0xFFFF4444 : 0xFFFF9999, true);
                ctxt(c, "countermeasures active...", 422, 996, 15, 0xFFFFB0B0, false);
            } else if (at >= 25.5f && at < 27.5f) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xDD001A0A);
                c.drawRect(110, 930, 734, 1012, p);
                stroke(G, 3f);
                c.drawRect(110, 930, 734, 1012, p);
                ctxt(c, "TRACE EVADED", 422, 970, 30, G, true);
                ctxt(c, "// you are a ghost", 422, 996, 15, W, false);
            }

            // touch ripple
            float rt = (upNow - rippleAt) / 1000f;
            if (rt >= 0 && rt < 0.8f) {
                float k = rt / 0.8f;
                stroke(a(G, (int) ((1 - k) * 220)), 3f);
                c.drawCircle(rippleX, rippleY, 10 + k * 70, p);
            }

            // ACCESS GRANTED banner after tapping UNLOCK
            float gt = (upNow - grantedAt) / 1000f;
            if (gt >= 0 && gt < 2.5f) {
                box(c, 172, 1500, 672, 1570);
                ctxt(c, "ACCESS GRANTED", 422, 1546, 34, G, true);
            }
        }
    }
}
