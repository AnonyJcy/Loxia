package com.cy.loxia

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 提供应用级 CoroutineScope（Java 调用入口）
 */
object AppScopeProvider {
    @JvmStatic
    fun createApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
