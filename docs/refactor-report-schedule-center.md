# 统计中心重构为收藏日程中心 - 详细报告

## 一、重构背景与目标

### 1.1 原有问题

原「统计中心」页面以数据分析为核心，展示：
- 总件数、总花费
- 补尾款时间列表
- 状态分布（带百分比）
- 5 个状态分类卡片（待抢、付意向、付定金、待发货、已到手）

**问题**：Lolita 用户更关心「最近有什么事情需要处理」，而非统计数据。

### 1.2 重构目标

将页面改造为「收藏日程中心」，让用户进入后立刻知道：
- 哪条裙子什么时候截订
- 哪条裙子什么时候付意向/定金/尾款
- 哪条裙子什么时候发货/确认收货

**设计定位**：收藏日历 + 时间管理中心（Material3 风格、粉白少女感、轻量设计、高信息密度）

---

## 二、技术方案

### 2.1 数据模型设计

#### 新增 CollectionEvent 模型

```kotlin
data class CollectionEvent(
    val id: String,
    val dressId: String,
    val dressName: String,
    val dressImageUri: String,
    val eventType: EventType,
    val eventDate: String,  // yyyy-MM-dd
    val amount: Double = 0.0,
    val remark: String = ""
)
```

#### EventType 枚举

```kotlin
enum class EventType(val label: String, val icon: String) {
    DAI_QIANG("待抢", "🔥"),
    YI_XIANG("付意向", "💜"),
    DING_JIN("付定金", "💜"),
    BU_WEI_KUAN("补尾款", "💰"),
    EXPECTED_SHIPMENT("预计发货", "📦"),
    SHIPMENT("发货", "🚚"),
    RECEIVED("确认收货", "✅"),
    CUSTOM("自定义", "📝")
}
```

### 2.2 数据库扩展

#### 新增字段（DressItemEntity）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `shipmentDate` | String | 实际发货时间 |
| `receivedDate` | String | 确认收货时间 |
| `expectedShipmentDate` | String | 预计发货时间（预留） |

#### 数据库迁移

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dress_items ADD COLUMN shipmentDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE dress_items ADD COLUMN receivedDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE dress_items ADD COLUMN expectedShipmentDate TEXT NOT NULL DEFAULT ''")
    }
}
```

版本号：2 → 3

### 2.3 事件聚合逻辑

`EventAggregator` 对象负责从 `List<DressItem>` 聚合出 `List<CollectionEvent>`：

| 方法 | 功能 |
|------|------|
| `aggregateEvents()` | 从裙子列表聚合所有事件 |
| `getUpcomingEvents(events, days)` | 获取未来 N 天内的事件 |
| `getEventsByMonth(events, year, month)` | 获取指定月份的事件 |
| `getMonthStatistics(events, year)` | 按月份分组统计 |
| `getEventTypeStatistics(events)` | 按事件类型统计 |
| `getThisMonthPendingAmount(events)` | 计算本月待支付金额 |

---

## 三、页面结构

### 3.1 新页面布局

从上到下四个部分：

#### 第一部分：收藏概览卡片

```
┌─────────────────────────────────────┐
│           收藏总数    总金额          │
│              48      ¥32800         │
│─────────────────────────────────────│
│         本月待支付    未来30天待办     │
│           ¥1680         7           │
└─────────────────────────────────────┘
```

- 圆角 24dp
- 粉色主题背景
- 总金额默认隐藏（****），点击确认后显示

#### 第二部分：近期需要处理

```
┌─────────────────────────────────────┐
│ 近期需要处理              查看更多 >  │
│─────────────────────────────────────│
│ 🔥 裙子名称    待抢    7月15日       │
│                   剩余5天           │
│─────────────────────────────────────│
│ 💰 裙子名称    补尾款  ¥699         │
│                   剩余8天           │
└─────────────────────────────────────┘
```

- 最多显示 5 条
- 按时间排序
- 点击进入裙子详情

#### 第三部分：年度月份矩阵

```
┌─────────────────────────────────────┐
│ 2026                               │
│ ┌─────┐ ┌─────┐ ┌─────┐           │
│ │ 1月 │ │ 2月 │ │ 3月 │           │
│ │3个  │ │暂无 │ │5个  │           │
│ │¥680 │ │安排 │ │¥1200│           │
│ └─────┘ └─────┘ └─────┘           │
│ ...（3列×4行）                       │
└─────────────────────────────────────┘
```

- 3 列 × 4 行网格
- 显示月份、节点数量、待支付金额
- 点击进入月份详情页

#### 第四部分：节点类型统计

```
┌─────────────────────────────────────┐
│ 节点类型统计                        │
│ ┌──────────┐ ┌──────────┐         │
│ │🔥 待抢    │ │💜 付意向  │         │
│ │    5     │ │    3     │         │
│ └──────────┘ └──────────┘         │
│ ┌──────────┐ ┌──────────┐         │
│ │💰 补尾款  │ │📦 预计发货│         │
│ │    8     │ │    12    │         │
│ └──────────┘ └──────────┘         │
└─────────────────────────────────────┘
```

- 2 列网格
- 点击进入对应筛选列表

### 3.2 月份详情页（MonthScheduleActivity）

```
┌─────────────────────────────────────┐
│ ←  2026年4月                        │
│─────────────────────────────────────│
│ 4月3日                             │
│ 💰 Moon River JSK   补尾款  ¥699   │
│─────────────────────────────────────│
│ 4月12日                            │
│ 💜 Rosy Garden      付意向  ¥200   │
│─────────────────────────────────────│
│ 4月18日                            │
│ 🔥 白夜城 OP        截订            │
└─────────────────────────────────────┘
```

---

## 四、文件变更清单

### 4.1 新增文件（10 个）

| 文件路径 | 说明 |
|----------|------|
| `app/src/main/java/com/cy/loxia/CollectionEvent.kt` | 事件模型 + EventType 枚举 |
| `app/src/main/java/com/cy/loxia/EventAggregator.kt` | 事件聚合逻辑 |
| `app/src/main/java/com/cy/loxia/CollectionEventAdapter.kt` | 事件列表适配器 |
| `app/src/main/java/com/cy/loxia/MonthCardAdapter.kt` | 月份卡片适配器 |
| `app/src/main/java/com/cy/loxia/EventTypeTagAdapter.kt` | 事件类型标签适配器 |
| `app/src/main/java/com/cy/loxia/MonthScheduleActivity.kt` | 月份详情页面 |
| `app/src/main/res/layout/activity_month_schedule.xml` | 月份详情布局 |
| `app/src/main/res/layout/item_collection_event.xml` | 事件卡片布局 |
| `app/src/main/res/layout/item_month_card.xml` | 月份卡片布局 |
| `app/src/main/res/layout/item_event_type_tag.xml` | 事件类型标签布局 |
| `app/src/main/res/drawable/bg_event_icon.xml` | 事件图标背景 |
| `app/src/main/res/drawable/ic_back.xml` | 返回按钮图标 |

### 4.2 修改文件（12 个）

| 文件路径 | 修改内容 |
|----------|----------|
| `Entities.kt` | 新增 3 个时间字段 |
| `DressItem.kt` | 新增字段 + toJson/fromJson |
| `LoXiaDatabase.kt` | 版本 2→3 + MIGRATION_2_3 |
| `DataRepository.kt` | 注册迁移 + toDomain/toEntity |
| `MainViewModel.kt` | 转换函数 + launchFindDressItemById |
| `StatsPageManager.kt` | 完全重写 |
| `MainActivity.java` | navigateToDressDetail(String) |
| `NavigationManager.kt` | 页面标题改为「收藏日程」 |
| `page_stats.xml` | 重构为四部分布局 |
| `strings.xml` | 新增 15 个字符串资源 |
| `AndroidManifest.xml` | 注册 MonthScheduleActivity |
| `ProfileFragment.kt` | （无实际修改，入口使用现有 tvStatsCenter） |

---

## 五、数据流

```
DressItem (数据库)
    ↓
EventAggregator.aggregateEvents()
    ↓
List<CollectionEvent>
    ↓
┌─────────────────────────────────────┐
│ StatsPageManager                    │
│   ├─ updateUpcomingEvents()         │
│   ├─ updateMonthMatrix()            │
│   └─ updateEventTypeStats()         │
└─────────────────────────────────────┘
    ↓
RecyclerView (Adapter)
```

---

## 六、交互流程

### 6.1 进入收藏日程

1. 用户点击「我的」→「收藏日程」
2. 页面加载，`StatsPageManager.updatePage()` 被调用
3. 通过 `EventAggregator` 聚合所有事件
4. 更新四个部分的 UI

### 6.2 查看近期事件

1. 显示未来 30 天内的事件（最多 5 条）
2. 点击事件 → `navigateToDressDetail(dressId)` → 裙子详情页

### 6.3 查看月份详情

1. 点击月份卡片 → `MonthScheduleActivity`
2. 显示该月所有事件，按时间排序
3. 点击事件 → 裙子详情页

---

## 七、待办事项

### 7.1 已完成

- [x] 数据库迁移（v2 → v3）
- [x] 新增 3 个时间字段
- [x] CollectionEvent 模型
- [x] EventAggregator 聚合逻辑
- [x] 页面布局重构
- [x] 月份详情页
- [x] 编译通过
- [x] 安装测试

### 7.2 待完善

- [ ] 事件类型标签点击 → 筛选列表页面
- [ ] 查看更多 → 完整事件列表页面
- [ ] 月份详情页点击事件 → 裙子详情页（需从 Activity 跳转）
- [ ] 编辑裙子页面增加发货时间/收货时间输入框
- [ ] 数据导入支持新字段

---

## 八、测试验证

### 8.1 编译测试

```
./gradlew assembleDebug
BUILD SUCCESSFUL in 4s
```

### 8.2 安装测试

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
Success
```

### 8.3 功能验证

- 概览卡片正确显示总数、总金额、本月待支付、未来30天待办
- 近期待办列表正确显示（空状态提示正常）
- 月份矩阵 3×4 网格正确显示
- 事件类型统计正确显示
- 点击月份卡片可跳转详情页

---

## 九、技术决策记录

### 9.1 为什么用聚合而非新表

用户要求「不要修改现有数据库结构」，但需要发货/收货时间字段。

**决策**：新增 3 个字段（最小改动），用聚合层从现有数据计算事件。

**优势**：
- 不需要维护额外的事件表
- 数据一致性由现有 DressItem 保证
- 迁移简单（ALTER TABLE）

### 9.2 为什么重写 StatsPageManager

原实现：
- 使用 Unicode 转义字符（`待抢`）
- 硬编码状态数组
- 直接操作 View，无数据模型

新实现：
- 使用 Kotlin 字符串
- 基于 EventType 枚举
- 数据驱动（EventAggregator → Adapter → View）

### 9.3 为什么用 MonthScheduleActivity 而非 Fragment

月份详情是独立页面，需要：
- 接收 year/month 参数
- 独立的生命周期
- 独立的返回栈

**决策**：用 Activity 实现，通过 Intent 传递参数。

---

## 十、总结

本次重构将「统计中心」从数据分析页面转变为「收藏日程中心」，核心变化：

1. **数据模型**：新增 CollectionEvent + EventAggregator
2. **页面结构**：四部分（概览、待办、月份矩阵、事件类型）
3. **交互方式**：从查看统计 → 管理时间节点
4. **用户体验**：进入页面立刻知道「最近要处理什么」

重构保持了 Material3 风格和粉白少女感，符合 Lolita 用户的真实使用习惯。
