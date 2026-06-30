package com.cy.loxia;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String itemId = intent.getStringExtra("item_id");
        String itemName = intent.getStringExtra("item_name");
        String statusLabel = intent.getStringExtra("status_label");
        int daysBefore = intent.getIntExtra("days_before", 0);
        String title = intent.getStringExtra("title");
        int soundMode = intent.getIntExtra("sound_mode", 0);
        boolean showBadge = intent.getBooleanExtra("show_badge", true);

        if (itemName == null) itemName = "裙子";
        if (statusLabel == null) statusLabel = "补尾款";
        if (title == null) title = "LoXia 裙匣提醒";

        String content;
        if (daysBefore <= 0) {
            content = "「" + itemName + "」的" + statusLabel + "今天到期";
        } else {
            content = "「" + itemName + "」的" + statusLabel + "将在" + daysBefore + "天后到期";
        }

        // NotificationChannel 已在 LoXiaApp.onCreate() 中创建，此处无需重复创建
        String channelId = AlarmScheduler.CHANNEL_ID;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(soundMode == 2 ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSilent(soundMode == 2);

        if (!showBadge) {
            builder.setNumber(0);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = itemId != null ? (itemId + statusLabel).hashCode() : (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
        }
    }
}
