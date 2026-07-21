# 裙匣（LoXia）UI 设计总览

> 本文档汇总项目所有 UI 相关内容：架构、布局、主题配色、字体图标、导航交互、Fragment/Manager 职责，以及适配器与弹窗体系。

---

## 一、整体架构

裙匣采用 **Single-Activity + Fragment** 架构，辅以 ViewStub 延迟加载的子页面覆盖层（overlay）。UI 层基于 **Material Design 3**，使用 **ViewBinding** 绑定视图，整体呈现粉色少女系 Lolita 风格。

```
MainActivity (ConstraintLayout)
├── main_content_container (FrameLayout)
│   └── Fragment 容器：HomeFragment / OverviewFragment / ProfileFragment
├── sub_page_container (FrameLayout, 默认 gone)
│   ├── MaterialToolbar (顶部工具栏)
│   ├── sub_page_content (子页面内容)
│   │   └── 8 个 ViewStub（按需 inflate 子页面）
│   └── layout_bottom_nav (底部导航胶囊)
└── MonthScheduleActivity (独立 Activity，月份事件列表)
```

**两种页面切换模式：**

1. **主 Tab 切换** — 通过 Fragment 事务 `replace()` 在 `main_content_container` 中切换 HomeFragment / OverviewFragment / ProfileFragment，底部导航栏始终可见。
2. **子页面覆盖** — 通过 `NavigationManager` 在 `sub_page_container` 中显示子页面（衣柜详情、添加裙子、裙子详情、统计、主题、通知、数据导入等），主内容容器隐藏，底部导航按需隐藏。子页面使用 ViewStub 首次延迟加载，加载后缓存在内存。

---

## 二、导航体系

### 底部导航（三 Tab）

底部导航采用 **Apple 风格悬浮胶囊** 设计，非传统 BottomNavigationView。

| Tab | 图标（Regular / Fill） | 标签 | Fragment |
|-----|----------------------|------|----------|
| 主页 | `ic_wardrobe` / `ic_wardrobe_fill` | 主页 | HomeFragment |
| 总览 | `ic_dress` / `ic_dress_fill` | 总览 | OverviewFragment |
| 我的 | `ic_user` / `ic_user_fill` | 我的 | ProfileFragment |

胶囊结构（`layout_bottom_nav.xml`）：`MaterialCardView`（圆角 32dp）内含滑动指示器 + 三个 FrameLayout Tab，每个 Tab 含 ImageView（22×22dp）+ TextView（10sp）。选中态通过 `nav_icon_color.xml` / `nav_item_color.xml` 颜色选择器切换为 `pink_primary`。

### 子页面导航（NavigationManager）

`NavigationManager` 管理子页面的显示/隐藏、标题栏、返回栈。维护一个 `pageStack` 实现返回逻辑。

| 页面 key | 标题 | 布局文件 | 说明 |
|----------|------|----------|------|
| `page_detail` | 柜子详情 | `page_wardrobe_detail.xml` | 衣柜内衣服列表 |
| `page_add_dress` | 添加衣服 | `page_add_dress_new.xml` | 添加/编辑裙子表单 |
| `page_dress_detail` | 裙子详情 | `page_dress_detail.xml` | 衣服详情只读展示 |
| `page_profile_detail` | 个人资料 | `page_profile_detail.xml` | 编辑头像昵称 |
| `page_stats` | 收藏日程 | `page_stats.xml` | 统计中心 |
| `page_theme_settings` | 主题设置 | `page_theme_settings.xml` | 浅色/深色/跟随系统 |
| `page_notifications` | 通知设置 | `page_notifications.xml` | 提醒时间/提前天数 |
| `page_data_import` | 数据导入 | `page_data_import.xml` | AI 辅助三步导入 |

---

## 三、主题与配色

### 主题定义（`themes.xml`）

基础主题 `Base.Theme.LoXia` 继承 `Theme.Material3.DayNight.NoActionBar`，核心配置：

- `colorPrimary` = `pink_primary`（#F58FB2）
- `colorPrimaryContainer` = `pink_secondary`（#FAD6E3）
- `dialogCornerRadius` = 24dp
- `fontFamily` = `@font/harmonyos_sans`（全局鸿蒙字体）

`Theme.LoXia` 继承基础主题，注入全局 `materialCardViewStyle`（统一浅灰细描边 0.5dp + 阴影）。

### 全局样式

| 样式名 | 继承 | 圆角 | 特点 |
|--------|------|------|------|
| `Widget.LoXia.CardView` | `Widget.Material3.CardView.Outlined` | — | 0.5dp 浅灰描边 |
| `Widget.LoXia.Button` | `Widget.Material3.Button` | 24dp | 粉色填充，加粗 14sp，minHeight 48dp |
| `Widget.LoXia.Button.Outlined` | `Widget.Material3.Button.OutlinedButton` | 24dp | 粉色描边，粉色文字 |
| `Widget.LoXia.Button.Text` | `Widget.Material3.Button.TextButton` | 24dp | 粉色文字 |
| `RoundedAlertDialog` | `ThemeOverlay.Material3.MaterialAlertDialog` | 24dp | 粉色主色 |
| `BottomNavIndicator` | `Widget.Material3.BottomNavigationView.ActiveIndicator` | — | 粉色半透明，无阴影 |
| `UCropTheme` | `Theme.AppCompat.Light.NoActionBar` | — | 裁剪页粉色状态栏 |

### 核心色板

#### 浅色模式

| 颜色 | 色值 | 用途 |
|------|------|------|
| `pink_primary` | `#F58FB2` | 主题主色（粉红） |
| `pink_secondary` | `#FAD6E3` | 辅色 / PrimaryContainer |
| `accent_purple` | `#8E7CFF` | 紫色强调 |
| `bg_soft` | `#FFF7FA` | 浅粉背景 |
| `text_primary` | `#2D2430` | 主文字（深紫黑） |
| `text_secondary` | `#7A6D78` | 次要文字 |
| `card_bg_white` | `#FEFEFE` | 卡片白底 |
| `card_border` | `#EAEAEA` | 卡片描边 |
| `status_pill_bg` | `#CCF58FB2` | 状态药丸（80% 粉） |
| `success_green` | `#6BCB77` | 成功 |
| `warning_orange` | `#FFB020` | 警告 |
| `danger_red` | `#FF6B6B` | 危险 |

#### 暗色模式（`values-night/`）

| 颜色 | 暗色值 | 说明 |
|------|--------|------|
| `bg_soft` / `bg_dark` | `#1C1C1E` / `#000000` | 纯黑底 |
| `surface_dark` | `#1A1A1A` | 深灰面板 |
| `text_primary` | `#F0F0F0` | 浅白文字 |
| `text_secondary` | `#AAAAAA` | 灰白次要 |
| `card_bg_white` | `#1E1E1E` | 暗色卡片 |
| `nav_bg` | `#000000` | 底栏纯黑 |

粉色调（`pink_primary` / `pink_secondary`）在深浅两套主题中保持不变，贯穿全局。

---

## 四、字体

**字体家族**：HarmonyOS Sans（华为鸿蒙字体），全局通过 `android:fontFamily` + `fontFamily` 应用。

| 字体名 | 字重 | 文件 |
|--------|------|------|
| `harmonyos_sans_regular` | 400 | `harmonyos_sans_regular.ttf` |
| `harmonyos_sans_medium` | 500 | `harmonyos_sans_medium.ttf` |
| `harmonyos_sans_bold` | 700 | `harmonyos_sans_bold.ttf` |

### 文字尺寸规范（`dimens.xml`）

| 名称 | 值 | 用途 |
|------|----|------|
| `text_caption` | 12sp | 辅助说明 |
| `text_body` | 14sp | 正文 |
| `text_body_large` | 16sp | 列表项标题 |
| `text_subtitle` | 20sp | 副标题 |
| `text_title` | 24sp | 页面标题 |

---

## 五、尺寸规范

### 间距（8dp 网格）

| 名称 | 值 |
|------|----|
| `spacing_xs` | 4dp |
| `spacing_sm` | 8dp |
| `spacing_md` | 12dp |
| `spacing_base` | 16dp |
| `spacing_lg` | 24dp |
| `spacing_xl` | 32dp |

### 卡片

| 名称 | 值 |
|------|----|
| `card_radius` | 20dp |
| `card_radius_small` | 16dp |
| `card_radius_large` | 24dp |
| `card_elevation` | 3dp |
| `card_elevation_soft` | 2dp |
| `card_padding` | 16dp |
| `card_stroke_width` | 0.5dp |

### 其他

| 名称 | 值 | 说明 |
|------|----|------|
| `touch_target_min` | 48dp | 最小点击区域 |
| `icon_sm` / `icon_md` / `icon_lg` | 20 / 24 / 32dp | 图标尺寸 |
| `item_padding` | 12dp | 列表项内边距 |
| `item_gap` | 8dp | 列表项间距 |
| `form_field_gap` | 12dp | 表单字段间距 |
| `form_section_gap` | 16dp | 表单分区间距 |

圆角体系共六级递进：6dp → 10dp → 16dp → 20dp → 24dp → 32dp。

---

## 六、图标体系

图标来源：**Phosphor Icons**（MIT 协议），SVG 转 Android Vector Drawable，采用 Regular + Fill 双态设计。约 44 个图标文件。

### 底部导航（3组 × 2态）

| Tab | Regular | Fill |
|-----|---------|------|
| 主页 | `ic_wardrobe` | `ic_wardrobe_fill` |
| 总览 | `ic_dress` | `ic_dress_fill` |
| 我的 | `ic_user` | `ic_user_fill` |

### 裙子状态（6组 × 2态）

| 状态 | Regular | Fill |
|------|---------|------|
| 待抢 | `ic_star` | `ic_star_fill` |
| 付意向 | `ic_heart` | `ic_heart_fill` |
| 付定金 | `ic_money` | `ic_money_fill` |
| 补尾款 | `ic_credit_card` | `ic_credit_card_fill` |
| 待发货 | `ic_package` | `ic_package_fill` |
| 已到手 | `ic_check_circle` | `ic_check_circle_fill` |

### 功能图标

衣柜管理（`ic_plus_circle`、`ic_arrow_up/down`、`ic_trash`、`ic_image`、`ic_pencil`）、详情字段（`ic_tag`、`ic_storefront`、`ic_calendar`、`ic_truck`、`ic_receipt`、`ic_check_square`）、金额显隐（`ic_eye`、`ic_eye_closed`）、数据导入（`ic_copy`、`ic_clipboard`、`ic_sparkle`、`ic_download`、`ic_stack`）、统计设置（`ic_chart_bar`、`ic_chart_pie`、`ic_sun/moon` + Fill、`ic_upload`、`ic_broom`、`ic_gear` + Fill）等。

---

## 七、背景与装饰 Drawable

| 文件 | 形状 | 圆角 | 用途 |
|------|------|------|------|
| `bg_bottom_nav.xml` | rectangle | 上24dp | 底栏背景 |
| `bg_nav_floating.xml` | rectangle | 32dp | 浮动导航面板 |
| `bg_nav_indicator.xml` | rectangle | 16dp | 底栏选中指示器 |
| `bg_nav_panel.xml` | rectangle | 32dp | 底栏面板（深浅适配） |
| `bg_nav_tab_active.xml` | rectangle | 20dp | 激活 Tab 背景 |
| `bg_status_pill.xml` | rectangle | 10dp | 状态药丸标签 |
| `bg_step_number.xml` | oval | — | 步骤序号圆圈 |
| `bg_image_placeholder.xml` | rectangle | 16dp | 图片占位渐变（玫瑰→粉） |
| `bg_event_icon.xml` | oval | — | 事件图标圆形底 |
| `default_wardrobe_cover.xml` | layer-list | 16dp | 衣柜默认封面（三层渐变） |
| `rounded_image_bg.xml` | rectangle | 6dp | 圆角图片背景 |

---

## 八、布局文件清单

### 主框架

| 布局 | 说明 |
|------|------|
| `activity_main.xml` | 主 Activity：Fragment 容器 + 子页面覆盖层 + 8 个 ViewStub + 底部导航 |
| `layout_bottom_nav.xml` | 悬浮胶囊导航（3 Tab + 滑动指示器） |
| `activity_month_schedule.xml` | 月份事件列表页（独立 Activity） |

### 主页（Home Tab）

| 布局 | 说明 |
|------|------|
| `page_my_wardrobes.xml` | 标题 + 催款浮条 + 统计卡片（总数/总花费）+ 衣柜列表 |
| `item_wardrobe.xml` | 衣柜卡片：预览图 + 名称 + 件数/更新时间 + 演示标签 |
| `dialog_add_wardrobe.xml` | 添加衣柜弹窗：名称输入 + 封面选择 |

### 总览页（Overview Tab）

| 布局 | 说明 |
|------|------|
| `page_overview.xml` | 标题 + 3 列网格 RecyclerView |
| `item_overview_dress.xml` | 大图卡片：图片 + 状态药丸 + 置顶标签 + 名称 + 价格 |

### 个人中心（Profile Tab）

| 布局 | 说明 |
|------|------|
| `page_profile.xml` | 用户卡片 + 三统计列 + 设置入口列表 |
| `page_profile_detail.xml` | 头像 + 昵称输入 + 今日标语 + 保存 |
| `page_stats.xml` | 概览卡片 + 近期事件 + 月份时间轴 + 事件类型统计 |
| `page_theme_settings.xml` | 显示模式 RadioGroup + 保存 |
| `page_notifications.xml` | 通知开关 + 提醒时间 + 提前天数 Slider + 保存 |
| `page_data_import.xml` | 三步引导：复制提示词 → 粘贴数据 → 确认导入 |

### 衣柜详情与衣服管理

| 布局 | 说明 |
|------|------|
| `page_wardrobe_detail.xml` | 标题 + 搜索 + ChipGroup 状态过滤 + 衣服列表 + 悬浮添加按钮 |
| `page_dress_detail.xml` | 大图 + 名称 + 状态 + 多列详情 + 时间节点 + 编辑按钮 |
| `page_add_dress_new.xml` | 完整表单：主图 + 状态勾选(联动日期) + 名称 + 价格明细 + 保存 |
| `item_dress_item.xml` | 衣服列表项：缩略图 + 名称 + 详情摘要 |

### 事件与统计列表项

| 布局 | 说明 |
|------|------|
| `item_collection_event.xml` | 事件卡片：图标 + 名称 + 类型 + 金额 + 日期 + 倒计时 |
| `item_month_card.xml` | 月份卡片：月份 + 节点数 + 待支付金额 |
| `item_event_type_tag.xml` | 事件类型标签：名称 + 计数 |
| `item_stats_dress_card.xml` | 统计衣服卡片：缩略图 + 名称 + 价格 + 状态药丸 |

---

## 九、Fragment 与 UI 管理器

### Fragment 层

三个主 Tab 均为 Fragment，通过 `Host` 接口与 Activity 通信，使用 `viewLifecycleOwner` 观察 LiveData。

#### HomeFragment

- 布局：`page_my_wardrobes.xml`
- 职责：衣柜列表展示、统计概览（总数/总花费）、催款 Banner 显示/隐藏（带淡入淡出动画）、总花费显隐（与 ProfileFragment 通过 ViewModel 共享状态）
- 通信：`Host` 接口（`onWardrobeClick` / `onWardrobeLongPress` / `onAddWardrobeClick` / `onTotalCostClick`）
- 观察：`wardrobeList` / `totalDressCount` / `totalCost` / `isTotalCostHidden` / `showHomeBanner` / `urgentFinalPaymentList`

#### OverviewFragment

- 布局：`page_overview.xml`
- 职责：3 列网格展示所有衣服，`onResume` 时重新拉取数据
- 通信：`Host` 接口（`onDressItemClick`）
- 观察：`filteredDressItems`

#### ProfileFragment

- 布局：`page_profile.xml`
- 职责：用户信息展示（头像/昵称/标语）、统计概览、设置入口导航、总花费显隐弹窗
- 通信：`Host` 接口（导航到主题/统计/数据导入/通知/个人资料）
- 特别功能：每日随机标语（`slogan_pool` 数组，按日期缓存）、总花费二次确认弹窗（10 条随机 ka 文案）

### UI 管理器

#### NavigationManager

- 管理子页面覆盖层的显示/隐藏、页面栈、标题栏
- ViewStub 首次 inflate 后回调 `onPageInflated` 通知 Activity 初始化子 View
- `showPage(name, title, showBack, showBottomNav)` 显示子页面
- `navigateBack()` 从页面栈弹出上一页

#### DialogManager

统一管理所有弹窗，全部使用 `MaterialAlertDialogBuilder` + `RoundedAlertDialog` 样式（24dp 圆角）：

| 方法 | 功能 |
|------|------|
| `showTotalCostDialog` | 查看总花费二次确认（10 条随机文案） |
| `showChannelPickerDialog` | 渠道选择（淘宝/拼多多/闲鱼/其他） |
| `showDatePickerDialog` | 日期选择器 |
| `showTimePickerDialog` | 时间选择器（24h） |
| `showAddWardrobeDialog` | 添加衣柜（名称输入 + 封面选择） |
| `showWardrobeContextMenu` | 衣柜长按菜单（删除/上移/下移） |
| `showDeleteWardrobeDialog` | 删除衣柜确认 |
| `showDressItemContextMenu` | 衣服长按菜单（置顶/移动/删除） |
| `showWardrobePickerForImport` | 导入目标衣柜选择 |

#### StatsPageManager

- 管理 `page_stats.xml` 的初始化与数据绑定
- 概览卡片：本月待支付金额（红色）、未来 30 天待办数（橙色）
- 近期事件列表：最多展示 5 条，超出显示"查看更多"
- 月份时间轴：当前月前后各 2 个月共 5 个月，横向滚动，自动定位当前月
- 事件类型统计：2 列网格，可折叠
- 跳转：点击事件 → 裙子详情；点击月份 → MonthScheduleActivity

---

## 十、适配器体系

所有适配器均使用 **ListAdapter + DiffUtil**，无闪烁/错位/越界风险。ViewHolder 在构造时完成 `findViewById` 缓存。

| 适配器 | 文件 | 语言 | 数据类型 | 布局 | 列表样式 |
|--------|------|------|----------|------|----------|
| `WardrobeAdapter` | Java | `Wardrobe` | `item_wardrobe.xml` | 线性列表 + 预览图网格 |
| `DressItemAdapter` | Java | `DressItem` | `item_dress_item.xml` | 线性列表 |
| `OverviewDressAdapter` | Java | `DressItem` | `item_overview_dress.xml` | 3 列网格 |
| `StatsCardAdapter` | Java | `DressItem` | `item_stats_dress_card.xml` | 线性列表 |
| `CollectionEventAdapter` | Kotlin | `CollectionEvent` | `item_collection_event.xml` | 线性列表 |
| `MonthCardAdapter` | Kotlin | `MonthCardData` | `item_month_card.xml` | 横向线性 |
| `EventTypeTagAdapter` | Kotlin | `EventTypeData` | `item_event_type_tag.xml` | 2 列网格 |

---

## 十一、图片加载

`ImageUtils.java` 提供完整的图片加载方案：

- **三级缓存**：LruCache（maxMemory/6）+ 磁盘缓存（50MB，7 天过期）+ 采样解码
- **采样**：`inJustDecodeBounds` + `calculateInSampleSize`，缩略图 320×320
- **配置**：RGB_565（无透明通道图片内存减半）
- **防错位**：`TAG_KEY_LOADING_PATH` 标记 ImageView 当前加载路径，回调时校验
- **防泄漏**：使用 ApplicationContext，检查 `isAttachedToWindow`
- **并发**：双线程池（Thread.MIN_PRIORITY）
- **LruCache 淘汰**：自动 `recycle()` 旧 Bitmap

---

## 十二、交互特性

### 动画

- 催款 Banner 淡入（300ms）/ 淡出（200ms）动画
- 底部导航选中指示器滑动

### 隐私保护

- 总花费默认隐藏（`****`），点击后弹出二次确认弹窗
- 花费显隐状态通过 ViewModel 共享，跨 Tab 同步

### 每日标语

- `slogan_pool` 字符串数组，每日随机选取一条
- 按日期缓存到 SharedPreferences，当天不重复随机

### AI 辅助导入

- 三步引导流程：复制 AI 提示词 → 粘贴 AI 返回数据 → 选择目标衣柜确认导入
- 支持纯文本多格式解析（表格制表符分隔 / 空格分隔 / 自由文本）

### 状态过滤

- 衣柜详情页 ChipGroup 7 个 Chip（全部/待抢/付意向/付定金/补尾款/待发货/已到手）
- 单选过滤，动态隐藏无数据的 Chip

### 深色模式

- 三档切换：浅色 / 深色 / 跟随系统
- 通过 `AppCompatDelegate.setDefaultNightMode` 实现
- 暗色模式纯黑底 + 深灰面板，粉色调保持不变

---

## 十三、字符串资源

`strings.xml` 定义了完整的中文 UI 文案，涵盖应用名、页面标题、状态名称、表单提示、导入流程、通知设置等。关键字符串包括：

- 六种裙子状态：待抢 / 付意向 / 付定金 / 补尾款 / 待发货 / 已到手
- 四种购买渠道：淘宝 / 拼多多 / 闲鱼 / 其他
- 通知设置文案、导入流程文案、主题模式文案
- `slogan_pool` 标语池（每日随机展示）
