package com.elyac.proplanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 9001;
    private WebView webView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(8,8,16));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(8,8,16));

        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(8,8,16));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(this), "ProPlannerNative");
        webView.addJavascriptInterface(new Bridge(this), "ProPlannerAndroid");
        setContentView(webView);

        webView.loadDataWithBaseURL(
            "https://pro-planner-by-elyac.workers.dev/",
            readAsset("index.html"),
            "text/html", "UTF-8",
            "https://pro-planner-by-elyac.workers.dev/");
    }

    private String readAsset(String name) {
        try (java.io.InputStream in=getAssets().open(name)) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] b=new byte[16384]; int n;
            while((n=in.read(b))!=-1) out.write(b,0,n);
            return out.toString("UTF-8");
        } catch(Exception e) {
            return "<html><body style='background:#080810;color:white;padding:30px'>Pro Planner could not load.</body></html>";
        }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored) {}
    }

    private boolean notificationsEnabled() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }

    private boolean exactAlarmEnabled() {
        return Build.VERSION.SDK_INT < 31 || ((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsEnabled()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            openNotificationSettings();
        }
    }

    private void openNotificationSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        } catch(Exception ignored) {}
    }

    private void openExactAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                i.setData(Uri.parse("package:"+getPackageName()));
                startActivity(i);
            }
        } catch(Exception ignored) {}
    }

    private void scheduleOne(String tag,String title,String body,long whenMs) {
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        if(am==null) return;
        Intent i=new Intent(this, PlannerAlarmReceiver.class)
            .putExtra("title",title).putExtra("body",body);
        int req=Math.abs(tag==null?0:tag.hashCode());
        PendingIntent pi=PendingIntent.getBroadcast(
            this, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenMs,pi);
        else am.setExact(AlarmManager.RTC_WAKEUP,whenMs,pi);
    }

    public class Bridge {
        private final Context ctx;
        Bridge(Context c){ctx=c;}

        @JavascriptInterface public boolean areNotificationsEnabled(){ return notificationsEnabled(); }
        @JavascriptInterface public boolean hasNotificationPermission(){ return notificationsEnabled(); }
        @JavascriptInterface public void requestNotificationPermission(){ requestNotifications(); }
        @JavascriptInterface public void openNotificationSettings(){ openNotificationSettings(); }

        @JavascriptInterface public boolean areExactAlarmsAllowed(){ return exactAlarmEnabled(); }
        @JavascriptInterface public boolean hasExactAlarmPermission(){ return exactAlarmEnabled(); }
        @JavascriptInterface public void openExactAlarmSettings(){ openExactAlarmSettings(); }

        @JavascriptInterface public boolean isBackgroundExecutionAllowed(){ return true; }
        @JavascriptInterface public void requestBatteryOptimizationExemption(){}

        @JavascriptInterface public String getSoundName(String type){ return ""; }
        @JavascriptInterface public void pickSound(String type){}
        @JavascriptInterface public void resetSound(String type){}

        @JavascriptInterface public void setLanguage(String lang){}
        @JavascriptInterface public void setTheme(String theme){}
        @JavascriptInterface public void showNotificationSetup(){}

        @JavascriptInterface public void openExternalUrl(String url){ openUrl(url); }

        @JavascriptInterface public void scheduleNotifications(String json){
            try{
                JSONArray arr=new JSONArray(json==null?"[]":json);
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.optJSONObject(i);
                    if(o==null) continue;
                    long when=o.optLong("whenMs",0);
                    if(when<=System.currentTimeMillis()) continue;
                    String id=o.optString("id","pp-"+i);
                    String title=o.optString("title","Pro Planner");
                    String body=o.optString("body","");
                    scheduleOne(id,title,body,when);
                }
            }catch(Exception ignored){}
        }
    }
}
