package com.shashwat.hackwall;

import android.Manifest;
import android.app.*;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

public class MainActivity extends androidx.fragment.app.FragmentActivity {
  LinearLayout root, panel; TextView status; HudView hud; Dialog scanDialog; boolean torchEnabled=false; int green=Color.rgb(0,255,112); int dim=Color.rgb(0,150,68); int red=Color.rgb(255,65,80);
  @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE); build(); }
  GradientDrawable shape(int stroke){ GradientDrawable d=new GradientDrawable(); d.setColor(Color.rgb(2,12,8)); d.setCornerRadius(dp(14)); d.setStroke(dp(1),stroke); return d; }
  int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);} 
  TextView text(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextColor(color); t.setTextSize(size); t.setTypeface(android.graphics.Typeface.MONOSPACE); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
  void build(){
    ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.BLACK);
    root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),dp(24)); root.setBackgroundColor(Color.BLACK); scroll.addView(root); setContentView(scroll);
    TextView top=text("● SYSTEM ONLINE     CYBER-OPS / 02.8",12,green); root.addView(top,new LinearLayout.LayoutParams(-1,dp(30)));
    hud=new HudView(this); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(310)); hp.topMargin=dp(6); root.addView(hud,hp);
    TextView title=text("SECURE ACCESS TERMINAL",18,Color.WHITE); title.setGravity(Gravity.CENTER); title.setTypeface(null,1); root.addView(title,new LinearLayout.LayoutParams(-1,dp(38)));
    TextView sub=text("BIOMETRIC GATE  •  LOCAL DEVICE AUTH",10,dim); sub.setGravity(Gravity.CENTER); root.addView(sub,new LinearLayout.LayoutParams(-1,dp(24)));
    status=text("> READY — AUTHENTICATION REQUIRED",12,green); status.setPadding(dp(12),dp(10),dp(12),dp(10)); status.setBackground(shape(dim)); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(8); root.addView(status,sp);
    button("◉   INITIATE BIOMETRIC / FACE SCAN",green,Color.BLACK,()->authenticate());
    button("⌗   DEMO ACCESS CODE",dim,Color.WHITE,()->pinDialog());
    button("▦   SET LIVE WALLPAPER",dim,Color.WHITE,()->{ try { Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER); i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,HackWallpaperService.class)); startActivity(i); } catch(Exception e){ launch(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)); }});
    button("⌁   START LOCK-SCREEN CONTROLS",dim,Color.WHITE,()->{ try { if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},44);} else {startForegroundService(new Intent(this,HackNotifService.class).setAction(HackNotifService.A_START)); setStatus("LOCK-SCREEN CONTROL NOTIFICATION STARTED",green);} } catch(Exception e){setStatus("LOCK-SCREEN CONTROLS UNAVAILABLE",red);} });
    TextView tools=text("QUICK ACTIONS  /  DEVICE SHORTCUTS",11,dim); tools.setPadding(0,dp(18),0,dp(8)); root.addView(tools);
    LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); root.addView(row);
    small(row,"☎\nCALL",()->launch(new Intent(Intent.ACTION_DIAL)));
    small(row,"▣\nCAMERA",()->launch(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)));
    small(row,"▤\nAPPS",()->launch(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)));
    small(row,"ϟ\nTORCH",()->toggleTorch());
    TextView logs=text("LIVE SYSTEM LOG  /  VISUAL SIMULATION",11,dim); logs.setPadding(0,dp(18),0,dp(8)); root.addView(logs);
    TextView feed=text("[OK] HUD renderer initialized\n[OK] Device status panel ready\n[WAIT] Awaiting local authentication\n[INFO] Network breach effects are visual only",11,green); feed.setPadding(dp(12),dp(12),dp(12),dp(12)); feed.setBackground(shape(0xff07552b)); root.addView(feed,new LinearLayout.LayoutParams(-1,-2));
    TextView foot=text("NO REMOTE ACCESS • NO DATA EXFILTRATION • YOU CONTROL THIS DEVICE",9,0xff6d9b7c); foot.setGravity(Gravity.CENTER); foot.setPadding(0,dp(18),0,0); root.addView(foot);
  }
  void button(String label,int stroke,int fg,Runnable r){ TextView b=text(label,13,fg); b.setGravity(Gravity.CENTER); b.setPadding(dp(12),dp(14),dp(12),dp(14)); b.setBackground(shape(stroke)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8); root.addView(b,lp); b.setOnClickListener(v->{b.animate().scaleX(.97f).scaleY(.97f).setDuration(70).withEndAction(()->b.animate().scaleX(1).scaleY(1).setDuration(90).start()).start(); r.run();}); }
  void small(LinearLayout row,String label,Runnable r){ TextView b=text(label,10,green); b.setGravity(Gravity.CENTER); b.setPadding(dp(2),dp(12),dp(2),dp(12)); b.setBackground(shape(dim)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(66),1); lp.setMargins(dp(2),0,dp(2),0); row.addView(b,lp); b.setOnClickListener(v->r.run()); }
  void launch(Intent i){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){setStatus("ACTION UNAVAILABLE ON THIS DEVICE",red);}}
  void setStatus(String s,int color){status.setText("> "+s);status.setTextColor(color);hud.invalidate();}
  void authenticate(){
    showScanDialog();
    androidx.biometric.BiometricManager bm=androidx.biometric.BiometricManager.from(this);
    int can=bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG);
    if(can==androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS){
      setStatus("BIOMETRIC SENSOR ACTIVE — WAITING",green); Executor ex=ContextCompat.getMainExecutor(this);
      BiometricPrompt prompt=new BiometricPrompt(this,ex,new BiometricPrompt.AuthenticationCallback(){
        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){super.onAuthenticationSucceeded(result);dismissScanDialog();setStatus("AUTHENTICATED BY ANDROID BIOMETRICS",green);hud.setSuccess(true);}
        @Override public void onAuthenticationFailed(){super.onAuthenticationFailed();setStatus("BIOMETRIC NOT RECOGNIZED — TRY AGAIN",red);hud.setSuccess(false);}
        @Override public void onAuthenticationError(int code,CharSequence msg){super.onAuthenticationError(code,msg);dismissScanDialog();setStatus("AUTHENTICATION FAILED: "+msg,red);hud.setSuccess(false);}
      });
      prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("CyberOps secure access").setSubtitle("Authenticate using enrolled device biometrics").setNegativeButtonText("Cancel").setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG).build());
    } else {
      android.app.KeyguardManager km=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
      if(km!=null && km.isDeviceSecure()){
        Intent confirm=km.createConfirmDeviceCredentialIntent("CyberOps secure access","Confirm your actual device PIN, pattern or password");
        if(confirm!=null){dismissScanDialog();setStatus("BIOMETRICS UNAVAILABLE — DEVICE CREDENTIAL REQUIRED",dim);startActivityForResult(confirm,81);return;}
      }
      dismissScanDialog();setStatus("AUTHENTICATION FAILED — NO ENROLLED STRONG BIOMETRICS OR DEVICE CREDENTIAL",red);hud.setSuccess(false);
    }
  }
  void dismissScanDialog(){ if(scanDialog!=null && scanDialog.isShowing()){scanDialog.dismiss();} scanDialog=null; }
  void showScanDialog(){
    dismissScanDialog(); scanDialog=new Dialog(this); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(18),dp(20),dp(18)); box.setBackground(shape(green));
    TextView head=text("// BIOMETRIC GATE",13,green); head.setGravity(Gravity.CENTER); box.addView(head,new LinearLayout.LayoutParams(-1,dp(30)));
    FaceScanView face=new FaceScanView(this); box.addView(face,new LinearLayout.LayoutParams(-1,dp(230)));
    TextView caption=text("VISUAL SCAN SIMULATION",11,green); caption.setGravity(Gravity.CENTER); box.addView(caption,new LinearLayout.LayoutParams(-1,dp(26)));
    TextView note=text("Real verification is handled by Android.\nNo face image is captured by this animation.",10,0xffa6d9b7); note.setGravity(Gravity.CENTER); box.addView(note,new LinearLayout.LayoutParams(-1,dp(44)));
    scanDialog.setContentView(box); Window w=scanDialog.getWindow(); if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(getResources().getDisplayMetrics().widthPixels-dp(44),-2);} scanDialog.setCancelable(true); scanDialog.setOnCancelListener(d->{scanDialog=null;setStatus("AUTHENTICATION CANCELLED",red);}); scanDialog.show(); if(w!=null)w.setLayout(getResources().getDisplayMetrics().widthPixels-dp(44),-2);
  }
  class FaceScanView extends View { Paint fp=new Paint(3); long born=SystemClock.uptimeMillis(); Handler fh=new Handler(Looper.getMainLooper()); Runnable fr=new Runnable(){public void run(){invalidate();fh.postDelayed(this,40);}}; FaceScanView(Context c){super(c);fh.post(fr);} @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2; c.drawColor(Color.rgb(2,10,7)); fp.setColor(0xff064326);fp.setStyle(Paint.Style.STROKE);fp.setStrokeWidth(1);for(int x=0;x<w;x+=18)c.drawLine(x,0,x,h,fp);for(int y=0;y<h;y+=18)c.drawLine(0,y,w,y,fp);float t=(SystemClock.uptimeMillis()-born)/1000f;fp.setColor(0xff00ff70);fp.setStrokeWidth(2);c.drawRoundRect(cx-w*.29f,cy-h*.39f,cx+w*.29f,cy+h*.39f,18,18,fp); c.drawOval(cx-w*.16f,cy-h*.27f,cx+w*.16f,cy+h*.20f,fp);c.drawArc(cx-w*.19f,cy-h*.02f,cx+w*.19f,cy+h*.30f,15,150,false,fp);c.drawArc(cx-w*.19f,cy-h*.02f,cx+w*.19f,cy+h*.30f,195,150,false,fp);c.drawLine(cx-w*.13f,cy-h*.09f,cx-w*.04f,cy-h*.09f,fp);c.drawLine(cx+w*.04f,cy-h*.09f,cx+w*.13f,cy-h*.09f,fp);c.drawLine(cx,cy-h*.03f,cx-w*.02f,cy+h*.04f,fp);c.drawLine(cx-w*.07f,cy+h*.12f,cx+w*.07f,cy+h*.12f,fp);float sy=cy-h*.34f+(float)((t%2.2f)/2.2f)*h*.68f;fp.setStyle(Paint.Style.FILL);fp.setColor(0x3500ff70);c.drawRect(cx-w*.27f,sy-8,cx+w*.27f,sy+8,fp);fp.setColor(0xff00ff70);c.drawRect(cx-w*.27f,sy-1,cx+w*.27f,sy+1,fp);fp.setTextSize(10);fp.setTypeface(Typeface.MONOSPACE);c.drawText("FRAME ANALYSIS // DEMO",8,h-8,fp);}@Override protected void onDetachedFromWindow(){fh.removeCallbacks(fr);super.onDetachedFromWindow();}}
  @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==81){if(res==RESULT_OK){setStatus("DEVICE CREDENTIAL VERIFIED BY ANDROID",green);hud.setSuccess(true);}else{setStatus("AUTHENTICATION FAILED — DEVICE CREDENTIAL NOT VERIFIED",red);hud.setSuccess(false);}}}
  void pinDialog(){ final EditText e=new EditText(this); e.setSingleLine(true); e.setInputType(2); e.setHint("Enter demo code 2468"); e.setTextColor(green); e.setHintTextColor(dim); LinearLayout box=new LinearLayout(this); box.setPadding(dp(20),dp(8),dp(20),dp(4)); box.addView(e); new AlertDialog.Builder(this).setTitle("LOCAL DEMO CODE").setMessage("Demo only — this is NOT your Android lock-screen PIN.").setView(box).setNegativeButton("CANCEL",(d,w)->{}).setPositiveButton("VERIFY",(d,w)->{if("2468".equals(e.getText().toString())){setStatus("DEMO CODE ACCEPTED — NOT DEVICE UNLOCK",green);hud.setSuccess(false);}else{setStatus("AUTHENTICATION FAILED — INVALID DEMO CODE",red);hud.setSuccess(false);}}).show(); }
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==45){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)toggleTorch();else setStatus("TORCH PERMISSION DENIED",red);}}
  void toggleTorch(){if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},45);return;}try{android.hardware.camera2.CameraManager cm=(android.hardware.camera2.CameraManager)getSystemService(CAMERA_SERVICE); for(String id:cm.getCameraIdList()){android.hardware.camera2.CameraCharacteristics c=cm.getCameraCharacteristics(id);Boolean flash=c.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer facing=c.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(flash)&&facing!=null&&facing==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK){torchEnabled=!torchEnabled;cm.setTorchMode(id,torchEnabled);setStatus(torchEnabled?"TORCH ENABLED":"TORCH DISABLED",green);return;}}setStatus("NO COMPATIBLE TORCH FOUND",red);}catch(Exception e){setStatus("TORCH UNAVAILABLE — CHECK PERMISSIONS",red);}}
  class HudView extends View { Paint p=new Paint(3); long start=SystemClock.uptimeMillis(); boolean success=false; Handler h=new Handler(Looper.getMainLooper()); Runnable tick=new Runnable(){public void run(){invalidate();h.postDelayed(this,45);}};
    HudView(Context c){super(c);h.post(tick);setLayerType(View.LAYER_TYPE_SOFTWARE,null);} void setSuccess(boolean b){success=b;invalidate();}
    protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),hh=getHeight(),cx=w/2,cy=hh*.48f; c.drawColor(Color.BLACK); p.setTypeface(Typeface.MONOSPACE); p.setStrokeWidth(1); p.setStyle(Paint.Style.STROKE); p.setColor(0xff063b20); for(int x=0;x<w;x+=18)c.drawLine(x,0,x,hh,p);for(int y=0;y<hh;y+=18)c.drawLine(0,y,w,y,p);
      float t=(SystemClock.uptimeMillis()-start)/1000f; p.setColor(0xff00a84a); for(int i=0;i<4;i++){p.setStrokeWidth(i==0?2:1); c.drawCircle(cx,cy,Math.min(w*.39f,hh*.39f)-i*13+(float)Math.sin(t+i)*2,p);} c.save();c.rotate(t*17,cx,cy); p.setColor(0xff00ff70); p.setStrokeWidth(2); c.drawArc(cx-w*.34f,cy-w*.34f,cx+w*.34f,cy+w*.34f,15,80,false,p);c.drawArc(cx-w*.34f,cy-w*.34f,cx+w*.34f,cy+w*.34f,195,48,false,p);c.restore();
      p.setStyle(Paint.Style.FILL); p.setColor(0x1800ff70); c.drawCircle(cx,cy,w*.22f,p); p.setColor(success?0xff00ff70:0xffb7ffd2); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); Path tri=new Path();tri.moveTo(cx,cy-w*.22f);tri.lineTo(cx+w*.22f,cy+w*.16f);tri.lineTo(cx-w*.22f,cy+w*.16f);tri.close();c.drawPath(tri,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(success?0xff00ff70:0xffd9ffe6);Path eye=new Path();eye.moveTo(cx-w*.12f,cy);eye.quadTo(cx,cy-w*.09f,cx+w*.12f,cy);eye.quadTo(cx,cy+w*.09f,cx-w*.12f,cy);eye.close();c.drawPath(eye,p);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,w*.027f,p);
      float scanY=cy-w*.28f+(float)((t%2.4f)/2.4f)*w*.56f;p.setColor(success?0xff00ff70:0xff00e85e);p.setStrokeWidth(2);c.drawLine(cx-w*.28f,scanY,cx+w*.28f,scanY,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(12);p.setTypeface(Typeface.MONOSPACE);p.setColor(0xff00ff70);c.drawText(success?"LOCAL AUTH VERIFIED":"BIOMETRIC GATE // STANDBY",cx,hh-25,p);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(9);p.setColor(0xff287c4a);c.drawText("SYS 01  /  ENCRYPTED SESSION",10,hh-7,p);
    }
  }
  @Override protected void onDestroy(){dismissScanDialog();super.onDestroy();}
}
