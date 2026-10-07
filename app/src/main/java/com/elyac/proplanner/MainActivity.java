package com.elyac.proplanner;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.webkit.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS=9001;
    private WebView webView;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(8,8,16));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(8,8,16));
        webView=new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(8,8,16));
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setAllowFileAccess(false); s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        Bridge bridge=new Bridge(this);
        webView.addJavascriptInterface(bridge,"ProPlannerNative");
        webView.addJavascriptInterface(bridge,"ProPlannerAndroid");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        String html=readAsset("index.html");
        webView.loadDataWithBaseURL("https://pro-planner-by-elyac.workers.dev/",html,"text/html","UTF-8","https://pro-planner-by-elyac.workers.dev/");
    }

    private String readAsset(String name){
        try(java.io.InputStream in=getAssets().open(name)){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] b=new byte[16384]; int n;
            while((n=in.read(b))!=-1) out.write(b,0,n);
            return out.toString("UTF-8");
        }catch(Exception e){ return "<html><body style='background:#080810;color:white;padding:30px'>Pro Planner load error.</body></html>"; }
    }

    private boolean notificationsEnabled(){
        return Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean exactAlarmEnabled(){
        return Build.VERSION.SDK_INT<31 || ((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
    }
    private void requestNotifications(){
        if(Build.VERSION.SDK_INT>=33 && !notificationsEnabled()) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_NOTIFICATIONS);
        else openNotificationSettings();
    }
    private void openNotificationSettings(){
        try{ Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS); i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()); startActivity(i); }catch(Exception ignored){}
    }
    private void openExactAlarmSettings(){
        try{ if(Build.VERSION.SDK_INT>=31){ Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())); startActivity(i);} }catch(Exception ignored){}
    }
    private PendingIntent pendingIntent(String tag, String title, String body){
        Intent i=new Intent(this,PlannerAlarmReceiver.class).putExtra("title",title).putExtra("body",body);
        int req=Math.abs((tag==null?"pp":tag).hashCode());
        return PendingIntent.getBroadcast(this,req,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void scheduleOne(String tag,String title,String body,long when){
        if(when<=System.currentTimeMillis())return;
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); if(am==null)return;
        PendingIntent pi=pendingIntent(tag,title,body);
        if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); else am.setExact(AlarmManager.RTC_WAKEUP,when,pi);
    }
    private void cancelOne(String tag){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); if(am==null)return;
        PendingIntent pi=pendingIntent(tag,"",""); am.cancel(pi); pi.cancel();
    }
    private String statusJson(){
        try{
            JSONObject o=new JSONObject();
            o.put("notifications",notificationsEnabled());
            o.put("exactAlarm",exactAlarmEnabled());
            o.put("battery",true);
            o.put("oem","generic");
            o.put("oemTitle","دستگاه");
            o.put("oemSummary","تنظیمات اختیاری");
            return o.toString();
        }catch(Exception e){return "{}";}
    }
    private String deviceJson(){
        try{
            DisplayMetrics dm=getResources().getDisplayMetrics();
            JSONObject o=new JSONObject();
            o.put("manufacturer",Build.MANUFACTURER==null?"":Build.MANUFACTURER);
            o.put("brand",Build.BRAND==null?"":Build.BRAND);
            o.put("model",Build.MODEL==null?"":Build.MODEL);
            o.put("android",Build.VERSION.RELEASE==null?"":Build.VERSION.RELEASE);
            o.put("sdk",Build.VERSION.SDK_INT);
            o.put("widthPx",dm.widthPixels); o.put("heightPx",dm.heightPixels);
            o.put("widthDp",Math.round(dm.widthPixels/dm.density)); o.put("heightDp",Math.round(dm.heightPixels/dm.density));
            o.put("density",dm.density);
            return o.toString();
        }catch(Exception e){return "{}";}
    }

    public class Bridge {
        private final Context ctx; Bridge(Context c){ctx=c;}
        @JavascriptInterface public boolean areNotificationsEnabled(){return notificationsEnabled();}
        @JavascriptInterface public boolean hasNotificationPermission(){return notificationsEnabled();}
        @JavascriptInterface public void requestNotificationPermission(){requestNotifications();}
        @JavascriptInterface public void openNotificationSettings(){MainActivity.this.openNotificationSettings();}
        @JavascriptInterface public boolean areExactAlarmsAllowed(){return exactAlarmEnabled();}
        @JavascriptInterface public boolean hasExactAlarmPermission(){return exactAlarmEnabled();}
        @JavascriptInterface public void openExactAlarmSettings(){MainActivity.this.openExactAlarmSettings();}
        @JavascriptInterface public String getNotificationAccessStatusJson(){return statusJson();}
        @JavascriptInterface public String getDeviceProfileJson(){return deviceJson();}
        @JavascriptInterface public boolean isBackgroundExecutionAllowed(){return true;}
        @JavascriptInterface public void requestBatteryOptimizationExemption(){}
        @JavascriptInterface public void openAutoStartSettings(){}
        @JavascriptInterface public String getSoundName(String type){return "";}
        @JavascriptInterface public void pickSound(String type){}
        @JavascriptInterface public void resetSound(String type){}
        @JavascriptInterface public void setLanguage(String lang){}
        @JavascriptInterface public void setTheme(String theme){}
        @JavascriptInterface public void showNotificationSetup(){}
        @JavascriptInterface public void openExternalUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}}
        @JavascriptInterface public void scheduleNotifications(String json){
            try{
                JSONArray arr=new JSONArray(json==null?"[]":json);
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.optJSONObject(i); if(o==null)continue;
                    long when=o.optLong("whenMs",0); if(when<=0) when=o.optLong("at",0);
                    scheduleOne(o.optString("id","pp-"+i),o.optString("title","Pro Planner"),o.optString("body",""),when);
                }
            }catch(Exception ignored){}
        }
        @JavascriptInterface public void cancelAbsolute(String tag){cancelOne(tag);}
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQUEST_NOTIFICATIONS && webView!=null){
            webView.evaluateJavascript("try{window.renderNotificationSetupCard&&window.renderNotificationSetupCard();window.renderNotifStatus&&window.renderNotifStatus();}catch(e){}",null);
        }
    }
}
