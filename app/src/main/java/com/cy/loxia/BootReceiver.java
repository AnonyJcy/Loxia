package com.cy.loxia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            boolean enabled = context.getSharedPreferences("loxia_prefs", Context.MODE_PRIVATE)
                    .getBoolean("notifications_enabled", true);
            if (!enabled) return;

            // goAsync() 延长 BroadcastReceiver 的生命周期，防止 ANR
            PendingResult pendingResult = goAsync();

            executor.execute(() -> {
                try {
                    DataRepository repository = DataRepository.getInstance(context);
                    // NotificationChannel 已在 LoXiaApp.onCreate() 中创建
                    AlarmScheduler.scheduleAllPendingReminders(context, repository);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
