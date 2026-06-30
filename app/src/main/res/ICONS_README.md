# Phosphor Icons - LoXia 图标清单

## 底部导航（3个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 衣柜 | `@drawable/ic_wardrobe` / `@drawable/ic_wardrobe_fill` | 底部导航主页 |
| 裙子 | `@drawable/ic_dress` / `@drawable/ic_dress_fill` | 底部导航总览 |
| 用户 | `@drawable/ic_user` / `@drawable/ic_user_fill` | 底部导航个人中心 |

## 衣柜管理（6个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 添加 | `@drawable/ic_plus_circle` | 添加衣柜按钮 |
| 上移 | `@drawable/ic_arrow_up` | 衣柜排序上移 |
| 下移 | `@drawable/ic_arrow_down` | 衣柜排序下移 |
| 删除 | `@drawable/ic_trash` | 删除衣柜/裙子 |
| 图片 | `@drawable/ic_image` | 衣柜封面选择 |
| 编辑 | `@drawable/ic_pencil` | 编辑裙子 |

## 裙子状态（12个）

| 状态 | Regular | Fill | 说明 |
|------|---------|------|------|
| 待抢 | `@drawable/ic_star` | `@drawable/ic_star_fill` | 心愿单 |
| 付意向 | `@drawable/ic_heart` | `@drawable/ic_heart_fill` | 意向金 |
| 付定金 | `@drawable/ic_money` | `@drawable/ic_money_fill` | 定金支付 |
| 补尾款 | `@drawable/ic_credit_card` | `@drawable/ic_credit_card_fill` | 尾款支付 |
| 待发货 | `@drawable/ic_package` | `@drawable/ic_package_fill` | 等待发货 |
| 已到手 | `@drawable/ic_check_circle` | `@drawable/ic_check_circle_fill` | 已收到 |

## 详情字段（6个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 标签 | `@drawable/ic_tag` | 裙子名称 |
| 店铺 | `@drawable/ic_storefront` | 购买店铺 |
| 日历 | `@drawable/ic_calendar` | 购买日期 |
| 运费 | `@drawable/ic_truck` | 运费信息 |
| 收据 | `@drawable/ic_receipt` | 金额信息 |
| 勾选 | `@drawable/ic_check_square` | 全款勾选 |

## 金额显示（2个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 显示 | `@drawable/ic_eye` | 显示总花费 |
| 隐藏 | `@drawable/ic_eye_closed` | 隐藏总花费 |

## 数据导入（5个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 复制 | `@drawable/ic_copy` | 复制提示词 |
| 粘贴 | `@drawable/ic_clipboard` | 粘贴数据 |
| 闪光 | `@drawable/ic_sparkle` | AI 智能导入 |
| 下载 | `@drawable/ic_download` | 导入数据 |
| 堆叠 | `@drawable/ic_stack` | 批量导入 |

## 统计与设置（10个）

| 图标 | 文件 | 用途 |
|------|------|------|
| 柱状图 | `@drawable/ic_chart_bar` | 统计图表 |
| 饼图 | `@drawable/ic_chart_pie` | 统计图表 |
| 太阳 | `@drawable/ic_sun` / `@drawable/ic_sun_fill` | 浅色模式 |
| 月亮 | `@drawable/ic_moon` / `@drawable/ic_moon_fill` | 深色模式 |
| 上传 | `@drawable/ic_upload` | 数据导出 |
| 扫帚 | `@drawable/ic_broom` | 缓存清理 |
| 齿轮 | `@drawable/ic_gear` / `@drawable/ic_gear_fill` | 设置 |

---

## 使用方式

### XML 布局中使用
```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_dress"
    android:tint="@color/pink_primary" />
```

### 代码中使用
```java
ImageView icon = findViewById(R.id.icon);
icon.setImageResource(R.drawable.ic_dress);
icon.setColorFilter(ContextCompat.getColor(this, R.color.pink_primary));
```

### 深浅模式适配
在 `values/colors.xml` 和 `values-night/colors.xml` 中定义不同的颜色：
```xml
<!-- values/colors.xml -->
<color name="icon_tint">#F58FB2</color>

<!-- values-night/colors.xml -->
<color name="icon_tint">#FFB6C1</color>
```

---

## 图标来源

- **图标库**: Phosphor Icons (https://phosphoricons.com/)
- **样式**: Regular (默认) + Fill (选中)
- **格式**: SVG 转 Android Vector Drawable
- **授权**: MIT License
