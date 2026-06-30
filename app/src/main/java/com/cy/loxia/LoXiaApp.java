package com.cy.loxia;

import android.app.Application;

import kotlinx.coroutines.CoroutineScope;

/**
 * Application 类：提供应用级 CoroutineScope 和 NotificationChannel 初始化
 */
public class LoXiaApp extends Application {

    private CoroutineScope applicationScope;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationScope = AppScopeProvider.createApplicationScope();
        // 应用启动时创建 NotificationChannel（只需创建一次）
        AlarmScheduler.createNotificationChannel(this);
    }

    /**
     * 获取应用级 CoroutineScope，生命周期等同于进程
     * DataRepository 等单例应使用此 scope 而非自建 scope
     */
    public CoroutineScope getApplicationScope() {
        return applicationScope;
    }
}
