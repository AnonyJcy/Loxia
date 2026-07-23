package com.cy.loxia

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cy.loxia.data.db.LoXiaDatabase
import com.cy.loxia.data.db.WardrobeEntity
import com.cy.loxia.data.db.DressItemEntity
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 数据仓库：统一走 Room Flow 路径，无内存缓存
 * 所有写入方法均为 suspend 函数，确保数据一致性
 */
class DataRepository private constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DataRepository"
        private const val PREFS_NAME = "loxia_prefs"
        private const val KEY_MIGRATED = "is_data_migrated_to_room"
        private const val KEY_DEFAULTS_INSERTED = "defaults_inserted_to_room"
        private const val KEY_WARDROBES = "key_wardrobes"
        private const val KEY_DRESS_ITEMS = "key_dress_items"

        // 默认衣柜 ID 常量（首次创建后持久化，不再每次生成）
        private const val KEY_DEFAULT_WARDROBE_1_ID = "default_wardrobe_1_id"
        private const val KEY_DEFAULT_WARDROBE_2_ID = "default_wardrobe_2_id"
        @Volatile
        private var INSTANCE: DataRepository? = null

        @JvmStatic
        fun getInstance(context: Context): DataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataRepository(
                    context.applicationContext,
                    (context.applicationContext as LoXiaApp).applicationScope
                ).also {
                    INSTANCE = it
                    it.initializeAsync()
                }
            }
        }
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Room 数据库和 DAO（Single Source of Truth）
    private val database: LoXiaDatabase = LoXiaDatabase.getInstance(context)
    private val dao = database.loXiaDao()

    // 错误通知（UI 层可观察此 LiveData 显示友好提示）
    private val _errorEvent = MutableLiveData<String?>()
    val errorEvent: LiveData<String?> = _errorEvent

    /**
     * 清除错误事件（UI 层处理后调用）
     */
    fun clearErrorEvent() {
        _errorEvent.value = null
    }

    // Readiness gate: migration + default data must complete before any reads
    private val readyDeferred = CompletableDeferred<Unit>()

    /**
     * Initialize: migrate legacy data + ensure defaults.
     * Safe to call multiple times; only the first call takes effect.
     */
    fun initializeAsync() {
        if (readyDeferred.isCompleted) return
        scope.launch {
            try {
                migrateDataToRoomIfNeeded()
                ensureDefaultData()
                fixWardrobeUpdatedAt()  // 修复旧数据的 updatedAt
                fixWardrobeCounts()     // 修复可能因移动裙子导致的计数错误
                readyDeferred.complete(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                _errorEvent.postValue("数据初始化失败，请重启应用重试")
                readyDeferred.complete(Unit)
            }
        }
    }

    /**
     * 修复衣柜的计数值（如果因为移动裙子等操作导致不同步）
     */
    private suspend fun fixWardrobeCounts() {
        try {
            val wardrobes = dao.getAllWardrobes()
            for (wardrobe in wardrobes) {
                val realCount = dao.getDressItemCountByWardrobeId(wardrobe.id)
                if (wardrobe.count != realCount) {
                    dao.updateWardrobe(wardrobe.copy(count = realCount))
                    Log.d(TAG, "Fixed count for wardrobe: ${wardrobe.name} to $realCount")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fix wardrobe counts", e)
        }
    }

    /**
     * Suspend until initialization is complete.
     */
    suspend fun awaitReady() {
        readyDeferred.await()
    }

    /**
     * 修复旧数据迁移后 updatedAt 为 0 的衣柜
     * 从关联裙子的 addTime 推断最近更新时间
     */
    private suspend fun fixWardrobeUpdatedAt() {
        try {
            val wardrobes = dao.getAllWardrobes()
            for (wardrobe in wardrobes) {
                if (wardrobe.updatedAt == 0L) {
                    val latestAddTime = dao.getLatestAddTimeByWardrobeId(wardrobe.id)
                    val updatedAt = latestAddTime ?: System.currentTimeMillis()
                    // 使用 update 而不是 insert，避免 REPLACE 策略触发级联删除
                    dao.updateWardrobe(wardrobe.copy(updatedAt = updatedAt))
                    Log.d(TAG, "Fixed updatedAt for wardrobe: ${wardrobe.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fix wardrobe updatedAt", e)
        }
    }

    // ==================== Wardrobe Operations ====================

    /**
     * 异步获取所有衣柜（suspend，由 ViewModel 在协程中调用）
     */
    suspend fun getWardrobesAsync(): List<Wardrobe> {
        awaitReady()
        return try {
            dao.getAllWardrobes().map { it.toDomain() }.sortedBy { it.sortOrder }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load wardrobes", e)
            _errorEvent.postValue("加载柜子数据失败")
            emptyList()
        }
    }

    /**
     * 添加衣柜（suspend）
     */
    suspend fun addWardrobe(wardrobe: Wardrobe) {
        awaitReady()
        try {
            val existing = dao.getAllWardrobes()
            val newWardrobe = wardrobe.copy(sortOrder = existing.size)
            dao.insertWardrobe(newWardrobe.toEntity())
            Log.d(TAG, "Added wardrobe: ${wardrobe.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add wardrobe", e)
            _errorEvent.postValue("添加柜子失败")
        }
    }

    /**
     * 删除衣柜（suspend）
     */
    suspend fun deleteWardrobe(wardrobeId: String) {
        awaitReady()
        try {
            dao.deleteWardrobeById(wardrobeId)
            // Room CASCADE 外键会自动删除关联裙子
            Log.d(TAG, "Deleted wardrobe: $wardrobeId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete wardrobe", e)
            _errorEvent.postValue("删除柜子失败")
        }
    }

    /**
     * 通过 ID 查找衣柜（suspend）
     */
    suspend fun findWardrobeById(id: String): Wardrobe? {
        awaitReady()
        return try {
            dao.findWardrobeById(id)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find wardrobe", e)
            null
        }
    }

    /**
     * 上移衣柜（suspend）
     */
    suspend fun moveWardrobeUp(wardrobeId: String) {
        awaitReady()
        try {
            val wardrobes = dao.getAllWardrobes().sortedBy { it.sortOrder }.toMutableList()
            val index = wardrobes.indexOfFirst { it.id == wardrobeId }
            if (index > 0) {
                val current = wardrobes[index]
                val above = wardrobes[index - 1]
                dao.updateWardrobe(current.copy(sortOrder = index - 1))
                dao.updateWardrobe(above.copy(sortOrder = index))
                Log.d(TAG, "Moved wardrobe up: $wardrobeId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move wardrobe up", e)
            _errorEvent.postValue("移动柜子失败")
        }
    }

    /**
     * 下移衣柜（suspend）
     */
    suspend fun moveWardrobeDown(wardrobeId: String) {
        awaitReady()
        try {
            val wardrobes = dao.getAllWardrobes().sortedBy { it.sortOrder }.toMutableList()
            val index = wardrobes.indexOfFirst { it.id == wardrobeId }
            if (index < wardrobes.size - 1) {
                val current = wardrobes[index]
                val below = wardrobes[index + 1]
                dao.updateWardrobe(current.copy(sortOrder = index + 1))
                dao.updateWardrobe(below.copy(sortOrder = index))
                Log.d(TAG, "Moved wardrobe down: $wardrobeId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move wardrobe down", e)
            _errorEvent.postValue("移动柜子失败")
        }
    }

    // ==================== DressItem Operations ====================

    /**
     * 异步获取所有裙子（suspend）
     */
    suspend fun getDressItemsAsync(): List<DressItem> {
        awaitReady()
        return try {
            dao.getAllDressItems().map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dress items", e)
            _errorEvent.postValue("加载裙子数据失败")
            emptyList()
        }
    }

    /**
     * 添加裙子（suspend）
     */
    suspend fun addDressItem(item: DressItem) {
        awaitReady()
        try {
            dao.insertDressItem(item.toEntity())
            updateWardrobeCount(item.wardrobeId)
            Log.d(TAG, "Added dress item: ${item.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add dress item", e)
            _errorEvent.postValue("添加裙子失败")
        }
    }

    /**
     * 更新裙子（suspend）
     */
    suspend fun updateDressItem(updated: DressItem) {
        awaitReady()
        try {
            val oldItem = dao.findDressItemById(updated.id)
            dao.updateDressItem(updated.toEntity())
            if (oldItem != null && oldItem.wardrobeId != updated.wardrobeId) {
                updateWardrobeCount(oldItem.wardrobeId)
            }
            updateWardrobeCount(updated.wardrobeId)
            Log.d(TAG, "Updated dress item: ${updated.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update dress item", e)
            _errorEvent.postValue("更新裙子失败")
        }
    }

    /**
     * 删除裙子（suspend）
     */
    suspend fun deleteDressItem(itemId: String) {
        awaitReady()
        try {
            val item = dao.findDressItemById(itemId)
            if (item != null) {
                dao.deleteDressItemById(itemId)
                updateWardrobeCount(item.wardrobeId)
                Log.d(TAG, "Deleted dress item: $itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete dress item", e)
            _errorEvent.postValue("删除裙子失败")
        }
    }

    /**
     * 获取特定衣柜的裙子（suspend）
     */
    suspend fun getDressItemEntitiesByWardrobe(wardrobeId: String): List<DressItemEntity> {
        awaitReady()
        return try {
            dao.getDressItemsByWardrobeId(wardrobeId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dress items by wardrobe", e)
            emptyList()
        }
    }

    /**
     * 获取排序后的裙子列表（suspend）
     */
    suspend fun getSortedItemsForWardrobe(wardrobeId: String): List<DressItem> {
        awaitReady()
        return try {
            dao.getSortedDressItemsByWardrobeId(wardrobeId).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sorted items", e)
            emptyList()
        }
    }

    /**
     * 通过 ID 查找裙子（suspend）
     */
    suspend fun findDressItemById(id: String): DressItem? {
        awaitReady()
        return try {
            dao.findDressItemById(id)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find dress item by id", e)
            null
        }
    }

    /**
     * 置顶/取消置顶裙子（suspend）
     */
    suspend fun pinDressItem(itemId: String, pinned: Boolean) {
        awaitReady()
        try {
            val item = dao.findDressItemById(itemId)
            if (item != null) {
                dao.updateDressItem(item.copy(pinned = pinned))
                Log.d(TAG, "Pinned dress item: $itemId = $pinned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pin dress item", e)
            _errorEvent.postValue("置顶操作失败")
        }
    }

    /**
     * 上移裙子（suspend）
     */
    suspend fun moveDressItemUp(itemId: String, wardrobeId: String) {
        awaitReady()
        try {
            val sorted = dao.getSortedDressItemsByWardrobeId(wardrobeId)
            val index = sorted.indexOfFirst { it.id == itemId }
            if (index > 0) {
                val current = sorted[index]
                val above = sorted[index - 1]
                dao.updateDressItem(current.copy(sortOrder = above.sortOrder))
                dao.updateDressItem(above.copy(sortOrder = current.sortOrder))
                Log.d(TAG, "Moved dress item up: $itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move dress item up", e)
            _errorEvent.postValue("移动裙子失败")
        }
    }

    /**
     * 下移裙子（suspend）
     */
    suspend fun moveDressItemDown(itemId: String, wardrobeId: String) {
        awaitReady()
        try {
            val sorted = dao.getSortedDressItemsByWardrobeId(wardrobeId)
            val index = sorted.indexOfFirst { it.id == itemId }
            if (index < sorted.size - 1) {
                val current = sorted[index]
                val below = sorted[index + 1]
                dao.updateDressItem(current.copy(sortOrder = below.sortOrder))
                dao.updateDressItem(below.copy(sortOrder = current.sortOrder))
                Log.d(TAG, "Moved dress item down: $itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move dress item down", e)
            _errorEvent.postValue("移动裙子失败")
        }
    }

    /**
     * 移到顶部（suspend）
     */
    suspend fun moveDressItemToTop(itemId: String, wardrobeId: String) {
        awaitReady()
        try {
            val sorted = dao.getSortedDressItemsByWardrobeId(wardrobeId)
            val target = sorted.find { it.id == itemId }
            if (target != null) {
                dao.updateDressItem(target.copy(sortOrder = -1))
                Log.d(TAG, "Moved dress item to top: $itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move dress item to top", e)
            _errorEvent.postValue("移动裙子失败")
        }
    }

    /**
     * 移到底部（suspend）
     */
    suspend fun moveDressItemToBottom(itemId: String, wardrobeId: String) {
        awaitReady()
        try {
            val sorted = dao.getSortedDressItemsByWardrobeId(wardrobeId)
            val target = sorted.find { it.id == itemId }
            if (target != null) {
                dao.updateDressItem(target.copy(sortOrder = sorted.size))
                Log.d(TAG, "Moved dress item to bottom: $itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move dress item to bottom", e)
            _errorEvent.postValue("移动裙子失败")
        }
    }

    /**
     * 移除所有裙子的某个状态（suspend）
     */
    suspend fun removeStatusFromAllItems(status: String) {
        awaitReady()
        try {
            val items = dao.getAllDressItems()
            items.forEach { item ->
                val parts = item.status.split("、").toMutableList()
                if (parts.remove(status)) {
                    dao.updateDressItem(item.copy(status = parts.joinToString("、")))
                }
            }
            Log.d(TAG, "Removed status from all items: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove status", e)
            _errorEvent.postValue("更新状态失败")
        }
    }

    /**
     * 批量更新裙子（suspend）
     */
    suspend fun updateAllDressItems(items: List<DressItem>) {
        awaitReady()
        try {
            dao.insertDressItems(items.map { it.toEntity() })
            val wardrobeIds = items.map { it.wardrobeId }.toSet()
            for (wId in wardrobeIds) {
                updateWardrobeCount(wId)
            }
            Log.d(TAG, "Updated all dress items: ${items.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update all dress items", e)
            _errorEvent.postValue("更新裙子数据失败")
        }
    }

    /**
     * 批量更新衣柜（suspend）
     */
    suspend fun updateAllWardrobes(wardrobes: List<Wardrobe>) {
        awaitReady()
        try {
            dao.insertWardrobes(wardrobes.map { it.toEntity() })
            Log.d(TAG, "Updated all wardrobes: ${wardrobes.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update all wardrobes", e)
            _errorEvent.postValue("更新柜子数据失败")
        }
    }

    /**
     * 原子性导入：在 Room 事务中同时更新衣柜和裙子数据
     */
    suspend fun importInTransaction(wardrobes: List<Wardrobe>, dressItems: List<DressItem>) {
        awaitReady()
        database.withTransaction {
            dao.insertWardrobes(wardrobes.map { it.toEntity() })
            dao.insertDressItems(dressItems.map { it.toEntity() })
        }
        Log.d(TAG, "Import in transaction completed: ${wardrobes.size} wardrobes, ${dressItems.size} items")
    }

    // ==================== Flow 路径（ViewModel 使用） ====================

    /**
     * 响应式获取衣柜列表 Flow
     */
    fun getAllWardrobesFlow(): Flow<List<WardrobeEntity>> {
        return dao.getAllWardrobesFlow()
    }

    /**
     * 响应式获取裙子列表 Flow
     */
    fun getAllDressItemsFlow(): Flow<List<DressItemEntity>> {
        return dao.getAllDressItemsFlow()
    }

    // ==================== 主题和分类设置 ====================

    fun setThemeMode(mode: String) {
        preferences.edit().putString("theme_mode", mode).apply()
    }

    fun getThemeMode(): String {
        return preferences.getString("theme_mode", "system") ?: "system"
    }

    fun getHiddenCategories(): List<String> {
        val hidden = mutableListOf<String>()
        try {
            val jsonText = preferences.getString("hidden_categories", "")
            if (!jsonText.isNullOrEmpty()) {
                val array = JSONArray(jsonText)
                for (i in 0 until array.length()) {
                    hidden.add(array.getString(i))
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse hidden categories", e)
        }
        return hidden
    }

    fun addHiddenCategory(category: String) {
        val hidden = getHiddenCategories().toMutableList()
        if (!hidden.contains(category)) {
            hidden.add(category)
            val array = JSONArray()
            hidden.forEach { array.put(it) }
            preferences.edit().putString("hidden_categories", array.toString()).apply()
            Log.d(TAG, "Added hidden category: $category")
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 更新衣柜的裙子计数（suspend）
     */
    private suspend fun updateWardrobeCount(wardrobeId: String) {
        try {
            val count = dao.getDressItemCountByWardrobeId(wardrobeId)
            val wardrobe = dao.findWardrobeById(wardrobeId)
            if (wardrobe != null) {
                // 如果 updatedAt 为 0（旧数据迁移），从裙子的 addTime 推断
                val updatedAt = if (wardrobe.updatedAt == 0L) {
                    dao.getLatestAddTimeByWardrobeId(wardrobeId) ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
                // 使用 update 而不是 insert，避免 REPLACE 策略触发级联删除
                dao.updateWardrobe(wardrobe.copy(
                    count = count,
                    updatedAt = updatedAt
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update wardrobe count", e)
        }
    }

    /**
     * Entity 转 Domain Model 扩展函数
     */
    private fun WardrobeEntity.toDomain(): Wardrobe = Wardrobe(
        id = id,
        name = name,
        count = count,
        updatedAt = updatedAt,
        isDemo = isDemo,
        cover = cover,
        sortOrder = sortOrder
    )

    /**
     * Domain Model 转 Entity 扩展函数
     */
    private fun Wardrobe.toEntity(): WardrobeEntity = WardrobeEntity(
        id = id,
        name = name,
        count = count,
        updatedAt = updatedAt,
        isDemo = isDemo,
        cover = cover,
        sortOrder = sortOrder
    )

    /**
     * Entity 转 Domain Model 扩展函数
     */
    private fun DressItemEntity.toDomain(): DressItem = DressItem(
        id = id,
        wardrobeId = wardrobeId,
        name = name,
        store = store,
        channel = channel,
        price = price,
        buyDate = buyDate,
        status = status,
        remindAt = remindAt,
        remark = remark,
        earnestMoney = earnestMoney,
        isFullPayment = isFullPayment,
        fullPaymentAmount = fullPaymentAmount,
        tailPayment = tailPayment,
        imageUri = imageUri,
        shippingFee = shippingFee,
        deposit = deposit,
        pinned = pinned,
        sortOrder = sortOrder,
        daiqiangDate = daiqiangDate,
        yixiangDate = yixiangDate,
        dingjinDate = dingjinDate,
        buweikuanDate = buweikuanDate,
        isWishlist = isWishlist,
        addTime = addTime,
        shipmentDate = shipmentDate,
        receivedDate = receivedDate,
        expectedShipmentDate = expectedShipmentDate
    )

    /**
     * Domain Model 转 Entity 扩展函数
     */
    private fun DressItem.toEntity(): DressItemEntity = DressItemEntity(
        id = id,
        wardrobeId = wardrobeId,
        name = name,
        store = store,
        channel = channel,
        price = price,
        buyDate = buyDate,
        status = status,
        remindAt = remindAt,
        remark = remark,
        earnestMoney = earnestMoney,
        isFullPayment = isFullPayment,
        fullPaymentAmount = fullPaymentAmount,
        tailPayment = tailPayment,
        imageUri = imageUri,
        shippingFee = shippingFee,
        deposit = deposit,
        pinned = pinned,
        sortOrder = sortOrder,
        daiqiangDate = daiqiangDate,
        yixiangDate = yixiangDate,
        dingjinDate = dingjinDate,
        buweikuanDate = buweikuanDate,
        isWishlist = isWishlist,
        addTime = if (addTime > 0L) addTime else System.currentTimeMillis(),
        shipmentDate = shipmentDate,
        receivedDate = receivedDate,
        expectedShipmentDate = expectedShipmentDate
    )

    /**
     * 获取默认衣柜列表
     * ID 首次生成后持久化到 SharedPreferences，确保跨启动稳定
     */
    private fun defaultWardrobes(): List<Wardrobe> {
        val id1 = getOrCreateDefaultId(KEY_DEFAULT_WARDROBE_1_ID)
        val id2 = getOrCreateDefaultId(KEY_DEFAULT_WARDROBE_2_ID)
        return listOf(
            Wardrobe(
                id = id1,
                name = "甜系主柜",
                count = 1,
                updatedAt = System.currentTimeMillis() - 86400000L,
                isDemo = true,
                sortOrder = 0
            ),
            Wardrobe(
                id = id2,
                name = "古典收藏柜",
                count = 1,
                updatedAt = System.currentTimeMillis() - 172800000L,
                isDemo = true,
                sortOrder = 1
            )
        )
    }

    /**
     * 获取或创建默认 ID：首次生成后持久化，后续直接读取
     */
    private fun getOrCreateDefaultId(key: String): String {
        var id = preferences.getString(key, null)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            preferences.edit().putString(key, id).apply()
        }
        return id
    }

    /**
     * 确保首次启动时有默认数据
     */
    private suspend fun ensureDefaultData() {
        if (preferences.getBoolean(KEY_DEFAULTS_INSERTED, false)) {
            return
        }
        try {
            val wardrobeCount = dao.getWardrobeCount()
            if (wardrobeCount == 0) {
                val wardrobes = defaultWardrobes()
                dao.insertWardrobes(wardrobes.map { it.toEntity() })
                Log.d(TAG, "Inserted default wardrobes: ${wardrobes.size}")

                val dressItems = defaultDressItems()
                dao.insertDressItems(dressItems.map { it.toEntity() })
                Log.d(TAG, "Inserted default dress items: ${dressItems.size}")
            }
            preferences.edit().putBoolean(KEY_DEFAULTS_INSERTED, true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure default data", e)
        }
    }

    private fun defaultDressItems(): List<DressItem> {
        val wardrobes = defaultWardrobes()
        val items = mutableListOf<DressItem>()
        if (wardrobes.isNotEmpty()) {
            val first = wardrobes[0]
            items.add(
                DressItem(
                    id = UUID.randomUUID().toString(),
                    wardrobeId = first.id,
                    name = "蝶语茶会 JSK",
                    store = "梦回茶会",
                    channel = "品牌直购",
                    price = 1314.0,
                    buyDate = "2026-04-20",
                    status = "已到手",
                    remindAt = "2026-05-05",
                    remark = "春日复古茶会风"
                ).apply {
                    isFullPayment = true
                    fullPaymentAmount = 1314.0
                }
            )
        }
        if (wardrobes.size > 1) {
            val second = wardrobes[1]
            items.add(
                DressItem(
                    id = UUID.randomUUID().toString(),
                    wardrobeId = second.id,
                    name = "草莓奶油 OP",
                    store = "奶油工坊",
                    channel = "淘宝",
                    price = 520.0,
                    buyDate = "2026-04-28",
                    status = "已下单",
                    remindAt = "2026-05-10",
                    remark = "期待甜美穿搭"
                ).apply {
                    isFullPayment = true
                    fullPaymentAmount = 520.0
                }
            )
        }
        return items
    }

    // ==================== Data Migration ====================

    /**
     * 从 SharedPreferences 迁移到 Room（原子性保证）
     * 只有在所有数据成功插入 Room 后才标记迁移完成
     * 注意：此方法在 IO 协程上下文中同步调用（IO 协程上下文中同步调用）
     */
    private suspend fun migrateDataToRoomIfNeeded() {
        // 检查是否已经迁移过
        if (preferences.getBoolean(KEY_MIGRATED, false)) {
            Log.d(TAG, "Data already migrated to Room")
            return
        }

        // 检查是否有旧数据需要迁移
        val hasOldWardrobes = preferences.contains(KEY_WARDROBES)
        val hasOldDresses = preferences.contains(KEY_DRESS_ITEMS)

        if (!hasOldWardrobes && !hasOldDresses) {
            // 没有旧数据，直接标记迁移完成
            preferences.edit().putBoolean(KEY_MIGRATED, true).apply()
            Log.d(TAG, "No legacy data found, migration marked complete")
            return
        }

        // 同步执行迁移（已在 IO 协程上下文中）
        try {
            Log.d(TAG, "Starting data migration to Room...")

                val wardrobesJson = preferences.getString(KEY_WARDROBES, "")
                val dressesJson = preferences.getString(KEY_DRESS_ITEMS, "")

                // 迁移衣柜数据
                if (!wardrobesJson.isNullOrEmpty()) {
                    val wardrobeArray = JSONArray(wardrobesJson)
                    val wardrobeEntities = mutableListOf<WardrobeEntity>()
                    for (i in 0 until wardrobeArray.length()) {
                        val json = wardrobeArray.getJSONObject(i)
                        // 兼容旧数据：updatedAt 可能是 String 或 Long
                        val updatedAtRaw = json.opt("updatedAt")
                        val updatedAtLong = when (updatedAtRaw) {
                            is Number -> updatedAtRaw.toLong()
                            is String -> {
                                if (updatedAtRaw.isNotEmpty()) {
                                    try { java.time.LocalDate.parse(updatedAtRaw).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (e: Exception) { 0L }
                                } else 0L
                            }
                            else -> 0L
                        }
                        wardrobeEntities.add(
                            WardrobeEntity(
                                id = json.optString("id", ""),
                                name = json.optString("name", ""),
                                count = json.optInt("count", 0),
                                updatedAt = updatedAtLong,
                                isDemo = json.optBoolean("isDemo", false),
                                cover = json.optString("cover", ""),
                                sortOrder = json.optInt("sortOrder", 0)
                            )
                        )
                    }
                    dao.insertWardrobes(wardrobeEntities)
                    Log.d(TAG, "Migrated ${wardrobeEntities.size} wardrobes to Room")
                }

                // 迁移裙子数据
                if (!dressesJson.isNullOrEmpty()) {
                    val dressArray = JSONArray(dressesJson)
                    val dressEntities = mutableListOf<DressItemEntity>()
                    for (i in 0 until dressArray.length()) {
                        val json = dressArray.getJSONObject(i)
                        dressEntities.add(
                            DressItemEntity(
                                id = json.optString("id", ""),
                                wardrobeId = json.optString("wardrobeId", ""),
                                name = json.optString("name", ""),
                                store = json.optString("store", ""),
                                channel = json.optString("channel", ""),
                                price = json.optDouble("price", 0.0),
                                buyDate = json.optString("buyDate", ""),
                                status = json.optString("status", ""),
                                remindAt = json.optString("remindAt", ""),
                                remark = json.optString("remark", ""),
                                earnestMoney = json.optDouble("earnestMoney", 0.0),
                                isFullPayment = json.optBoolean("isFullPayment", false),
                                fullPaymentAmount = json.optDouble("fullPaymentAmount", 0.0),
                                tailPayment = json.optDouble("tailPayment", 0.0),
                                imageUri = json.optString("imageUri", ""),
                                shippingFee = json.optString("shippingFee", "包邮"),
                                deposit = json.optDouble("deposit", 0.0),
                                pinned = json.optBoolean("pinned", false),
                                sortOrder = json.optInt("sortOrder", 0),
                                daiqiangDate = json.optString("daiqiangDate", ""),
                                yixiangDate = json.optString("yixiangDate", ""),
                                dingjinDate = json.optString("dingjinDate", ""),
                                buweikuanDate = json.optString("buweikuanDate", ""),
                                isWishlist = false,
                                addTime = System.currentTimeMillis()
                            )
                        )
                    }
                    dao.insertDressItems(dressEntities)
                    Log.d(TAG, "Migrated ${dressEntities.size} dress items to Room")
                }

                // 只有在所有插入成功后才标记迁移完成（原子性保证）
                preferences.edit().putBoolean(KEY_MIGRATED, true).apply()
                Log.d(TAG, "Data migration to Room completed successfully")

        } catch (e: Exception) {
            // 迁移失败，不标记迁移完成，下次启动会重试
            Log.e(TAG, "Failed to migrate data to Room, will retry next launch", e)
        }
    }
}
