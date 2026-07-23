package com.cy.loxia;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AlarmScheduler {
    public static final String CHANNEL_ID = "loxia_reminder";
    private static final String CHANNEL_NAME = "裙子专属提醒";
    private static final String PREFS_NAME = "loxia_prefs";

    /**
     * 生成稳定的 requestCode，避免 String.hashCode() 碰撞。
     * 使用 FNV-1a 算法对 itemId + statusKey + daysBefore 组合生成唯一整数。
     */
    private static int generateRequestCode(String itemId, String statusKey, int daysBefore) {
        String composite = itemId + "|" + statusKey + "|" + daysBefore;
        int hash = 0x811c9dc5; // FNV offset basis
        for (int i = 0; i < composite.length(); i++) {
            hash ^= composite.charAt(i);
            hash *= 0x01000193; // FNV prime
        }
        return hash;
    }

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("裙子状态到期提醒");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void scheduleReminderForStatus(Context context, DressItem item,
            String statusKey, String statusLabel, String targetDate, int advanceDays) {
        if (targetDate == null || targetDate.isEmpty()) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int hour = prefs.getInt("notif_time_hour", 8);
            int minute = prefs.getInt("notif_time_minute", 0);
            int frequency = prefs.getInt("notif_frequency", 0);
            boolean showBadge = prefs.getBoolean("notif_show_badge", true);
            int soundMode = prefs.getInt("notif_sound_mode", 0);
            boolean silentMode = prefs.getBoolean("notif_silent_mode", false);
            String title = prefs.getString("notif_title", "LoXia 裙子专属提醒");

            LocalDate dueDate = LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (frequency == 2) { // Recurring daily
                for (int d = advanceDays; d >= 0; d--) {
                    scheduleSingle(context, item, statusKey, statusLabel, dueDate, d,
                            hour, minute, title, soundMode, silentMode, showBadge);
                }
            } else if (frequency == 1) { // Multiple: advance + on-day
                scheduleSingle(context, item, statusKey, statusLabel, dueDate, advanceDays,
                        hour, minute, title, soundMode, silentMode, showBadge);
                scheduleSingle(context, item, statusKey, statusLabel, dueDate, 0,
                        hour, minute, title, soundMode, silentMode, showBadge);
            } else { // Once: on the exact day
                scheduleSingle(context, item, statusKey, statusLabel, dueDate, 0,
                        hour, minute, title, soundMode, silentMode, showBadge);
            }
        } catch (Exception e) {
            Log.e("AlarmScheduler", "Failed to schedule reminder for " + item.getName(), e);
        }
    }

    private static void scheduleSingle(Context context, DressItem item,
            String statusKey, String statusLabel, LocalDate dueDate, int daysBefore,
            int hour, int minute, String title, int soundMode, boolean silentMode, boolean showBadge) {
        LocalDate triggerDate = dueDate.minusDays(daysBefore);
        LocalDateTime triggerTime = LocalDateTime.of(triggerDate, LocalTime.of(hour, minute));
        if (triggerTime.isBefore(LocalDateTime.now())) return;

        long triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("item_id", item.getId());
        intent.putExtra("item_name", item.getName());
        intent.putExtra("status_key", statusKey);
        intent.putExtra("status_label", statusLabel);
        intent.putExtra("days_before", daysBefore);
        intent.putExtra("title", title);
        intent.putExtra("sound_mode", soundMode);
        intent.putExtra("silent_mode", silentMode);
        intent.putExtra("show_badge", showBadge);

        int requestCode = generateRequestCode(item.getId(), statusKey, daysBefore);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
                    return;
                }
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, DressItem item) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] statusKeys = {"daiqiang", "yixiang", "dingjin", "buweikuan"};
        for (String statusKey : statusKeys) {
            for (int d = 0; d <= 365; d++) {
                Intent intent = new Intent(context, ReminderReceiver.class);
                int requestCode = generateRequestCode(item.getId(), statusKey, d);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    public static void scheduleAllPendingReminders(Context context, DataRepository repository) {
        // 使用 runBlocking 调用 suspend 函数（已在后台线程）
        List<DressItem> items;
        try {
            items = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getDressItemsAsync(continuation)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("AlarmScheduler", "Failed to load dress items", e);
            return;
        }
        for (DressItem item : items) {
            String status = item.getStatus();
            if (status == null) continue;

            if (status.contains("待抢") && item.getDaiqiangDate() != null && !item.getDaiqiangDate().isEmpty()) {
                int advance = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getInt("notif_advance_daiqiang", 3);
                scheduleReminderForStatus(context, item, "daiqiang", "待抢", item.getDaiqiangDate(), advance);
            }
            if (status.contains("付意向") && item.getYixiangDate() != null && !item.getYixiangDate().isEmpty()) {
                int advance = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getInt("notif_advance_yixiang", 3);
                scheduleReminderForStatus(context, item, "yixiang", "付意向", item.getYixiangDate(), advance);
            }
            if (status.contains("付定金") && item.getDingjinDate() != null && !item.getDingjinDate().isEmpty()) {
                int advance = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getInt("notif_advance_dingjin", 3);
                scheduleReminderForStatus(context, item, "dingjin", "付定金", item.getDingjinDate(), advance);
            }
            if (status.contains("补尾款") && item.getBuweikuanDate() != null && !item.getBuweikuanDate().isEmpty()) {
                int advance = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getInt("notif_advance_buweikuan", 3);
                scheduleReminderForStatus(context, item, "buweikuan", "补尾款", item.getBuweikuanDate(), advance);
            }
        }
    }

    public static void cancelAllReminders(Context context, DataRepository repository) {
        // 使用 runBlocking 调用 suspend 函数（已在后台线程）
        List<DressItem> items;
        try {
            items = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getDressItemsAsync(continuation)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("AlarmScheduler", "Failed to load dress items", e);
            return;
        }
        for (DressItem item : items) {
            cancelReminder(context, item);
        }
    }
}
