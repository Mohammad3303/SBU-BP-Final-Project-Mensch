package com.elyac.proplanner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PlannerAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "proplanner_test_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        String title = intent != null ? intent.getStringExtra("title") : null;
        String body = intent != null ? intent.getStringExtra("body") : null;
        if (title == null || title.isEmpty()) title = "Pro Planner";
        if (body == null) body = "";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Pro Planner", NotificationManager.IMPORTANCE_HIGH));
        }
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new android.app.Notification.Builder(context, CHANNEL_ID)
            : new android.app.Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle(title)
         .setContentText(body)
         .setAutoCancel(true)
         .setCategory(android.app.Notification.CATEGORY_REMINDER);
        nm.notify((int)(System.currentTimeMillis() & 0x7fffffff), b.build());
    }
}
