# 裙匣（LoXia）全文件详解报告

## 一、项目概述

裙匣（LoXia）是一款 Android Lolita 裙子衣柜管理应用，采用 Single-Activity + Fragment 架构，使用 Kotlin + Java 混合开发。

**技术栈**：
- 语言：Kotlin（ViewModel, Repository, Fragment）+ Java（Activity, Adapter）
- 架构：MVVM + Repository
- 数据库：Room（SQLite）
- UI：Material Design 3 + ViewBinding
- 异步：Kotlin Coroutines + Flow

---

## 二、源代码文件详解

### 2.1 应用入口

#### `LoXiaApp.java`
**路径**：`app/src/main/java/com/cy/loxia/LoXiaApp.java`

Application 类，应用启动时初始化：
- 全局协程作用域（`applicationScope`）
- 通知渠道（`CHANNEL_ID`）
- 主题模式（浅色/深色/跟随系统）

```java
public class LoXiaApp extends Application {
    public final CoroutineScope applicationScope = ...;
    public static final String CHANNEL_ID = "loxia_reminder";
}
```

#### `AndroidManifest.xml`
**路径**：`app/src/main/AndroidManifest.xml`

应用清单文件，声明：
- 权限：`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`
- Activity：`MainActivity`（主入口）, `MonthScheduleActivity`（月份详情）
- Receiver：`ReminderReceiver`（提醒）, `BootReceiver`（开机）

---

### 2.2 Activity 层

#### `MainActivity.java`
**路径**：`app/src/main/java/com/cy/loxia/MainActivity.java`

**核心 Activity**，约 2300 行代码，负责：
- 底部导航栏管理（主页、总览、我的）
- 所有子页面的显示/隐藏（ViewStub 懒加载）
- 裙子增删改查的 UI 逻辑
- 衣柜管理的 UI 逻辑
- 数据统计页面的初始化
- 详情页面的展示

**关键方法**：
| 方法 | 功能 |
|------|------|
| `initUIManagers()` | 初始化所有 UI 管理器 |
| `navigateToStats()` | 跳转收藏日程页 |
| `navigateToDressDetail(DressItem)` | 跳转裙子详情 |
| `navigateToDressDetail(String)` | 通过 ID 跳转裙子详情 |
| `showAddDressPage()` | 显示添加裙子页面 |
| `updateDressDetailPage()` | 更新裙子详情展示 |

**架构说明**：
- 使用 `NavigationManager` 管理页面栈
- 使用 `ViewStub` 懒加载子页面
- 使用 `MainViewModel` 获取数据
- 使用各种 `Manager` 类分离 UI 逻辑

#### `MonthScheduleActivity.kt`
**路径**：`app/src/main/java/com/cy/loxia/MonthScheduleActivity.kt`

月份详情页面，展示指定月份的所有收藏事件：
- 接收 `year`, `month` 参数
- 使用 `EventAggregator` 获取该月事件
- 使用 `CollectionEventAdapter` 展示列表

---

### 2.3 Fragment 层

#### `HomeFragment.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/HomeFragment.kt`

主页 Fragment，展示：
- 统计概览卡片（总件数、总花费、本月新增）
- 衣柜列表（使用 `WardrobeAdapter`）
- 添加衣柜按钮

**接口**：
```kotlin
interface Host {
    fun onWardrobeClick(wardrobe: Wardrobe)
    fun onAddWardrobe()
}
```

#### `OverviewFragment.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/OverviewFragment.kt`

总览 Fragment，展示：
- 搜索栏
- 状态筛选标签
- 全部裙子列表（使用 `OverviewDressAdapter`）

**功能**：
- 按名称/店铺搜索
- 按状态筛选（待抢、付意向、付定金等）
- 点击进入裙子详情

#### `ProfileFragment.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/ProfileFragment.kt`

个人中心 Fragment，展示：
- 用户头像、昵称、今日标语
- 统计卡片（总件数、总花费、本月新增）
- 菜单列表（收藏日程、主题设置、通知与提醒、数据导入）

**接口**：
```kotlin
interface Host {
    fun onProfileNavigateToThemeSettings()
    fun onProfileNavigateToStats()
    fun onProfileNavigateToDataImport()
    fun onProfileNavigateToNotifications()
    fun onProfileNavigateToProfileDetail()
}
```

---

### 2.4 ViewModel 层

#### `MainViewModel.kt`
**路径**：`app/src/main/java/com/cy/loxia/MainViewModel.kt`

主 ViewModel，约 670 行代码，负责：
- 数据状态管理（LiveData）
- 业务逻辑处理
- 数据转换（Entity ↔ Domain Model）

**核心数据流**：
```
Room Flow → Repository → ViewModel → UI (LiveData)
```

**主要 LiveData**：
| LiveData | 类型 | 说明 |
|----------|------|------|
| `wardrobeList` | `List<Wardrobe>` | 衣柜列表 |
| `selectedWardrobeId` | `String?` | 选中的衣柜 ID |
| `allDressItems` | `List<DressItem>` | 所有裙子（内部） |
| `filteredDressItems` | `List<DressItem>` | 筛选后的裙子 |
| `totalDressCount` | `Int` | 总件数 |
| `totalCost` | `Double` | 总花费 |
| `thisMonthItemCount` | `Int` | 本月新增 |

**关键方法**：
| 方法 | 功能 |
|------|------|
| `fetchStatistics()` | 刷新统计数据 |
| `saveDressItem()` | 保存裙子（suspend） |
| `importParsedData()` | 导入 AI 解析数据 |
| `launchAddDressItem()` | 添加裙子（非挂起） |
| `launchDeleteDressItem()` | 删除裙子（非挂起） |
| `launchFindDressItemById()` | 通过 ID 查找裙子 |

---

### 2.5 Repository 层

#### `DataRepository.kt`
**路径**：`app/src/main/java/com/cy/loxia/DataRepository.kt`

数据仓库，约 750 行代码，负责：
- 数据库访问（Room DAO）
- 数据迁移（SharedPreferences → Room）
- 默认数据初始化
- 错误事件通知

**关键方法**：
| 方法 | 功能 |
|------|------|
| `initializeAsync()` | 异步初始化 |
| `awaitReady()` | 等待初始化完成 |
| `getAllDressItemsFlow()` | 获取裙子 Flow |
| `addDressItem()` | 添加裙子（suspend） |
| `updateDressItem()` | 更新裙子（suspend） |
| `deleteDressItem()` | 删除裙子（suspend） |
| `migrateDataToRoomIfNeeded()` | 数据迁移 |

---

### 2.6 数据库层

#### `LoXiaDatabase.kt`
**路径**：`app/src/main/java/com/cy/loxia/data/db/LoXiaDatabase.kt`

Room 数据库定义：
- 版本：3
- 实体：`WardrobeEntity`, `DressItemEntity`, `OutfitRecordEntity`
- 迁移：`MIGRATION_1_2`（updatedAt 类型）, `MIGRATION_2_3`（新增时间字段）

#### `LoXiaDao.kt`
**路径**：`app/src/main/java/com/cy/loxia/data/db/LoXiaDao.kt`

数据访问对象，定义数据库操作：
- Wardrobe CRUD
- DressItem CRUD
- OutfitRecord CRUD
- 特殊查询（排序、统计）

**关键查询**：
```kotlin
@Query("SELECT * FROM dress_items WHERE id = :id")
suspend fun findDressItemById(id: String): DressItemEntity?

@Query("SELECT * FROM dress_items ORDER BY addTime DESC LIMIT :limit")
suspend fun getRecentDresses(limit: Int): List<DressItemEntity>
```

#### `Entities.kt`
**路径**：`app/src/main/java/com/cy/loxia/data/db/Entities.kt`

Room 实体定义：

**WardrobeEntity**（衣柜表）：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String (PK) | UUID |
| name | String | 衣柜名称 |
| count | Int | 裙子数量 |
| updatedAt | Long | 最后更新时间 |
| isDemo | Boolean | 是否演示数据 |
| cover | String | 封面图 URI |
| sortOrder | Int | 排序顺序 |

**DressItemEntity**（裙子表）：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String (PK) | UUID |
| wardrobeId | String (FK) | 所属衣柜 ID |
| name | String | 裙子名称 |
| store | String | 店铺/品牌 |
| channel | String | 购买渠道 |
| price | Double | 价格 |
| buyDate | String | 购入时间 |
| status | String | 状态（可组合） |
| earnestMoney | Double | 意向金 |
| deposit | Double | 定金 |
| tailPayment | Double | 尾款 |
| fullPaymentAmount | Double | 全款金额 |
| isFullPayment | Boolean | 是否全款支付 |
| shippingFee | String | 运费 |
| daiqiangDate | String | 待抢时间 |
| yixiangDate | String | 付意向截止时间 |
| dingjinDate | String | 付定金时间 |
| buweikuanDate | String | 补尾款时间 |
| shipmentDate | String | 实际发货时间 |
| receivedDate | String | 确认收货时间 |
| expectedShipmentDate | String | 预计发货时间 |
| imageUri | String | 图片 URI |
| pinned | Boolean | 是否置顶 |
| sortOrder | Int | 排序顺序 |
| addTime | Long | 添加时间 |

**OutfitRecordEntity**（穿搭记录表）：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增 ID |
| date | String | 日期 |
| dressIds | List<String> | 裙子 ID 列表 |
| photoPath | String | 照片路径 |
| mood | String | 心情 |
| createTime | Long | 创建时间 |

#### `Converters.kt`
**路径**：`app/src/main/java/com/cy/loxia/data/db/Converters.kt`

Room 类型转换器：
- `List<String>` ↔ `String`（JSON）

---

### 2.7 领域模型

#### `DressItem.kt`
**路径**：`app/src/main/java/com/cy/loxia/DressItem.kt`

裙子领域模型，与 `DressItemEntity` 字段一一对应：
- `getTailPaymentDate()` - 兼容旧代码
- `getEffectiveTotal()` - 计算有效总价（意向金+定金+尾款+运费）
- `toJson()` / `fromJson()` - JSON 序列化

#### `Wardrobe.kt`
**路径**：`app/src/main/java/com/cy/loxia/Wardrobe.kt`

衣柜领域模型：
```kotlin
data class Wardrobe(
    val id: String,
    val name: String,
    val count: Int = 0,
    val updatedAt: Long = 0L,
    val isDemo: Boolean = false,
    val cover: String = "",
    val sortOrder: Int = 0
)
```

#### `CollectionEvent.kt`
**路径**：`app/src/main/java/com/cy/loxia/CollectionEvent.kt`

收藏事件模型（新增）：
- `daysFromToday()` - 计算距离今天的天数
- `getRemainingText()` - 获取剩余天数描述

**EventType 枚举**：
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

---

### 2.8 事件聚合

#### `EventAggregator.kt`
**路径**：`app/src/main/java/com/cy/loxia/EventAggregator.kt`

事件聚合器，从 `List<DressItem>` 聚合出 `List<CollectionEvent>`：

| 方法 | 功能 |
|------|------|
| `aggregateEvents()` | 聚合所有事件 |
| `getUpcomingEvents(events, days)` | 获取未来 N 天事件 |
| `getEventsByMonth(events, year, month)` | 获取指定月份事件 |
| `getMonthStatistics(events, year)` | 按月份统计 |
| `getEventTypeStatistics(events)` | 按类型统计 |
| `getThisMonthPendingAmount(events)` | 本月待支付金额 |

---

### 2.9 Adapter 层

#### `DressItemAdapter.java`
**路径**：`app/src/main/java/com/cy/loxia/DressItemAdapter.java`

主页衣柜详情页的裙子列表适配器：
- 网格布局（2 列）
- 显示图片、名称、价格、状态
- 支持点击、长按操作

#### `OverviewDressAdapter.java`
**路径**：`app/src/main/java/com/cy/loxia/OverviewDressAdapter.java`

总览页的裙子列表适配器：
- 列表布局
- 显示图片、名称、店铺、状态
- 支持点击进入详情

#### `WardrobeAdapter.java`
**路径**：`app/src/main/java/com/cy/loxia/WardrobeAdapter.java`

主页衣柜列表适配器：
- 卡片布局
- 显示封面、名称、数量、更新时间
- 支持点击、长按操作

#### `StatsCardAdapter.java`
**路径**：`app/src/main/java/com/cy/loxia/StatsCardAdapter.java`

统计页的裙子卡片适配器（旧版，保留兼容）：
- 卡片布局
- 显示图片、名称、价格、状态

#### `CollectionEventAdapter.kt`
**路径**：`app/src/main/java/com/cy/loxia/CollectionEventAdapter.kt`

收藏事件列表适配器（新增）：
- 显示事件图标、裙子名称、事件类型、金额、日期、剩余天数
- 支持点击跳转

#### `MonthCardAdapter.kt`
**路径**：`app/src/main/java/com/cy/loxia/MonthCardAdapter.kt`

月份卡片适配器（新增）：
- 3 列网格
- 显示月份、节点数量、待支付金额
- 支持点击进入月份详情

#### `EventTypeTagAdapter.kt`
**路径**：`app/src/main/java/com/cy/loxia/EventTypeTagAdapter.kt`

事件类型标签适配器（新增）：
- 2 列网格
- 显示事件类型图标、名称、数量
- 支持点击筛选

---

### 2.10 UI 管理器

#### `NavigationManager.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/NavigationManager.kt`

页面导航管理器：
- 管理页面栈（push/pop）
- 控制页面显示/隐藏
- 更新标题栏
- 处理返回按钮

**支持的页面**：
| 页面 ID | 标题 |
|---------|------|
| `page_wardrobes` | 主页 |
| `page_overview` | 总览 |
| `page_profile` | 我的 |
| `page_detail` | 衣柜详情 |
| `page_add_dress` | 添加裙子 |
| `page_dress_detail` | 裙子详情 |
| `page_profile_detail` | 个人资料 |
| `page_stats` | 收藏日程 |
| `page_theme_settings` | 主题设置 |
| `page_notifications` | 通知设置 |
| `page_data_import` | 数据导入 |

#### `StatsPageManager.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/StatsPageManager.kt`

收藏日程页面管理器（重构后）：
- 初始化四部分 UI
- 使用 `EventAggregator` 聚合数据
- 更新概览卡片、近期待办、月份矩阵、事件类型统计

#### `DialogManager.kt`
**路径**：`app/src/main/java/com/cy/loxia/ui/DialogManager.kt`

对话框管理器：
- `showTotalCostDialog()` - 查看总花费确认框
- `showAddWardrobeDialog()` - 添加衣柜对话框
- `showDeleteConfirmDialog()` - 删除确认框

#### `DialogCallbacks.java`
**路径**：`app/src/main/java/com/cy/loxia/ui/DialogCallbacks.java`

对话框回调接口：
```java
public interface DialogCallbacks {
    interface OnConfirm { void onConfirm(); }
    interface OnCancel { void onCancel(); }
    interface OnInput { void onInput(String text); }
}
```

---

### 2.11 工具类

#### `ImageUtils.java`
**路径**：`app/src/main/java/com/cy/loxia/ImageUtils.java`

图片工具类：
- `loadIntoView()` - 加载图片到 ImageView
- `getRealPathFromURI()` - 从 URI 获取真实路径
- `copyImageToAppStorage()` - 复制图片到应用存储

#### `DataBackupManager.java`
**路径**：`app/src/main/java/com/cy/loxia/DataBackupManager.java`

数据备份管理器：
- `exportToJson()` - 导出为 JSON
- `importFromJson()` - 从 JSON 导入
- `exportToCsv()` - 导出为 CSV

#### `AppScopeProvider.kt`
**路径**：`app/src/main/java/com/cy/loxia/AppScopeProvider.kt`

协程作用域提供者：
```kotlin
object AppScopeProvider {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}
```

---

### 2.12 通知与提醒

#### `AlarmScheduler.java`
**路径**：`app/src/main/java/com/cy/loxia/AlarmScheduler.java`

闹钟调度器：
- `scheduleReminder()` - 设置提醒闹钟
- `cancelReminder()` - 取消提醒
- `scheduleDailyCheck()` - 设置每日检查

#### `ReminderReceiver.java`
**路径**：`app/src/main/java/com/cy/loxia/ReminderReceiver.java`

提醒广播接收器：
- 接收闹钟广播
- 显示通知
- 处理尾款到期提醒

#### `BootReceiver.java`
**路径**：`app/src/main/java/com/cy/loxia/BootReceiver.java`

开机广播接收器：
- 开机后重新设置闹钟
- 恢复提醒计划

---

## 三、资源文件详解

### 3.1 布局文件（Layout）

#### 主页面布局

| 文件 | 说明 |
|------|------|
| `activity_main.xml` | 主 Activity 布局，包含底部导航栏和所有子页面容器 |
| `layout_bottom_nav.xml` | 底部导航栏布局 |
| `page_my_wardrobes.xml` | 主页（衣柜列表） |
| `page_overview.xml` | 总览页（全部裙子） |
| `page_profile.xml` | 个人中心 |

#### 详情页面布局

| 文件 | 说明 |
|------|------|
| `page_wardrobe_detail.xml` | 衣柜详情页 |
| `page_dress_detail.xml` | 裙子详情页 |
| `page_add_dress_new.xml` | 添加/编辑裙子页 |

#### 功能页面布局

| 文件 | 说明 |
|------|------|
| `page_stats.xml` | 收藏日程页（重构后） |
| `page_theme_settings.xml` | 主题设置页 |
| `page_notifications.xml` | 通知设置页 |
| `page_data_import.xml` | 数据导入页 |
| `page_profile_detail.xml` | 个人资料页 |
| `activity_month_schedule.xml` | 月份详情页（新增） |

#### 列表项布局

| 文件 | 说明 |
|------|------|
| `item_wardrobe.xml` | 衣柜列表项 |
| `item_dress_item.xml` | 裙子列表项（主页） |
| `item_overview_dress.xml` | 裙子列表项（总览） |
| `item_stats_dress_card.xml` | 统计页裙子卡片 |
| `item_collection_event.xml` | 收藏事件卡片（新增） |
| `item_month_card.xml` | 月份卡片（新增） |
| `item_event_type_tag.xml` | 事件类型标签（新增） |

#### 对话框布局

| 文件 | 说明 |
|------|------|
| `dialog_add_wardrobe.xml` | 添加衣柜对话框 |

---

### 3.2 Drawable 资源

#### 背景资源

| 文件 | 说明 |
|------|------|
| `bg_bottom_nav.xml` | 底部导航栏背景 |
| `bg_nav_floating.xml` | 导航浮动背景 |
| `bg_nav_indicator.xml` | 导航指示器背景 |
| `bg_nav_panel.xml` | 导航面板背景 |
| `bg_nav_tab_active.xml` | 导航标签激活背景 |
| `bg_status_pill.xml` | 状态标签背景 |
| `bg_step_number.xml` | 步骤数字背景 |
| `bg_image_placeholder.xml` | 图片占位背景 |
| `bg_event_icon.xml` | 事件图标背景（新增） |
| `rounded_image_bg.xml` | 圆角图片背景 |

#### 图标资源（Vector Drawable）

| 文件 | 说明 |
|------|------|
| `ic_launcher_foreground.xml` | 启动图标前景 |
| `ic_launcher_background.xml` | 启动图标背景 |
| `ic_dress.xml` / `ic_dress_fill.xml` | 裙子图标 |
| `ic_wardrobe.xml` / `ic_wardrobe_fill.xml` | 衣柜图标 |
| `ic_heart.xml` / `ic_heart_fill.xml` | 心形图标 |
| `ic_star.xml` / `ic_star_fill.xml` | 星星图标 |
| `ic_calendar.xml` / `ic_calendar_fill.xml` | 日历图标 |
| `ic_gear.xml` / `ic_gear_fill.xml` | 设置图标 |
| `ic_user.xml` / `ic_user_fill.xml` | 用户图标 |
| `ic_money.xml` / `ic_money_fill.xml` | 金钱图标 |
| `ic_package.xml` / `ic_package_fill.xml` | 包裹图标 |
| `ic_truck.xml` | 卡车图标（发货） |
| `ic_plus.xml` / `ic_plus_circle.xml` | 添加图标 |
| `ic_pencil.xml` | 编辑图标 |
| `ic_trash.xml` | 删除图标 |
| `ic_check.xml` / `ic_check_circle.xml` | 检查图标 |
| `ic_close.xml` / `ic_x.xml` | 关闭图标 |
| `ic_back.xml` / `ic_back_arrow.xml` | 返回图标 |
| `ic_arrow_left.xml` / `ic_arrow_right.xml` | 箭头图标 |
| `ic_arrow_up.xml` / `ic_arrow_down.xml` | 箭头图标 |
| `ic_chevron_right.xml` | 右箭头图标 |
| `ic_eye.xml` / `ic_eye_closed.xml` | 眼睛图标 |
| `ic_copy.xml` / `ic_clipboard.xml` | 复制图标 |
| `ic_download.xml` / `ic_upload.xml` | 下载/上传图标 |
| `ic_image.xml` | 图片图标 |
| `ic_alarm.xml` | 闹钟图标 |
| `ic_broom.xml` | 扫帚图标（清理） |
| `ic_chart_bar.xml` / `ic_chart_pie.xml` | 图表图标 |
| `ic_credit_card.xml` / `ic_credit_card_fill.xml` | 信用卡图标 |
| `ic_moon.xml` / `ic_moon_fill.xml` | 月亮图标 |
| `ic_sun.xml` / `ic_sun_fill.xml` | 太阳图标 |
| `ic_receipt.xml` | 收据图标 |
| `ic_sparkle.xml` | 闪光图标 |
| `ic_stack.xml` | 堆叠图标 |
| `ic_storefront.xml` | 店铺图标 |
| `ic_tag.xml` | 标签图标 |
| `default_wardrobe_cover.xml` | 默认衣柜封面 |

---

### 3.3 颜色资源

#### `values/colors.xml`
**路径**：`app/src/main/res/values/colors.xml`

浅色模式颜色定义：

| 颜色名 | 值 | 说明 |
|--------|-----|------|
| `pink_primary` | #F58FB2 | 主粉色 |
| `pink_secondary` | #FAD6E3 | 次粉色 |
| `accent_purple` | #8E7CFF | 强调紫色 |
| `bg_soft` | #FFF7FA | 柔和背景 |
| `text_primary` | #2D2430 | 主文本 |
| `text_secondary` | #7A6D78 | 次文本 |
| `success_green` | #6BCB77 | 成功绿色 |
| `warning_orange` | #FFB020 | 警告橙色 |
| `danger_red` | #FF6B6B | 危险红色 |
| `summary_card_bg` | #FAD6E3 | 概览卡片背景 |
| `card_bg_white` | #FEFEFE | 卡片白色背景 |

#### `values-night/colors.xml`
**路径**：`app/src/main/res/values-night/colors.xml`

深色模式颜色定义（覆盖浅色模式）。

---

### 3.4 尺寸资源

#### `values/dimens.xml`
**路径**：`app/src/main/res/values/dimens.xml`

尺寸定义：

| 类别 | 尺寸名 | 值 |
|------|--------|-----|
| 间距 | `spacing_xs` | 4dp |
| 间距 | `spacing_sm` | 8dp |
| 间距 | `spacing_md` | 12dp |
| 间距 | `spacing_base` | 16dp |
| 间距 | `spacing_lg` | 24dp |
| 间距 | `spacing_xl` | 32dp |
| 卡片 | `card_radius` | 20dp |
| 卡片 | `card_radius_small` | 16dp |
| 卡片 | `card_radius_large` | 24dp |
| 卡片 | `card_elevation` | 3dp |
| 卡片 | `card_padding` | 16dp |
| 文字 | `text_caption` | 12sp |
| 文字 | `text_body` | 14sp |
| 文字 | `text_body_large` | 16sp |
| 文字 | `text_subtitle` | 20sp |
| 文字 | `text_title` | 24sp |
| 图标 | `icon_sm` | 20dp |
| 图标 | `icon_md` | 24dp |
| 图标 | `icon_lg` | 32dp |

---

### 3.5 字符串资源

#### `values/strings.xml`
**路径**：`app/src/main/res/values/strings.xml`

字符串资源，包含：
- 应用名称
- 页面标题
- 表单提示
- 状态标签
- 通知文案
- AI 导入提示词
- 每日标语

**关键字符串**：
| 字符串名 | 值 |
|----------|-----|
| `app_name` | LoXia |
| `title_stats` | 收藏日程 |
| `status_daiqiang` | 待抢 |
| `status_fuyixiang` | 付意向 |
| `status_fudingjin` | 付定金 |
| `status_buweikuan` | 补尾款 |
| `status_daifahuo` | 待发货 |
| `status_daoshou` | 已到手 |

---

### 3.6 主题资源

#### `values/themes.xml`
**路径**：`app/src/main/res/values/themes.xml`

浅色主题定义：
```xml
<style name="Theme.LoXia" parent="Theme.Material3.Light.NoActionBar">
    <item name="colorPrimary">@color/pink_primary</item>
    <item name="colorSecondary">@color/accent_purple</item>
    ...
</style>
```

#### `values-night/themes.xml`
**路径**：`app/src/main/res/values-night/themes.xml`

深色主题定义。

---

### 3.7 菜单资源

#### `menu/bottom_nav_menu.xml`
**路径**：`app/src/main/res/menu/bottom_nav_menu.xml`

底部导航栏菜单定义：
```xml
<menu>
    <item android:id="@+id/nav_home" android:title="主页" android:icon="@drawable/ic_wardrobe" />
    <item android:id="@+id/nav_overview" android:title="总览" android:icon="@drawable/ic_dress" />
    <item android:id="@+id/nav_profile" android:title="我的" android:icon="@drawable/ic_user" />
</menu>
```

---

### 3.8 颜色选择器

#### `color/nav_icon_color.xml`
**路径**：`app/src/main/res/color/nav_icon_color.xml`

导航图标颜色选择器：
```xml
<selector>
    <item android:color="@color/pink_primary" android:state_checked="true" />
    <item android:color="@color/nav_unselected" />
</selector>
```

#### `color/nav_item_color.xml`
**路径**：`app/src/main/res/color/nav_item_color.xml`

导航文字颜色选择器。

---

### 3.9 XML 配置

#### `xml/backup_rules.xml`
**路径**：`app/src/main/res/xml/backup_rules.xml`

备份规则配置。

#### `xml/data_extraction_rules.xml`
**路径**：`app/src/main/res/xml/data_extraction_rules.xml`

数据提取规则配置。

---

## 四、架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
├─────────────────────────────────────────────────────────────┤
│  MainActivity.java                                          │
│    ├─ HomeFragment.kt (主页)                                │
│    ├─ OverviewFragment.kt (总览)                            │
│    ├─ ProfileFragment.kt (我的)                             │
│    └─ StatsPageManager.kt (收藏日程)                        │
│                                                             │
│  MonthScheduleActivity.kt (月份详情)                        │
│                                                             │
│  Adapters:                                                  │
│    ├─ WardrobeAdapter.java                                  │
│    ├─ DressItemAdapter.java                                 │
│    ├─ OverviewDressAdapter.java                             │
│    ├─ StatsCardAdapter.java                                 │
│    ├─ CollectionEventAdapter.kt                             │
│    ├─ MonthCardAdapter.kt                                   │
│    └─ EventTypeTagAdapter.kt                                │
├─────────────────────────────────────────────────────────────┤
│                     ViewModel Layer                         │
├─────────────────────────────────────────────────────────────┤
│  MainViewModel.kt                                           │
│    ├─ LiveData<List<Wardrobe>>                              │
│    ├─ LiveData<List<DressItem>>                             │
│    ├─ LiveData<Int> (totalDressCount)                       │
│    └─ LiveData<Double> (totalCost)                          │
├─────────────────────────────────────────────────────────────┤
│                    Repository Layer                          │
├─────────────────────────────────────────────────────────────┤
│  DataRepository.kt                                          │
│    ├─ Room DAO (LoXiaDao)                                   │
│    ├─ SharedPreferences (迁移、设置)                        │
│    └─ 数据迁移逻辑                                          │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                             │
├─────────────────────────────────────────────────────────────┤
│  LoXiaDatabase.kt (Room Database)                           │
│    ├─ WardrobeEntity (衣柜表)                               │
│    ├─ DressItemEntity (裙子表)                              │
│    └─ OutfitRecordEntity (穿搭记录表)                       │
│                                                             │
│  Domain Models:                                             │
│    ├─ Wardrobe.kt                                           │
│    ├─ DressItem.kt                                          │
│    └─ CollectionEvent.kt                                    │
│                                                             │
│  EventAggregator.kt (事件聚合)                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 五、数据流图

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Room DB   │ →  │ Repository  │ →  │  ViewModel  │
│  (SQLite)   │    │  (DataRepo) │    │ (MainVM)    │
└─────────────┘    └─────────────┘    └─────────────┘
                                            │
                                            ↓
                                     ┌─────────────┐
                                     │   LiveData   │
                                     └─────────────┘
                                            │
                                            ↓
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│     UI      │ ←  │   Manager   │ ←  │  Observer   │
│  (Fragment) │    │ (PageMgr)   │    │ (Lifecycle) │
└─────────────┘    └─────────────┘    └─────────────┘
```

---

## 六、文件统计

### 6.1 源代码文件

| 类型 | 数量 | 说明 |
|------|------|------|
| Kotlin (.kt) | 20 | ViewModel, Repository, Fragment, Model, Adapter |
| Java (.java) | 14 | Activity, Adapter, Receiver, Utils |
| XML Layout | 22 | 页面布局、列表项、对话框 |
| XML Drawable | 50+ | 图标、背景、形状 |
| XML Values | 6 | 颜色、尺寸、字符串、主题 |
| XML Other | 5 | 菜单、颜色选择器、配置 |

**总计**：约 120 个文件

### 6.2 代码行数估算

| 组件 | 行数 |
|------|------|
| MainActivity.java | ~2300 |
| MainViewModel.kt | ~670 |
| DataRepository.kt | ~750 |
| StatsPageManager.kt | ~300 |
| Fragment (3个) | ~500 |
| Adapter (7个) | ~800 |
| Model (4个) | ~400 |
| Database | ~300 |
| Utils | ~500 |
| **总计** | **~6500** |

---

## 七、关键依赖

### 7.1 Android Jetpack

- `androidx.room:room-runtime` - 数据库
- `androidx.lifecycle:lifecycle-viewmodel-ktx` - ViewModel
- `androidx.lifecycle:lifecycle-livedata-ktx` - LiveData
- `androidx.fragment:fragment-ktx` - Fragment
- `androidx.recyclerview:recyclerview` - 列表
- `androidx.cardview:cardview` - 卡片

### 7.2 Material Design

- `com.google.android.material:material` - Material 3 组件

### 7.3 图片加载

- `com.github.bumptech.glide:glide` - 图片加载（或自定义 ImageUtils）

### 7.4 图片裁剪

- `com.yalantis:ucrop` - 图片裁剪

---

## 八、总结

裙匣（LoXia）是一款功能完善的 Lolita 裙子衣柜管理应用，采用现代 Android 开发架构，代码结构清晰，功能模块化。

**核心功能**：
1. 衣柜管理（增删改查、排序）
2. 裙子管理（增删改查、状态、时间节点）
3. 收藏日程（时间管理、待办提醒）
4. 数据导入（AI 智能导入）
5. 数据备份（JSON 导入导出）
6. 通知提醒（尾款到期提醒）
7. 主题切换（浅色/深色）

**技术特点**：
- MVVM + Repository 架构
- Room 数据库 + Flow 响应式
- Kotlin + Java 混合开发
- Material Design 3 UI
- 模块化设计（Manager 模式）
