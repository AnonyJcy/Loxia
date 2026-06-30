package com.cy.loxia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cy.loxia.data.db.DressItemEntity
import com.cy.loxia.data.db.WardrobeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 通过 DataRepository 统一访问数据（Single Source of Truth）
    private val repository = DataRepository.getInstance(application)

    // 暴露 Repository 的错误事件给 UI 层
    val errorEvent: LiveData<String?> = repository.errorEvent

    /**
     * 清除错误事件（UI 层处理后调用）
     */
    fun clearErrorEvent() {
        repository.clearErrorEvent()
    }

    init {
        // 串行化初始化：先等待 ready，再启动 Flow 收集
        viewModelScope.launch {
            repository.awaitReady()
            // ready 后再启动 Flow 收集
            launch {
                repository.getAllWardrobesFlow().collect { entities ->
                    _wardrobeList.value = entities.map { it.toDomain() }
                    // Flow 数据到达后初始化选中的衣柜
                    if (_selectedWardrobeId.value == null) {
                        _selectedWardrobeId.value = entities.firstOrNull()?.id
                    }
                }
            }
            launch {
                repository.getAllDressItemsFlow().collect { entities ->
                    val items = entities.map { it.toDomain() }
                    _allDressItems.value = items
                    refreshStatisticsFromItems(items)
                }
            }
        }
    }

    // ==================== 衣柜状态（主页） ====================

    /** 衣柜列表：Room Flow 驱动（SSOT），暴露 Domain Model */
    private val _wardrobeList = MutableLiveData<List<Wardrobe>>()
    val wardrobeList: LiveData<List<Wardrobe>> = _wardrobeList

    private val _selectedWardrobeId = MutableLiveData<String?>()
    val selectedWardrobeId: LiveData<String?> = _selectedWardrobeId

    // 催款浮条状态：是否已被用户操作过（关闭或自动隐藏）
    private var hasBannerBeenActioned = false

    // ==================== 状态聚合（催款看板） ====================

    companion object {
        /** 状态常量：已付定金 */
        const val STATUS_DEPOSIT_PAID = "付定金"
        /** 状态常量：待补尾款 */
        const val STATUS_FINAL_PAYMENT = "补尾款"
        /** 状态常量：待发货 */
        const val STATUS_SHIPPED = "待发货"
    }

    // 所有裙子数据源（必须在 MediatorLiveData 之前声明），暴露 Domain Model
    private val _allDressItems = MutableLiveData<List<DressItem>>()

    /**
     * 紧急催款列表：筛选出所有状态包含"补尾款"的裙子
     */
    val urgentFinalPaymentList: LiveData<List<DressItem>> = MediatorLiveData<List<DressItem>>().apply {
        fun update() {
            val items = _allDressItems.value ?: emptyList()
            val filtered = items.filter { it.status.contains(STATUS_FINAL_PAYMENT) }
            (this as MediatorLiveData).value = filtered
        }
        addSource(_allDressItems) { update() }
    }

    /**
     * 在途进度管线计数
     */
    data class PipelineCounts(
        val depositPaid: Int = 0,
        val finalPayment: Int = 0,
        val shipped: Int = 0
    )

    val pipelineCounts: LiveData<PipelineCounts> = MediatorLiveData<PipelineCounts>().apply {
        fun update() {
            val items = _allDressItems.value ?: emptyList()
            val counts = PipelineCounts(
                depositPaid = items.count { it.status.contains(STATUS_DEPOSIT_PAID) },
                finalPayment = items.count { it.status.contains(STATUS_FINAL_PAYMENT) },
                shipped = items.count { it.status.contains(STATUS_SHIPPED) }
            )
            (this as MediatorLiveData).value = counts
        }
        addSource(_allDressItems) { update() }
    }

    /**
     * 首页催款提示浮条显示信号
     */
    val showHomeBanner: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun update() {
            val items = _allDressItems.value ?: emptyList()
            (this as MediatorLiveData).value = items.any { it.status.contains(STATUS_FINAL_PAYMENT) }
        }
        addSource(_allDressItems) { update() }
    }

    // ==================== 裙子状态（总览页） ====================

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedStatuses = MutableLiveData<List<String>>(emptyList())
    val selectedStatuses: LiveData<List<String>> = _selectedStatuses

    /**
     * 经过搜索词和状态筛选过滤后的裙子列表（暴露 Domain Model）
     */
    val filteredDressItems: LiveData<List<DressItem>> = MediatorLiveData<List<DressItem>>().apply {
        fun update() {
            val items = _allDressItems.value ?: emptyList()
            val query = _searchQuery.value?.trim()?.lowercase() ?: ""
            val statuses = _selectedStatuses.value ?: emptyList()

            val filtered = items.filter { item ->
                val matchesQuery = query.isEmpty() ||
                    item.name.lowercase().contains(query) ||
                    item.store.lowercase().contains(query)
                if (!matchesQuery) return@filter false
                if (statuses.isEmpty()) return@filter true
                val itemStatuses = item.status.split("、")
                statuses.any { it in itemStatuses }
            }
            (this as MediatorLiveData).value = filtered
        }
        addSource(_allDressItems) { update() }
        addSource(_searchQuery) { update() }
        addSource(_selectedStatuses) { update() }
    }

    // ==================== 衣柜详情裙子流 ====================

    private val _wardrobeDressItems = MutableLiveData<List<DressItem>>()
    val wardrobeDressItems: LiveData<List<DressItem>> = _wardrobeDressItems

    /**
     * 获取特定衣柜内的裙子列表（暴露 Domain Model）
     */
    fun fetchDressesByWardrobe(wardrobeId: String) {
        viewModelScope.launch {
            val entities = repository.getDressItemEntitiesByWardrobe(wardrobeId)
            _wardrobeDressItems.value = entities.map { it.toDomain() }
        }
    }

    // ==================== 智能支付表单状态 ====================

    private val _isFullPayment = MutableLiveData<Boolean>(false)
    val isFullPayment: LiveData<Boolean> = _isFullPayment

    fun setIsFullPayment(isFull: Boolean) {
        _isFullPayment.value = isFull
    }

    /**
     * 计算裙子的有效总价
     */
    fun calculateEffectiveTotal(
        price: Double,
        isFullPayment: Boolean,
        fullPaymentAmount: Double,
        earnestMoney: Double,
        tailPayment: Double,
        shippingFee: String,
        deposit: Double
    ): Double {
        var total = 0.0
        total += earnestMoney
        if (isFullPayment) {
            total += fullPaymentAmount
        } else {
            total += deposit
            total += tailPayment
        }
        total += parseShippingFee(shippingFee)
        if (total < 0.001) {
            total = price
        }
        return total
    }

    private fun parseShippingFee(shippingFee: String): Double {
        if (shippingFee.isEmpty() || shippingFee == "包邮") return 0.0
        return try {
            shippingFee.replace("¥", "").replace("$", "").replace("元", "").trim().toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    // ==================== 保存操作 ====================

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    /**
     * 保存裙子数据（suspend，接受 Domain Model）
     */
    suspend fun saveDressItem(dress: DressItem) {
        try {
            withContext(Dispatchers.IO) {
                repository.updateDressItem(dress)
            }
            _saveResult.value = SaveResult(true, "保存成功")
        } catch (e: Exception) {
            _saveResult.value = SaveResult(false, "保存失败: ${e.message}")
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    data class SaveResult(val success: Boolean, val message: String)

    // ==================== 统计数据流 ====================

    private val _totalDressCount = MutableLiveData<Int>(0)
    val totalDressCount: LiveData<Int> = _totalDressCount

    private val _totalCost = MutableLiveData<Double>(0.0)
    val totalCost: LiveData<Double> = _totalCost

    private val _thisMonthItemCount = MutableLiveData<Int>(0)
    val thisMonthItemCount: LiveData<Int> = _thisMonthItemCount

    private val _isTotalCostHidden = MutableLiveData<Boolean>(true)
    val isTotalCostHidden: LiveData<Boolean> = _isTotalCostHidden

    fun setTotalCostHidden(hidden: Boolean) {
        _isTotalCostHidden.value = hidden
    }

    /**
     * 从裙子列表派生统计数据（由 Flow 自动调用，接受 Domain Model）
     */
    private fun refreshStatisticsFromItems(items: List<DressItem>) {
        _totalDressCount.value = items.size
        val cost = items.sumOf { it.getEffectiveTotal() }
        _totalCost.value = cost

        val now = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        _thisMonthItemCount.value = items.count { item ->
            try {
                val date = LocalDate.parse(item.buyDate, formatter)
                date.year == now.year && date.month == now.month
            } catch (e: Exception) {
                false
            }
        }
    }

    fun fetchStatistics() {
        refreshStatisticsFromItems(_allDressItems.value ?: emptyList())
    }

    // ==================== AI 数据导入 ====================

    private val _importResult = MutableLiveData<ImportResult?>()
    val importResult: LiveData<ImportResult?> = _importResult

    /**
     * 导入 AI 解析的裙子数据到指定衣柜（接受 Domain Model）
     */
    fun importParsedData(wardrobeId: String, items: List<DressItem>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val updatedItems = items.map { it.copy(wardrobeId = wardrobeId, addTime = System.currentTimeMillis()) }
                    repository.updateAllDressItems(updatedItems)
                }
                _importResult.value = ImportResult(true, "成功导入 ${items.size} 条裙子")
            } catch (e: Exception) {
                _importResult.value = ImportResult(false, "导入失败: ${e.message}")
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    data class ImportResult(val success: Boolean, val message: String)

    // ==================== 衣柜操作 ====================

    /**
     * 添加衣柜（suspend）
     */
    suspend fun addWardrobe(wardrobe: Wardrobe) {
        withContext(Dispatchers.IO) {
            repository.addWardrobe(wardrobe)
        }
    }

    /**
     * 删除衣柜（suspend）
     */
    suspend fun deleteWardrobe(wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.deleteWardrobe(wardrobeId)
        }
    }

    /**
     * 上移衣柜（suspend）
     */
    suspend fun moveWardrobeUp(wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveWardrobeUp(wardrobeId)
        }
    }

    /**
     * 下移衣柜（suspend）
     */
    suspend fun moveWardrobeDown(wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveWardrobeDown(wardrobeId)
        }
    }

    /**
     * 初始化选中的衣柜（首次加载时调用）
     */
    fun initSelectedWardrobe() {
        if (_selectedWardrobeId.value == null) {
            _selectedWardrobeId.value = _wardrobeList.value?.firstOrNull()?.id
        }
    }

    // ==================== 催款浮条状态 ====================

    fun hasBannerBeenActioned(): Boolean = hasBannerBeenActioned

    fun markBannerAsActioned() {
        hasBannerBeenActioned = true
    }

    fun selectWardrobe(wardrobeId: String) {
        _selectedWardrobeId.value = wardrobeId
    }

    // ==================== 裙子操作 ====================

    /**
     * 添加裙子（suspend）
     */
    suspend fun addDressItem(item: DressItem) {
        withContext(Dispatchers.IO) {
            repository.addDressItem(item)
        }
    }

    /**
     * 更新裙子（suspend）
     */
    suspend fun updateDressItem(item: DressItem) {
        withContext(Dispatchers.IO) {
            repository.updateDressItem(item)
        }
    }

    /**
     * 删除裙子（suspend）
     */
    suspend fun deleteDressItem(itemId: String) {
        withContext(Dispatchers.IO) {
            repository.deleteDressItem(itemId)
        }
    }

    /**
     * 置顶/取消置顶裙子（suspend）
     */
    suspend fun pinDressItem(itemId: String, pinned: Boolean) {
        withContext(Dispatchers.IO) {
            repository.pinDressItem(itemId, pinned)
        }
    }

    /**
     * 上移裙子（suspend）
     */
    suspend fun moveDressItemUp(itemId: String, wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveDressItemUp(itemId, wardrobeId)
        }
    }

    /**
     * 下移裙子（suspend）
     */
    suspend fun moveDressItemDown(itemId: String, wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveDressItemDown(itemId, wardrobeId)
        }
    }

    /**
     * 移到顶部（suspend）
     */
    suspend fun moveDressItemToTop(itemId: String, wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveDressItemToTop(itemId, wardrobeId)
        }
    }

    /**
     * 移到底部（suspend）
     */
    suspend fun moveDressItemToBottom(itemId: String, wardrobeId: String) {
        withContext(Dispatchers.IO) {
            repository.moveDressItemToBottom(itemId, wardrobeId)
        }
    }

    /**
     * 刷新全部裙子数据
     */
    fun fetchAllDressItems() {
        refreshStatisticsFromItems(_allDressItems.value ?: emptyList())
    }

    /**
     * 异步获取所有裙子数据（用于导出备份等场景）
     */
    suspend fun getAllDressItemsForExport(): List<DressItem> {
        return repository.getDressItemsAsync()
    }

    /**
     * 异步获取所有衣柜数据（用于导出备份等场景）
     */
    suspend fun getAllWardrobesForExport(): List<Wardrobe> {
        return repository.getWardrobesAsync()
    }

    /**
     * 异步获取排序后的裙子列表（suspend）
     */
    suspend fun getSortedItemsForWardrobe(wardrobeId: String): List<DressItem> {
        return withContext(Dispatchers.IO) {
            repository.getSortedItemsForWardrobe(wardrobeId)
        }
    }

    /**
     * 查找衣柜（suspend）
     */
    suspend fun findWardrobeById(id: String): Wardrobe? {
        return withContext(Dispatchers.IO) {
            repository.findWardrobeById(id)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedStatuses(statuses: List<String>) {
        _selectedStatuses.value = statuses
    }

    fun toggleStatus(status: String) {
        val current = _selectedStatuses.value?.toMutableList() ?: mutableListOf()
        if (current.contains(status)) {
            current.remove(status)
        } else {
            current.add(status)
        }
        _selectedStatuses.value = current
    }

    // ==================== 非挂起便捷方法（供 Java UI 层调用） ====================

    /**
     * 添加衣柜（非挂起，内部启动协程）
     */
    fun launchAddWardrobe(wardrobe: Wardrobe) {
        viewModelScope.launch { repository.addWardrobe(wardrobe) }
    }

    /**
     * 删除衣柜（非挂起，内部启动协程）
     */
    fun launchDeleteWardrobe(wardrobeId: String) {
        viewModelScope.launch { repository.deleteWardrobe(wardrobeId) }
    }

    /**
     * 上移衣柜（非挂起，内部启动协程）
     */
    fun launchMoveWardrobeUp(wardrobeId: String) {
        viewModelScope.launch { repository.moveWardrobeUp(wardrobeId) }
    }

    /**
     * 下移衣柜（非挂起，内部启动协程）
     */
    fun launchMoveWardrobeDown(wardrobeId: String) {
        viewModelScope.launch { repository.moveWardrobeDown(wardrobeId) }
    }

    /**
     * 添加裙子（非挂起，内部启动协程）
     */
    fun launchAddDressItem(item: DressItem) {
        viewModelScope.launch { repository.addDressItem(item) }
    }

    /**
     * 更新裙子（非挂起，内部启动协程）
     */
    fun launchUpdateDressItem(item: DressItem) {
        viewModelScope.launch { repository.updateDressItem(item) }
    }

    /**
     * 删除裙子（非挂起，内部启动协程）
     */
    fun launchDeleteDressItem(itemId: String) {
        viewModelScope.launch { repository.deleteDressItem(itemId) }
    }

    /**
     * 置顶/取消置顶裙子（非挂起，内部启动协程）
     */
    fun launchPinDressItem(itemId: String, pinned: Boolean) {
        viewModelScope.launch { repository.pinDressItem(itemId, pinned) }
    }

    /**
     * 上移裙子（非挂起，内部启动协程）
     */
    fun launchMoveDressItemUp(itemId: String, wardrobeId: String) {
        viewModelScope.launch { repository.moveDressItemUp(itemId, wardrobeId) }
    }

    /**
     * 下移裙子（非挂起，内部启动协程）
     */
    fun launchMoveDressItemDown(itemId: String, wardrobeId: String) {
        viewModelScope.launch { repository.moveDressItemDown(itemId, wardrobeId) }
    }

    /**
     * 移到顶部（非挂起，内部启动协程）
     */
    fun launchMoveDressItemToTop(itemId: String, wardrobeId: String) {
        viewModelScope.launch { repository.moveDressItemToTop(itemId, wardrobeId) }
    }

    /**
     * 移到底部（非挂起，内部启动协程）
     */
    fun launchMoveDressItemToBottom(itemId: String, wardrobeId: String) {
        viewModelScope.launch { repository.moveDressItemToBottom(itemId, wardrobeId) }
    }

    /**
     * 批量更新裙子（非挂起，内部启动协程）
     */
    fun launchUpdateAllDressItems(items: List<DressItem>) {
        viewModelScope.launch { repository.updateAllDressItems(items) }
    }

    /**
     * 批量更新衣柜（非挂起，内部启动协程）
     */
    fun launchUpdateAllWardrobes(wardrobes: List<Wardrobe>) {
        viewModelScope.launch { repository.updateAllWardrobes(wardrobes) }
    }

    /**
     * 查找衣柜（非挂起，通过回调返回结果）
     */
    fun launchFindWardrobeById(id: String, callback: (Wardrobe?) -> Unit) {
        viewModelScope.launch {
            val result = repository.findWardrobeById(id)
            callback(result)
        }
    }

    /**
     * 获取排序后的裙子列表（非挂起，通过回调返回结果）
     */
    fun launchGetSortedItemsForWardrobe(wardrobeId: String, callback: (List<DressItem>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getSortedItemsForWardrobe(wardrobeId)
            callback(result)
        }
    }

    /**
     * 通过 ID 查找裙子（非挂起，通过回调返回结果）
     */
    fun launchFindDressItemById(dressId: String, callback: (DressItem?) -> Unit) {
        viewModelScope.launch {
            val result = repository.findDressItemById(dressId)
            callback(result)
        }
    }

    /**
     * 获取所有衣柜列表（非挂起，通过回调返回结果）
     */
    fun launchGetAllWardrobes(callback: (List<Wardrobe>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getWardrobesAsync()
            callback(result)
        }
    }

    /**
     * DressItemEntity 转 Domain Model
     */
    private fun DressItemEntity.toDomain(): DressItem = DressItem(
        id = id, wardrobeId = wardrobeId, name = name, store = store, channel = channel,
        price = price, buyDate = buyDate, status = status, remindAt = remindAt, remark = remark,
        earnestMoney = earnestMoney, isFullPayment = isFullPayment, fullPaymentAmount = fullPaymentAmount,
        tailPayment = tailPayment, imageUri = imageUri, shippingFee = shippingFee, deposit = deposit,
        pinned = pinned, sortOrder = sortOrder, daiqiangDate = daiqiangDate, yixiangDate = yixiangDate,
        dingjinDate = dingjinDate, buweikuanDate = buweikuanDate, isWishlist = isWishlist, addTime = addTime,
        shipmentDate = shipmentDate, receivedDate = receivedDate, expectedShipmentDate = expectedShipmentDate
    )

    /**
     * WardrobeEntity 转 Domain Model
     */
    private fun WardrobeEntity.toDomain(): Wardrobe = Wardrobe(
        id = id, name = name, count = count, updatedAt = updatedAt,
        isDemo = isDemo, cover = cover, sortOrder = sortOrder
    )

    /**
     * DressItem Domain Model 转 Entity
     */
    private fun DressItem.toEntity(): DressItemEntity = DressItemEntity(
        id = id, wardrobeId = wardrobeId, name = name, store = store, channel = channel,
        price = price, buyDate = buyDate, status = status, remindAt = remindAt, remark = remark,
        earnestMoney = earnestMoney, isFullPayment = isFullPayment, fullPaymentAmount = fullPaymentAmount,
        tailPayment = tailPayment, imageUri = imageUri, shippingFee = shippingFee, deposit = deposit,
        pinned = pinned, sortOrder = sortOrder, daiqiangDate = daiqiangDate, yixiangDate = yixiangDate,
        dingjinDate = dingjinDate, buweikuanDate = buweikuanDate, isWishlist = isWishlist,
        addTime = if (addTime > 0L) addTime else System.currentTimeMillis(),
        shipmentDate = shipmentDate, receivedDate = receivedDate, expectedShipmentDate = expectedShipmentDate
    )

    /**
     * Wardrobe Domain Model 转 Entity
     */
    private fun Wardrobe.toEntity(): WardrobeEntity = WardrobeEntity(
        id = id, name = name, count = count, updatedAt = updatedAt,
        isDemo = isDemo, cover = cover, sortOrder = sortOrder
    )
}
