package com.cy.loxia;

import androidx.annotation.NonNull;
import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.HapticFeedbackConstants;

import com.yalantis.ucrop.UCrop;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.cy.loxia.ui.ProfileFragment;
import com.cy.loxia.ui.HomeFragment;
import com.cy.loxia.ui.OverviewFragment;

public class MainActivity extends AppCompatActivity implements ProfileFragment.Host, HomeFragment.Host, OverviewFragment.Host {
    // 单线程 IO 执行器，用于后台任务（避免内存泄漏）
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private static final String[] STATUS_OPTIONS = {"待抢", "付意向", "付定金", "补尾款", "待发货", "已到手"};
    private static final String PREFS_NAME = "loxia_prefs";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_RESTORE_NAV = "restore_nav";
    private static final String KEY_PROFILE_AVATAR = "profile_avatar_uri";
    private static final String KEY_PROFILE_NICKNAME = "profile_nickname";
    private static final String KEY_SLOGAN_DATE = "slogan_date";
    private static final String KEY_SLOGAN_TEXT = "slogan_text";
    private static final String FORMAT_PRICE = "¥%.2f";
    private static final String DEFAULT_SHIPPING = "包邮";
    private static final String DIALOG_TITLE_COST = "查看总花费";
    private static final String MIME_IMAGE = "image/*";

    // Theme modes
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";
    private static final String THEME_SYSTEM = "system";

    // Notification preference keys
    private static final String KEY_NOTIF_TIME_HOUR = "notif_time_hour";
    private static final String KEY_NOTIF_TIME_MINUTE = "notif_time_minute";
    private static final String KEY_NOTIF_ADVANCE_DAYS = "notif_advance_days";
    private static final String KEY_NOTIF_DAIQIANG = "notif_advance_daiqiang";
    private static final String KEY_NOTIF_YIXIANG = "notif_advance_yixiang";
    private static final String KEY_NOTIF_DINGJIN = "notif_advance_dingjin";
    private static final String KEY_NOTIF_BUWEIKUAN = "notif_advance_buweikuan";
    // Pages
    private View bottomNavigation;
    private View navHome, navOverview, navProfile;
    private View navIndicator;
    private ImageView ivNavHome, ivNavOverview, ivNavProfile;
    private TextView tvNavHome, tvNavOverview, tvNavProfile;

    // Wardrobe list

    private RecyclerView rvOverview;
    // Detail page
    private RecyclerView rvDressItems;
    private TextInputEditText etSearch;
    private View searchContainer;
    private ChipGroup chipGroup;
    private MaterialButton btnAddDress;

    // Add dress page
    private ImageView ivMainImagePreview;
    private View layoutUploadPlaceholder;
    // Status checkboxes
    private com.google.android.material.chip.Chip cbDaiQiang, cbFuYixiang, cbFuDingjin, cbBuWeikuan, cbDaiFahuo, cbDaoShou;
    // Per-status date fields
    private TextInputLayout layoutDaiqiangDate, layoutYixiangDate, layoutDingjinDate, layoutBuweikuanDate;
    private TextInputEditText etDaiqiangDate, etYixiangDate, etDingjinDate, etBuweikuanDate;
    private TextInputEditText etNameAdd;
    private TextInputEditText etStoreAdd;
    private TextInputEditText etChannelAdd;
    private TextInputEditText etBuyDateAdd;
    private TextInputEditText etEarnestMoneyAdd;
    private TextInputEditText etShippingFeeAdd;
    private com.google.android.material.materialswitch.MaterialSwitch cbFullPayment;
    private TextInputLayout layoutFullPaymentAmount;
    private TextInputEditText etFullPaymentAmountAdd;
    private TextInputLayout layoutTailPayment;
    private TextInputEditText etTailPaymentAdd;
    private TextInputLayout layoutDepositAdd;
    private TextInputEditText etDepositAdd;
    private MaterialButton btnSaveAddDress;
    private Uri pendingMainImageUri;

    // Dress detail page
    private ImageView ivDressDetailImage;
    private TextView tvDressDetailName;
    private TextView tvDressDetailWardrobeName;
    private TextView tvDressDetailStatus;
    private TextView tvDressDetailStore;
    private TextView tvDressDetailChannel;
    private TextView tvDressDetailBuyDate;
    private TextView tvDressDetailPrice;
    private TextView tvDressDetailEarnest;
    private TextView tvDressDetailFullPayment;
    private TextView tvDressDetailTailPayment;
    private TextView tvDressDetailTailDate;
    private TextView tvDressDetailShipping;
    private TextView tvDressDetailDeposit;
    private TextView tvDressDetailRemark;
    private LinearLayout layoutDressDetailTimeNodes;
    private MaterialButton btnEditDress;


    // Profile detail
    private ImageView ivProfileAvatar;
    private TextInputEditText etProfileName;
    private TextView tvSloganPreview;
    private Uri pendingAvatarUri;
    private ActivityResultLauncher<Intent> avatarPickerLauncher;

    // Data import
    private TextInputEditText etImportText;
    private TextView tvImportPreview;
    private View cardImportPreview;
    private TextView tvImportWardrobeLabel;
    private List<DressItem> pendingImportItems;
    private String selectedImportWardrobeId;

    // Notifications (简化版)
    private SwitchMaterial swNotificationEnabled;
    private MaterialButton btnReminderTime;
    private int notifTimeHour = 8, notifTimeMinute = 0;
    private Slider sliderAdvanceDays;
    private TextView tvAdvanceDays;

    // Theme settings
    private RadioGroup rgThemeMode;


    // Stats section RecyclerViews and adapters

    // UI Managers
    private com.cy.loxia.ui.DialogManager dialogManager;
    private com.cy.loxia.ui.NavigationManager navigationManager; // sub-page navigation (detail/add_dress/etc)
    private com.cy.loxia.ui.StatsPageManager statsPageManager;

    private DataRepository repository;
    private MainViewModel viewModel;
    private DressItemAdapter dressItemAdapter;
    private Wardrobe currentWardrobe;
    private DressItem currentDressItem;
    private List<Wardrobe> wardrobes = new ArrayList<>();
    private int currentMainNavId = R.id.nav_detail;
    private boolean isFromOverview = false;
    private boolean isFromStats = false;

    // Edit form change tracking
    private String editEntryName, editEntryStore, editEntryChannel, editEntryBuyDate, editEntryStatus;
    private String editEntryShippingFee, editEntryEarnest, editEntryFullPayment, editEntryTailPayment, editEntryDeposit;
    private String editEntryDaiqiangDate, editEntryYixiangDate, editEntryDingjinDate, editEntryBuweikuanDate;
    private boolean editEntryIsFull;
    private Uri editEntryImageUri;

    // Image pickers
    private Uri pendingWardrobeCover;
    private ImageView dialogCoverPreview;
    private ActivityResultLauncher<Intent> wardrobeCoverPickerLauncher;
    private ActivityResultLauncher<Intent> mainImagePickerLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private Uri pendingCropTargetUri;
    private int pendingCropType; // 0=avatar, 1=cover, 2=mainImage

    private void applyThemeMode(String mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String themeMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("theme_mode", THEME_SYSTEM);
        applyThemeMode(themeMode);

        super.onCreate(savedInstanceState);
        // 初始化图片磁盘缓存
        ImageUtils.initDiskCache(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = DataRepository.getInstance(this);
        pendingWardrobeCover = null;
        pendingMainImageUri = null;

        wardrobeCoverPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        pendingCropType = 1;
                        pendingCropTargetUri = uri;
                        handleCroppedCover();
                    }
                }
            }
        );

        mainImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        pendingCropType = 2;
                        pendingCropTargetUri = uri;
                        handleCroppedMainImage();
                    }
                }
            }
        );

        avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        pendingCropType = 0;
                        pendingCropTargetUri = uri;
                        handleCroppedAvatar();
                    }
                }
            }
        );

        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (!granted) {
                    // 用户拒绝了通知权限，静默降级即可，不做额外提示
                    // 通知调度仍会执行，只是通知不会显示
                }
            }
        );

        cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        // 保存 URI 到局部变量，避免后台线程访问时被清空
                        Uri cropUri = resultUri;
                        int cropType = pendingCropType;
                        pendingCropTargetUri = cropUri;

                        switch (cropType) {
                            case 0: handleCroppedAvatar(); break;
                            case 1: handleCroppedCover(); break;
                            case 2: handleCroppedMainImage(); break;
                        }
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable error = UCrop.getError(result.getData());
                    if (error != null) {
                        Toast.makeText(this, "裁剪失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        initViews();
        initListAdapters();
        initActions();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        initUIManagers();

        // 观察数据层错误事件，显示友好提示
        viewModel.getErrorEvent().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                viewModel.clearErrorEvent();
            }
        });

        viewModel.getWardrobeList().observe(this, wardrobeList -> {
            if (wardrobeList == null) return;
            // ViewModel 已转换为 Domain Model，直接使用
            wardrobes = new ArrayList<>(wardrobeList);
            refreshPreviewImages();
            if (currentWardrobe == null && !wardrobes.isEmpty()) {
                currentWardrobe = wardrobes.get(0);
            }
            updateDetailPage();
        });

        // 观察统计数据
        viewModel.getTotalDressCount().observe(this, count -> {
            if (count == null) return;
        });

        viewModel.getTotalCost().observe(this, cost -> {
            if (cost == null) return;
            String formattedCost = String.format(FORMAT_PRICE, cost);
        });

        viewModel.getThisMonthItemCount().observe(this, count -> {
            if (count == null) return;
        });

        // 催款提示条：合并观察，避免嵌套 observe 导致泄漏
        viewModel.getUrgentFinalPaymentList().observe(this, urgentList -> {
            com.google.android.material.card.MaterialCardView bannerFinalPayment =
                    findViewById(R.id.bannerFinalPayment);
            if (bannerFinalPayment == null) return;

            // 如果浮条已被用户操作过（关闭或自动隐藏），直接隐藏
            if (viewModel.hasBannerBeenActioned()) {
                bannerFinalPayment.setVisibility(View.GONE);
                return;
            }

            Boolean showBanner = viewModel.getShowHomeBanner().getValue();
            if (showBanner == null || !showBanner) {
                bannerFinalPayment.setVisibility(View.GONE);
                return;
            }

            if (urgentList == null) return;
            int count = urgentList.size();
            if (count > 0) {
                TextView tvBannerMessage = bannerFinalPayment.findViewById(R.id.tvBannerMessage);
                if (tvBannerMessage != null) {
                    tvBannerMessage.setText(String.format(
                            "提示：有 %d 条裙子正在等待补尾款，快去看看吧！", count));
                }
                // 淡入显示
                if (bannerFinalPayment.getVisibility() != View.VISIBLE) {
                    bannerFinalPayment.setAlpha(0f);
                    bannerFinalPayment.setScaleX(0.95f);
                    bannerFinalPayment.setScaleY(0.95f);
                    bannerFinalPayment.setVisibility(View.VISIBLE);
                    bannerFinalPayment.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                            .start();
                }
                // 自动隐藏8秒后淡出
                bannerFinalPayment.postDelayed(() -> {
                    if (bannerFinalPayment.getVisibility() == View.VISIBLE) {
                                bannerFinalPayment.animate()
                                        .alpha(0f)
                                        .scaleX(0.95f)
                                        .scaleY(0.95f)
                                        .setDuration(200)
                                        .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
                                .withEndAction(() -> {
                                    bannerFinalPayment.setVisibility(View.GONE);
                                    viewModel.markBannerAsActioned();
                                })
                                .start();
                    }
                }, 8000);
            } else {
                // 无催款时隐藏
                bannerFinalPayment.setVisibility(View.GONE);
            }

            // 点击浮条跳转到统计中心
            bannerFinalPayment.setOnClickListener(v -> onProfileNavigateToStats());

            // 关闭按钮：点击隐藏浮条
            View ivBannerClose = bannerFinalPayment.findViewById(R.id.ivBannerClose);
            if (ivBannerClose != null) {
                ivBannerClose.setOnClickListener(v -> {
                    bannerFinalPayment.animate()
                            .alpha(0f)
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
                            .withEndAction(() -> {
                                bannerFinalPayment.setVisibility(View.GONE);
                                viewModel.markBannerAsActioned();
                            })
                            .start();
                });
            }
        });

        // 缓存 SharedPreferences，避免重复 getSharedPreferences() 调用
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean restoreNav = prefs.getBoolean(KEY_RESTORE_NAV, false);
        if (restoreNav) {
            prefs.edit().putBoolean(KEY_RESTORE_NAV, false).apply();
            int savedNavId = prefs.getInt("last_main_nav", R.id.nav_detail);
            currentMainNavId = savedNavId;
        } else {
            currentMainNavId = R.id.nav_detail;
        }
        updateNavSelection();

        // 首帧绘制完成后再加载数据，不阻塞界面展示
        mainView.post(() -> {
            loadData();
            // 使用 IO 线程池处理耗时任务
            ioExecutor.execute(() -> {
                migrateExistingImages();
                AlarmScheduler.createNotificationChannel(MainActivity.this);
                boolean notifEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
                if (notifEnabled) {
                    AlarmScheduler.scheduleAllPendingReminders(MainActivity.this, repository);
                }
            });
        });
        requestNotificationPermission();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        updateNavSelection();
    }

    private void initViews() {
        // Main content views are now managed by individual Fragments
    }

    private void initUIManagers() {
        dialogManager = new com.cy.loxia.ui.DialogManager(this, getLayoutInflater());
        statsPageManager = new com.cy.loxia.ui.StatsPageManager(this, viewModel, repository);

        // Sub-page NavigationManager for detail/add/edit pages (ViewStub-based)
        View bottomNavView = findViewById(R.id.bottomNavigation);
        View subPageContainer = findViewById(R.id.sub_page_container);
        View mainContentContainer = findViewById(R.id.main_content_container);
        com.google.android.material.appbar.MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        java.util.Map<String, View> subPages = new java.util.HashMap<>();
        subPages.put("page_detail", findViewById(R.id.stub_detail));
        subPages.put("page_add_dress", findViewById(R.id.stub_add_dress));
        subPages.put("page_dress_detail", findViewById(R.id.stub_dress_detail));
        subPages.put("page_profile_detail", findViewById(R.id.stub_profile_detail));
        subPages.put("page_stats", findViewById(R.id.stub_stats));
        subPages.put("page_theme_settings", findViewById(R.id.stub_theme_settings));
        subPages.put("page_notifications", findViewById(R.id.stub_notifications));
        subPages.put("page_data_import", findViewById(R.id.stub_data_import));

        navigationManager = new com.cy.loxia.ui.NavigationManager(
            topAppBar, subPageContainer, mainContentContainer, bottomNavView, subPages, () -> onBackPressed()
        );

        navigationManager.setOnPageInflated((pageName, pageView) -> {
            switch (pageName) {
                case "page_detail": initDetailPage(pageView); break;
                case "page_add_dress": initAddDressPage(pageView); break;
                case "page_profile_detail": initProfileDetailPage(pageView); break;
                case "page_dress_detail": initDressDetailPage(pageView); break;
                case "page_stats": initStatsPage(pageView); break;
                case "page_theme_settings": initThemeSettingsPage(pageView); break;
                case "page_notifications": initNotificationsPage(pageView); break;
                case "page_data_import": initDataImportPage(pageView); break;
            }
        });

        // Initialize Fragment-based tab navigation
        if (getSupportFragmentManager().findFragmentById(R.id.main_content_container) == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_content_container, new HomeFragment(), "home")
                .commit();
        }
    }

    private void initListAdapters() {
        // Wardrobe list is now managed by HomeFragment
    }

    private StatsCardAdapter setupStatsRecyclerView(RecyclerView rv) {
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(10);
        StatsCardAdapter adapter = new StatsCardAdapter();
        adapter.setOnItemClickListener(item -> {
            isFromStats = true;
            navigateToDressDetail(item);
        });
        rv.setAdapter(adapter);
        return adapter;
    }

    private void hideAllPages() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        if (root != null) {
            android.transition.Transition transition = new android.transition.Fade();
            transition.setDuration(200);
            android.transition.TransitionManager.beginDelayedTransition(root, transition);
        }
        navigationManager.hideAllPages();
        findViewById(R.id.sub_page_container).setVisibility(View.GONE);
    }

    /** Restore main fragment container and bottom nav after returning from sub-pages */
    private void restoreMainContent() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        if (root != null) {
            android.transition.Transition transition = new android.transition.Fade();
            transition.setDuration(200);
            android.transition.TransitionManager.beginDelayedTransition(root, transition);
        }
        findViewById(R.id.main_content_container).setVisibility(View.VISIBLE);
        bottomNavigation.setVisibility(View.VISIBLE);
    }

    private void navigateToDetail() {
        String title = currentWardrobe != null ? currentWardrobe.getName() : getString(R.string.title_detail);
        navigationManager.showPage("page_detail", title, true, false);
        if (searchContainer != null) searchContainer.setVisibility(View.GONE);
        if (etSearch != null) etSearch.setText("");
    }

    private void navigateBackFromDetail() {
        hideAllPages();
        restoreMainContent();
        currentMainNavId = R.id.nav_detail;
        updateNavSelection();
        if (searchContainer != null) searchContainer.setVisibility(View.GONE);
        if (chipGroup != null) chipGroup.check(R.id.chipAll);
    }

    private void navigateToAddDress() {
        currentDressItem = null;
        navigationManager.showPage("page_add_dress", getString(R.string.title_add), true, false);
        clearAddDressForm();
    }

    private void navigateBackFromAddDress() {
        if (etNameAdd == null) { exitAddDressPage(); return; }
        boolean hasImage = pendingMainImageUri != null;
        String name = etNameAdd.getText() != null ? etNameAdd.getText().toString().trim() : "";
        String status = getCheckedStatusString();
        boolean isEdit = currentDressItem != null;

        if (!isEdit && (hasImage || !name.isEmpty() || !status.isEmpty())) {
            new AlertDialog.Builder(this)
                .setTitle("放弃编辑？")
                .setMessage("当前填写的内容将不会被保存，确定要退出吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    clearAddDressForm();
                    exitAddDressPage();
                })
                .setNegativeButton("继续编辑", null)
                .show();
        } else {
            clearAddDressForm();
            exitAddDressPage();
        }
    }

    private void exitAddDressPage() {
        navigationManager.showPage("page_detail", getString(R.string.title_detail), true, false);
    }

    private void navigateToDressDetail(DressItem item) {
        currentDressItem = item;
        // isFromOverview / isFromStats 由调用方在回调中设置
        navigationManager.showPage("page_dress_detail", "裙子详情", true, false);
        updateDressDetailPage();
    }

    /**
     * 通过 ID 跳转到裙子详情（供 StatsPageManager 调用）
     */
    public void navigateToDressDetail(String dressId) {
        // 从缓存数据中查找
        List<DressItem> items = viewModel.getFilteredDressItems().getValue();
        if (items != null) {
            for (DressItem item : items) {
                if (item.getId().equals(dressId)) {
                    navigateToDressDetail(item);
                    return;
                }
            }
        }
        // 如果缓存中没有，通过 ViewModel 异步查找
        viewModel.launchFindDressItemById(dressId, item -> {
            if (item != null) {
                runOnUiThread(() -> navigateToDressDetail(item));
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private void navigateBackFromDressDetail() {
        if (isFromOverview) {
            hideAllPages();
            restoreMainContent();
            showPage(R.id.nav_overview);
            currentMainNavId = R.id.nav_overview;
        } else if (isFromStats) {
            hideAllPages();
            restoreMainContent();
            showPage(R.id.nav_profile);
            currentMainNavId = R.id.nav_profile;
            navigateToStats();
        } else if (currentWardrobe != null) {
            navigationManager.showPage("page_detail", getString(R.string.title_detail), true, false);
            // 保持在详情页面，不更新主导航状态
        } else {
            hideAllPages();
            restoreMainContent();
            currentMainNavId = R.id.nav_detail;
        }
        updateNavSelection();
        currentDressItem = null;
        isFromOverview = false;
        isFromStats = false;
    }

    private void updateDressDetailPage() {
        if (currentDressItem == null) return;
        if (tvDressDetailName == null) return; // page not yet inflated
        DressItem item = currentDressItem;

        ImageUtils.loadIntoView(this, item.getImageUri(), ivDressDetailImage, R.drawable.bg_image_placeholder);

        tvDressDetailName.setText(item.getName());

        if (isFromOverview) {
            viewModel.launchFindWardrobeById(item.getWardrobeId(), wardrobe -> {
                runOnUiThread(() -> {
                    if (wardrobe != null) {
                        tvDressDetailWardrobeName.setText(wardrobe.getName());
                        tvDressDetailWardrobeName.setVisibility(View.VISIBLE);
                    } else {
                        tvDressDetailWardrobeName.setVisibility(View.GONE);
                    }
                });
                return kotlin.Unit.INSTANCE;
            });
        } else {
            tvDressDetailWardrobeName.setVisibility(View.GONE);
        }

        String status = (item.getStatus() != null && !item.getStatus().isEmpty()) ? item.getStatus() : "无状态";
        tvDressDetailStatus.setText(status);
        int bgTint = getStatusColorForDetail(status);
        tvDressDetailStatus.setBackgroundTintList(ColorStateList.valueOf(bgTint));
        tvDressDetailStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvDressDetailStatus.setPadding(14, 5, 14, 5);
        // Text color: pure white for dark mode (not R.color.white which is overridden in night), dark for light mode
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            tvDressDetailStatus.setTextColor(Color.WHITE);
        } else {
            tvDressDetailStatus.setTextColor(Color.BLACK);
        }

        String store = item.getStore();
        tvDressDetailStore.setText(store != null && !store.isEmpty() ? store : "-");
        String channel = item.getChannel();
        tvDressDetailChannel.setText(channel != null && !channel.isEmpty() ? channel : "-");
        String buyDate = item.getBuyDate();
        tvDressDetailBuyDate.setText(buyDate != null && !buyDate.isEmpty() ? buyDate : "-");
        tvDressDetailPrice.setText(String.format(FORMAT_PRICE, item.getEffectiveTotal()));

        tvDressDetailEarnest.setText(item.getEarnestMoney() > 0 ? String.format(FORMAT_PRICE, item.getEarnestMoney()) : "-");
        tvDressDetailShipping.setText(getShippingFeeText(item));

        if (item.isFullPayment()) {
            tvDressDetailFullPayment.setText(String.format(FORMAT_PRICE, item.getFullPaymentAmount()));
            findViewById(R.id.layoutDressDetailFullPayment).setVisibility(View.VISIBLE);
            findViewById(R.id.layoutDressDetailTailPayment).setVisibility(View.GONE);
            findViewById(R.id.layoutDressDetailDeposit).setVisibility(View.GONE);
            tvDressDetailDeposit.setText("-");
        } else {
            findViewById(R.id.layoutDressDetailFullPayment).setVisibility(View.GONE);
            findViewById(R.id.layoutDressDetailTailPayment).setVisibility(View.VISIBLE);
            findViewById(R.id.layoutDressDetailDeposit).setVisibility(View.VISIBLE);
            tvDressDetailTailPayment.setText(item.getTailPayment() > 0 ? String.format(FORMAT_PRICE, item.getTailPayment()) : "-");
            tvDressDetailTailDate.setText(item.getTailPaymentDate() != null && !item.getTailPaymentDate().isEmpty() ? item.getTailPaymentDate() : "-");
            tvDressDetailDeposit.setText(getDepositText(item));
        }
        tvDressDetailRemark.setText(item.getRemark() != null && !item.getRemark().isEmpty() ? item.getRemark() : "-");

        // Populate time nodes
        layoutDressDetailTimeNodes.removeAllViews();
        addTimeNodeRow("待抢时间", item.getDaiqiangDate(), R.color.warning_orange);
        addTimeNodeRow("付意向时间", item.getYixiangDate(), R.color.accent_purple);
        addTimeNodeRow("付定金时间", item.getDingjinDate(), R.color.accent_purple);
        addTimeNodeRow("补尾款时间", item.getBuweikuanDate(), R.color.danger_red);
        layoutDressDetailTimeNodes.setVisibility(layoutDressDetailTimeNodes.getChildCount() > 0 ? View.VISIBLE : View.GONE);

        btnEditDress.setOnClickListener(v -> navigateToEditDress());
    }

    private void addTimeNodeRow(String label, String date, int colorResId) {
        if (date == null || date.isEmpty()) return;
        int dotColor = ContextCompat.getColor(this, colorResId);
        float density = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (2 * density), 0, (int) (2 * density));

        // Colored dot
        View dot = new View(this);
        int dotSize = (int) (8 * density);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotLp.rightMargin = (int) (6 * density);
        dot.setLayoutParams(dotLp);
        dot.setBackgroundTintList(ColorStateList.valueOf(dotColor));

        TextView labelTv = new TextView(this);
        labelTv.setText(label + "→");
        labelTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        TextView dateTv = new TextView(this);
        dateTv.setText(date);
        dateTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        dateTv.setPadding((int) (2 * density), 0, 0, 0);

        row.addView(dot);
        row.addView(labelTv);
        row.addView(dateTv);
        layoutDressDetailTimeNodes.addView(row);
    }

    private String getShippingFeeText(DressItem item) {
        String sf = item.getShippingFee();
        return (sf != null && !sf.isEmpty()) ? sf : DEFAULT_SHIPPING;
    }

    private String getDepositText(DressItem item) {
        return item.getDeposit() > 0 ? String.format(FORMAT_PRICE, item.getDeposit()) : "-";
    }

    private int getStatusColorForDetail(String status) {
        if (status.contains("已到手")) return ContextCompat.getColor(this, R.color.success_green);
        if (status.contains("待抢") || status.contains("待发货")) return ContextCompat.getColor(this, R.color.warning_orange);
        if (status.contains("付定金") || status.contains("付意向")) return ContextCompat.getColor(this, R.color.accent_purple);
        if (status.contains("补尾款")) return ContextCompat.getColor(this, R.color.danger_red);
        return ContextCompat.getColor(this, R.color.pink_secondary);
    }

    private void navigateToEditDress() {
        if (currentDressItem == null) return;
        DressItem item = currentDressItem;
        // Capture entry state for change detection
        editEntryName = item.getName();
        editEntryStore = item.getStore();
        editEntryChannel = item.getChannel();
        editEntryBuyDate = item.getBuyDate();
        editEntryStatus = item.getStatus();
        editEntryShippingFee = item.getShippingFee();
        editEntryEarnest = item.getEarnestMoney() > 0 ? String.valueOf(item.getEarnestMoney()) : "";
        editEntryFullPayment = item.getFullPaymentAmount() > 0 ? String.valueOf(item.getFullPaymentAmount()) : "";
        editEntryTailPayment = item.getTailPayment() > 0 ? String.valueOf(item.getTailPayment()) : "";
        editEntryDeposit = item.getDeposit() > 0 ? String.valueOf(item.getDeposit()) : "";
        editEntryIsFull = item.isFullPayment();
        editEntryImageUri = item.getImageUri() != null && !item.getImageUri().isEmpty() ? Uri.parse(item.getImageUri()) : null;
        editEntryDaiqiangDate = item.getDaiqiangDate() != null ? item.getDaiqiangDate() : "";
        editEntryYixiangDate = item.getYixiangDate() != null ? item.getYixiangDate() : "";
        editEntryDingjinDate = item.getDingjinDate() != null ? item.getDingjinDate() : "";
        editEntryBuweikuanDate = item.getBuweikuanDate() != null ? item.getBuweikuanDate() : "";

        // Navigate to add dress page in edit mode (inflate ViewStub first)
        navigationManager.showPage("page_add_dress", "编辑裙子", true, false);
        // Pre-fill the add dress form with existing values
        if (ivMainImagePreview == null || cbDaiQiang == null || etNameAdd == null) return;

        if (item.getImageUri() != null && !item.getImageUri().isEmpty()) {
            pendingMainImageUri = Uri.parse(item.getImageUri());
            Bitmap bitmap = ImageUtils.decodeSampledBitmap(item.getImageUri(), 800, 1066);
            if (bitmap == null) {
                try (InputStream inputStream = getContentResolver().openInputStream(pendingMainImageUri)) {
                    if (inputStream != null) {
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (Exception e) { // image stream may be unavailable
                }
            }
            if (bitmap != null) {
                ivMainImagePreview.setImageBitmap(bitmap);
                ivMainImagePreview.setVisibility(View.VISIBLE);
                layoutUploadPlaceholder.setVisibility(View.GONE);
            }
        }
        // Parse status string to checkboxes
        String status = item.getStatus() != null ? item.getStatus() : "";
        cbDaiQiang.setChecked(status.contains("待抢"));
        cbFuYixiang.setChecked(status.contains("付意向"));
        cbFuDingjin.setChecked(status.contains("付定金"));
        cbBuWeikuan.setChecked(status.contains("补尾款"));
        cbDaiFahuo.setChecked(status.contains("待发货"));
        cbDaoShou.setChecked(status.contains("已到手"));

        // Show/hide per-status date fields
        layoutDaiqiangDate.setVisibility(cbDaiQiang.isChecked() ? View.VISIBLE : View.GONE);
        layoutYixiangDate.setVisibility(cbFuYixiang.isChecked() ? View.VISIBLE : View.GONE);
        layoutDingjinDate.setVisibility(cbFuDingjin.isChecked() ? View.VISIBLE : View.GONE);
        layoutBuweikuanDate.setVisibility(cbBuWeikuan.isChecked() ? View.VISIBLE : View.GONE);

        etDaiqiangDate.setText(item.getDaiqiangDate() != null ? item.getDaiqiangDate() : "");
        etYixiangDate.setText(item.getYixiangDate() != null ? item.getYixiangDate() : "");
        etDingjinDate.setText(item.getDingjinDate() != null ? item.getDingjinDate() : "");
        etBuweikuanDate.setText(item.getBuweikuanDate() != null ? item.getBuweikuanDate() : "");

        etNameAdd.setText(item.getName());
        etStoreAdd.setText(item.getStore());
        etChannelAdd.setText(item.getChannel());
        etBuyDateAdd.setText(item.getBuyDate());
        etShippingFeeAdd.setText(item.getShippingFee());
        etEarnestMoneyAdd.setText(item.getEarnestMoney() > 0 ? String.valueOf(item.getEarnestMoney()) : "");
        etDepositAdd.setText(item.getDeposit() > 0 ? String.valueOf(item.getDeposit()) : "");
        cbFullPayment.setChecked(item.isFullPayment());
        etFullPaymentAmountAdd.setText(item.getFullPaymentAmount() > 0 ? String.valueOf(item.getFullPaymentAmount()) : "");
        etTailPaymentAdd.setText(item.getTailPayment() > 0 ? String.valueOf(item.getTailPayment()) : "");

        if (item.isFullPayment()) {
            layoutDepositAdd.setVisibility(View.GONE);
            layoutTailPayment.setVisibility(View.GONE);
            layoutFullPaymentAmount.setVisibility(View.VISIBLE);
        } else {
            layoutDepositAdd.setVisibility(View.VISIBLE);
            layoutTailPayment.setVisibility(View.VISIBLE);
            layoutFullPaymentAmount.setVisibility(View.GONE);
        }
    }

    private void navigateBackFromEditDress() {
        if (hasEditChanges()) {
            new AlertDialog.Builder(this)
                .setTitle("保存更改？")
                .setMessage("已修改了裙子信息，是否保存更改？")
                .setPositiveButton("保存", (dialog, which) -> {
                    saveAddDressItem();
                })
                .setNegativeButton("丢弃", (dialog, which) -> {
                    clearAddDressForm();
                    navigationManager.showPage("page_dress_detail", "裙子详情", true, false);
                })
                .setCancelable(true)
                .show();
        } else {
            clearAddDressForm();
            navigationManager.showPage("page_dress_detail", "裙子详情", true, false);
        }
    }

    private boolean hasEditChanges() {
        if (!nullSafeEquals(editEntryName, etNameAdd.getText())) return true;
        if (!nullSafeEquals(editEntryStore, etStoreAdd.getText())) return true;
        if (!nullSafeEquals(editEntryChannel, etChannelAdd.getText())) return true;
        if (!nullSafeEquals(editEntryBuyDate, etBuyDateAdd.getText())) return true;
        if (!nullSafeEquals(editEntryStatus, getCheckedStatusString())) return true;
        if (!nullSafeEquals(editEntryShippingFee, etShippingFeeAdd.getText())) return true;
        if (!nullSafeEquals(editEntryEarnest, etEarnestMoneyAdd.getText())) return true;
        if (!nullSafeEquals(editEntryDeposit, etDepositAdd.getText())) return true;
        if (!nullSafeEquals(editEntryFullPayment, etFullPaymentAmountAdd.getText())) return true;
        if (!nullSafeEquals(editEntryTailPayment, etTailPaymentAdd.getText())) return true;
        if (!nullSafeEquals(editEntryDaiqiangDate, etDaiqiangDate.getText())) return true;
        if (!nullSafeEquals(editEntryYixiangDate, etYixiangDate.getText())) return true;
        if (!nullSafeEquals(editEntryDingjinDate, etDingjinDate.getText())) return true;
        if (!nullSafeEquals(editEntryBuweikuanDate, etBuweikuanDate.getText())) return true;
        if (editEntryIsFull != cbFullPayment.isChecked()) return true;
        if (!TextUtils.equals(
                editEntryImageUri != null ? editEntryImageUri.toString() : null,
                pendingMainImageUri != null ? pendingMainImageUri.toString() : null)) return true;
        return false;
    }

    private String getCheckedStatusString() {
        if (cbDaiQiang == null) return "";
        StringBuilder sb = new StringBuilder();
        if (cbDaiQiang.isChecked()) appendStatus(sb, "待抢");
        if (cbFuYixiang.isChecked()) appendStatus(sb, "付意向");
        if (cbFuDingjin.isChecked()) appendStatus(sb, "付定金");
        if (cbBuWeikuan.isChecked()) appendStatus(sb, "补尾款");
        if (cbDaiFahuo.isChecked()) appendStatus(sb, "待发货");
        if (cbDaoShou.isChecked()) appendStatus(sb, "已到手");
        return sb.toString();
    }

    private void appendStatus(StringBuilder sb, String status) {
        if (sb.length() > 0) sb.append("/");
        sb.append(status);
    }

    private boolean nullSafeEquals(String a, CharSequence b) {
        String bStr = b != null ? b.toString().trim() : "";
        String aStr = a != null ? a.trim() : "";
        return aStr.equals(bStr);
    }

    private void initActions() {
        // 初始化自定义导航栏
        bottomNavigation = findViewById(R.id.bottomNavigation);
        navHome = bottomNavigation.findViewById(R.id.nav_home);
        navOverview = bottomNavigation.findViewById(R.id.nav_overview);
        navProfile = bottomNavigation.findViewById(R.id.nav_profile);
        navIndicator = bottomNavigation.findViewById(R.id.navIndicator);
        ivNavHome = bottomNavigation.findViewById(R.id.ivNavHome);
        ivNavOverview = bottomNavigation.findViewById(R.id.ivNavOverview);
        ivNavProfile = bottomNavigation.findViewById(R.id.ivNavProfile);
        tvNavHome = bottomNavigation.findViewById(R.id.tvNavHome);
        tvNavOverview = bottomNavigation.findViewById(R.id.tvNavOverview);
        tvNavProfile = bottomNavigation.findViewById(R.id.tvNavProfile);

        // 设置导航点击事件
        navHome.setOnClickListener(v -> showPage(R.id.nav_detail));
        navOverview.setOnClickListener(v -> showPage(R.id.nav_overview));
        navProfile.setOnClickListener(v -> showPage(R.id.nav_profile));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String currentPage = navigationManager.getCurrentPage();
                switch (currentPage) {
                    case "page_add_dress":
                        if (currentDressItem != null) {
                            navigateBackFromEditDress();
                        } else {
                            navigateBackFromAddDress();
                        }
                        break;
                    case "page_dress_detail":
                        navigateBackFromDressDetail();
                        break;
                    case "page_profile_detail":
                        navigateBackFromProfileDetail();
                        break;
                    case "page_theme_settings":
                        navigateBackFromThemeSettings();
                        break;
                    case "page_notifications":
                        navigateBackFromNotifications();
                        break;
                    case "page_data_import":
                        navigateBackFromDataImport();
                        break;
                    case "page_detail":
                        navigateBackFromDetail();
                        break;
                    case "page_stats":
                        navigateBackFromStats();
                        break;
                    default:
                        finish();
                        break;
                }
            }
        });
    }

    // --- Per-page lazy init methods (called from onPageInflated callback) ---

    @SuppressWarnings("unused")
    private void initOverviewPage(View page) {
        // Overview 由 OverviewFragment 独立管理，此处无需初始化
    }

    private void initDetailPage(View page) {
        rvDressItems = page.findViewById(R.id.rvDressItems);
        etSearch = page.findViewById(R.id.etSearch);
        searchContainer = page.findViewById(R.id.searchContainer);
        chipGroup = page.findViewById(R.id.chipGroup);
        btnAddDress = page.findViewById(R.id.btnAddDress);

        dressItemAdapter = new DressItemAdapter();
        dressItemAdapter.setOnDressItemClickListener(item -> navigateToDressDetail(item));
        dressItemAdapter.setOnDressItemLongPressListener(this::showDressItemContextMenu);
        rvDressItems.setLayoutManager(new LinearLayoutManager(this));
        rvDressItems.setAdapter(dressItemAdapter);
        rvDressItems.setItemViewCacheSize(10);

        // 观察 ViewModel 的裙子柜子详情数据（ViewModel 已转换为 Domain Model）
        viewModel.getWardrobeDressItems().observe(this, dressItems -> {
            if (dressItems == null || dressItemAdapter == null) return;
            // 应用搜索和筛选过滤
            String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase();
            String filter = getSelectedFilter();
            List<DressItem> filtered = new ArrayList<>();
            for (DressItem item : dressItems) {
                String itemName = item.getName();
                String itemStore = item.getStore();
                String itemStatus = item.getStatus();
                boolean matchesQuery = query.isEmpty()
                    || (itemName != null && itemName.toLowerCase().contains(query))
                    || (itemStore != null && itemStore.toLowerCase().contains(query));
                boolean matchesFilter = filter.equals("全部") || (itemStatus != null && itemStatus.contains(filter));
                if (matchesQuery && matchesFilter) {
                    filtered.add(item);
                }
            }
            // 提交数据后恢复
            dressItemAdapter.submitList(filtered);
        });

        com.google.android.material.appbar.MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_search) {
                    if (searchContainer != null && etSearch != null) {
                        if (searchContainer.getVisibility() == View.VISIBLE) {
                            searchContainer.setVisibility(View.GONE);
                            etSearch.setText("");
                            // 重新触发数据刷新
                            if (currentWardrobe != null) {
                                viewModel.fetchDressesByWardrobe(currentWardrobe.getId());
                            }
                        } else {
                            searchContainer.setVisibility(View.VISIBLE);
                            etSearch.requestFocus();
                        }
                    }
                    return true;
                }
                return false;
            });
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 重新触发数据刷新
                if (currentWardrobe != null) {
                    viewModel.fetchDressesByWardrobe(currentWardrobe.getId());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                group.check(R.id.chipAll);
            }
            // 重新触发数据刷新
            if (currentWardrobe != null) {
                viewModel.fetchDressesByWardrobe(currentWardrobe.getId());
            }
        });

        hideDeletedCategoryChips();

        btnAddDress.setOnClickListener(v -> navigateToAddDress());
    }

    private void initAddDressPage(View page) {
        ivMainImagePreview = page.findViewById(R.id.ivMainImagePreview);
        layoutUploadPlaceholder = page.findViewById(R.id.layoutUploadPlaceholder);
        cbDaiQiang = page.findViewById(R.id.cbDaiQiang);
        cbFuYixiang = page.findViewById(R.id.cbFuYixiang);
        cbFuDingjin = page.findViewById(R.id.cbFuDingjin);
        cbBuWeikuan = page.findViewById(R.id.cbBuWeikuan);
        cbDaiFahuo = page.findViewById(R.id.cbDaiFahuo);
        cbDaoShou = page.findViewById(R.id.cbDaoShou);
        layoutDaiqiangDate = page.findViewById(R.id.layoutDaiqiangDate);
        etDaiqiangDate = page.findViewById(R.id.etDaiqiangDate);
        layoutYixiangDate = page.findViewById(R.id.layoutYixiangDate);
        etYixiangDate = page.findViewById(R.id.etYixiangDate);
        layoutDingjinDate = page.findViewById(R.id.layoutDingjinDate);
        etDingjinDate = page.findViewById(R.id.etDingjinDate);
        layoutBuweikuanDate = page.findViewById(R.id.layoutBuweikuanDate);
        etBuweikuanDate = page.findViewById(R.id.etBuweikuanDate);
        etNameAdd = page.findViewById(R.id.etNameAdd);
        etStoreAdd = page.findViewById(R.id.etStoreAdd);
        etChannelAdd = page.findViewById(R.id.etChannelAdd);
        etBuyDateAdd = page.findViewById(R.id.etBuyDateAdd);
        etEarnestMoneyAdd = page.findViewById(R.id.etEarnestMoneyAdd);
        etShippingFeeAdd = page.findViewById(R.id.etShippingFeeAdd);
        cbFullPayment = page.findViewById(R.id.cbFullPayment);
        layoutFullPaymentAmount = page.findViewById(R.id.layoutFullPaymentAmount);
        etFullPaymentAmountAdd = page.findViewById(R.id.etFullPaymentAmountAdd);
        layoutTailPayment = page.findViewById(R.id.layoutTailPayment);
        etTailPaymentAdd = page.findViewById(R.id.etTailPaymentAdd);
        layoutDepositAdd = page.findViewById(R.id.layoutDepositAdd);
        etDepositAdd = page.findViewById(R.id.etDepositAdd);
        btnSaveAddDress = page.findViewById(R.id.btnSaveAddDress);

        // Shipping fee focus behavior: default "包邮" (Free Shipping), on focus become number input
        etShippingFeeAdd.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (DEFAULT_SHIPPING.equals(etShippingFeeAdd.getText().toString().trim())) {
                    etShippingFeeAdd.setText("");
                }
                etShippingFeeAdd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else {
                if (android.text.TextUtils.isEmpty(etShippingFeeAdd.getText().toString().trim())) {
                    etShippingFeeAdd.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                    etShippingFeeAdd.setText(DEFAULT_SHIPPING);
                }
            }
        });


        // Status checkbox -> show/hide per-status date fields
        cbDaiQiang.setOnCheckedChangeListener((btn, checked) -> layoutDaiqiangDate.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbFuYixiang.setOnCheckedChangeListener((btn, checked) -> layoutYixiangDate.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbFuDingjin.setOnCheckedChangeListener((btn, checked) -> layoutDingjinDate.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbBuWeikuan.setOnCheckedChangeListener((btn, checked) -> layoutBuweikuanDate.setVisibility(checked ? View.VISIBLE : View.GONE));

        // Channel picker dialog
        etChannelAdd.setOnClickListener(v -> showChannelPickerDialog());

        // Date pickers
        etBuyDateAdd.setOnClickListener(v -> showDatePickerDialog(etBuyDateAdd));
        etDaiqiangDate.setOnClickListener(v -> showDatePickerDialog(etDaiqiangDate));
        etYixiangDate.setOnClickListener(v -> showDatePickerDialog(etYixiangDate));
        etDingjinDate.setOnClickListener(v -> showDatePickerDialog(etDingjinDate));
        etBuweikuanDate.setOnClickListener(v -> showDatePickerDialog(etBuweikuanDate));

        // Full payment checkbox - 同步状态到 ViewModel
        cbFullPayment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setIsFullPayment(isChecked);
        });

        // 观察 ViewModel 的全款支付状态
        viewModel.isFullPayment().observe(this, isFull -> {
            if (isFull == null) return;
            if (isFull) {
                layoutDepositAdd.setVisibility(View.GONE);
                layoutTailPayment.setVisibility(View.GONE);
                layoutFullPaymentAmount.setVisibility(View.VISIBLE);
            } else {
                layoutDepositAdd.setVisibility(View.VISIBLE);
                layoutTailPayment.setVisibility(View.VISIBLE);
                layoutFullPaymentAmount.setVisibility(View.GONE);
            }
        });

        // Main image picker - whole card clickable
        page.findViewById(R.id.cardMainImage).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.cy.loxia.ui.ImagePickerActivity.class);
            intent.putExtra("crop_type", 2);
            mainImagePickerLauncher.launch(intent);
        });
        ImageView btnPickMainImage = page.findViewById(R.id.btnPickMainImage);
        btnPickMainImage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.cy.loxia.ui.ImagePickerActivity.class);
            intent.putExtra("crop_type", 2);
            mainImagePickerLauncher.launch(intent);
        });

        btnSaveAddDress.setOnClickListener(v -> saveAddDressItem());
    }

    private void initProfileDetailPage(View page) {
        ivProfileAvatar = page.findViewById(R.id.ivProfileAvatar);
        etProfileName = page.findViewById(R.id.etProfileName);
        tvSloganPreview = page.findViewById(R.id.tvSloganPreview);

        page.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileDetail());
        ivProfileAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.cy.loxia.ui.ImagePickerActivity.class);
            intent.putExtra("crop_type", 0);
            avatarPickerLauncher.launch(intent);
        });
    }

    private void initDressDetailPage(View page) {
        ivDressDetailImage = page.findViewById(R.id.ivDressDetailImage);
        tvDressDetailName = page.findViewById(R.id.tvDressDetailName);
        tvDressDetailWardrobeName = page.findViewById(R.id.tvDressDetailWardrobeName);
        tvDressDetailStatus = page.findViewById(R.id.tvDressDetailStatus);
        tvDressDetailStore = page.findViewById(R.id.tvDressDetailStore);
        tvDressDetailChannel = page.findViewById(R.id.tvDressDetailChannel);
        tvDressDetailBuyDate = page.findViewById(R.id.tvDressDetailBuyDate);
        tvDressDetailPrice = page.findViewById(R.id.tvDressDetailPrice);
        tvDressDetailEarnest = page.findViewById(R.id.tvDressDetailEarnest);
        tvDressDetailFullPayment = page.findViewById(R.id.tvDressDetailFullPayment);
        tvDressDetailTailPayment = page.findViewById(R.id.tvDressDetailTailPayment);
        tvDressDetailTailDate = page.findViewById(R.id.tvDressDetailTailDate);
        tvDressDetailShipping = page.findViewById(R.id.tvDressDetailShipping);
        tvDressDetailDeposit = page.findViewById(R.id.tvDressDetailDeposit);
        tvDressDetailRemark = page.findViewById(R.id.tvDressDetailRemark);
        layoutDressDetailTimeNodes = page.findViewById(R.id.layoutDressDetailTimeNodes);
        btnEditDress = page.findViewById(R.id.btnEditDress);
    }

    private void initStatsPage(View page) {
        statsPageManager.initPage(page);
    }

    private void initThemeSettingsPage(View page) {
        rgThemeMode = page.findViewById(R.id.rgThemeMode);
        page.findViewById(R.id.btnSaveTheme).setOnClickListener(v -> saveAndApplyTheme());
    }

    private void initNotificationsPage(View page) {
        swNotificationEnabled = page.findViewById(R.id.swNotificationEnabled);
        btnReminderTime = page.findViewById(R.id.btnReminderTime);
        sliderAdvanceDays = page.findViewById(R.id.sliderAdvanceDays);
        tvAdvanceDays = page.findViewById(R.id.tvAdvanceDays);

        sliderAdvanceDays.addOnChangeListener((slider, value, fromUser) ->
                tvAdvanceDays.setText(((int) value) + " →"));
        btnReminderTime.setOnClickListener(v -> showTimePickerDialog());
        page.findViewById(R.id.btnSaveNotifications).setOnClickListener(v -> saveNotificationSettings());
    }

    private void initDataImportPage(View page) {
        etImportText = page.findViewById(R.id.etImportText);
        tvImportPreview = page.findViewById(R.id.tvImportPreview);
        cardImportPreview = page.findViewById(R.id.cardImportPreview);
        tvImportWardrobeLabel = page.findViewById(R.id.tvImportWardrobeLabel);

        page.findViewById(R.id.btnCopyAiPrompt).setOnClickListener(v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("ai_prompt", getString(R.string.ai_prompt_standard)));
            Toast.makeText(this, "提示词已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });
        page.findViewById(R.id.btnParseImport).setOnClickListener(v -> parseImportText());
        page.findViewById(R.id.btnConfirmImport).setOnClickListener(v -> confirmImport());
        tvImportWardrobeLabel.setOnClickListener(v -> showWardrobePickerForImport());

        // 观察导入结果（只注册一次，避免重复观察者累积）
        viewModel.getImportResult().observe(this, result -> {
            if (result == null) return;
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            if (result.getSuccess()) {
                navigateBackFromDataImport();
            }
            viewModel.clearImportResult();
        });
    }

    private void showTotalCostDialog() {
        Double cost = viewModel.getTotalCost().getValue();
        double totalCost = cost != null ? cost : 0.0;
        dialogManager.showTotalCostDialog(totalCost, new com.cy.loxia.ui.DialogCallbacks.OnConfirm() {
            @Override
            public void onConfirm() {
                viewModel.setTotalCostHidden(false);
            }
        });
    }


    private void showChannelPickerDialog() {
        String currentChannel = etChannelAdd.getText() != null ? etChannelAdd.getText().toString() : "";
        dialogManager.showChannelPickerDialog(currentChannel, new com.cy.loxia.ui.DialogCallbacks.OnSelect<String>() {
            @Override
            public void onSelect(String channel) {
                etChannelAdd.setText(channel);
            }
        });
    }

    private void showDatePickerDialog(TextInputEditText targetField) {
        Calendar calendar = Calendar.getInstance();
        String existingDate = targetField.getText() != null ? targetField.getText().toString() : "";
        if (!existingDate.isEmpty()) {
            try {
                String[] parts = existingDate.split("-");
                if (parts.length == 3) {
                    calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                }
            } catch (Exception e) { // malformed date string, use current date
            }
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            targetField.setText(String.format("%04d-%02d-%02d", y, m + 1, d));
        }, year, month, day);
        dialog.show();
    }

    private void loadData() {
        // Room Flow 自动刷新，initSelectedWardrobe 已在 ViewModel Flow 回调中处理
        viewModel.fetchAllDressItems();
        viewModel.fetchStatistics();
    }

    private void refreshPreviewImages() {
        // Preview images are now managed by HomeFragment via LiveData.
        // No-op: removed synchronous Room query that crashed on main thread.
    }

    private void updateProfilePanel() {
        ProfileFragment frag = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        if (frag != null) {
            frag.updatePanel();
        }
    }

    private void navigateToProfileDetail() {
        navigationManager.showPage("page_profile_detail", getString(R.string.title_profile_detail), true, false);
        if (ivProfileAvatar == null || etProfileName == null || tvSloganPreview == null) return;

        String avatarUri = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_PROFILE_AVATAR, null);
        pendingAvatarUri = avatarUri != null ? Uri.parse(avatarUri) : null;
        if (avatarUri != null) {
            ImageUtils.loadIntoView(this, avatarUri, ivProfileAvatar, R.drawable.bg_image_placeholder);
        } else {
            ivProfileAvatar.setImageResource(R.drawable.bg_image_placeholder);
        }
        String nickname = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_PROFILE_NICKNAME, "");
        etProfileName.setText(nickname);
        tvSloganPreview.setText(getDailySlogan());
    }

    // === ProfileFragment.Host implementation ===

    @Override
    public void onProfileNavigateToThemeSettings() {
        navigateToThemeSettings();
    }

    @Override
    public void onProfileNavigateToStats() {
        navigateToStats();
        statsPageManager.updatePage();
    }

    @Override
    public void onProfileNavigateToDataImport() {
        navigateToDataImport();
    }

    @Override
    public void onProfileNavigateToNotifications() {
        navigateToNotifications();
    }

    @Override
    public void onProfileNavigateToProfileDetail() {
        navigateToProfileDetail();
    }

    // === HomeFragment.Host implementation ===

    @Override
    public void onWardrobeClick(Wardrobe wardrobe) {
        currentWardrobe = wardrobe;
        navigateToDetail();
        updateDetailPage();
    }

    @Override
    public void onWardrobeLongPress(Wardrobe wardrobe, int position) {
        showWardrobeContextMenu(wardrobe, position);
    }

    @Override
    public void onAddWardrobeClick() {
        showAddWardrobeDialog();
    }

    @Override
    public void onTotalCostClick() {
        showTotalCostDialog();
    }

    // === OverviewFragment.Host implementation ===

    @Override
    public void onDressItemClick(DressItem item) {
        isFromOverview = true;
        navigateToDressDetail(item);
    }

    private void navigateBackFromProfileDetail() {
        hideAllPages();
        restoreMainContent();
        showPage(R.id.nav_profile);
        currentMainNavId = R.id.nav_profile;
        updateNavSelection();
        updateProfilePanel();
    }

    private void saveProfileDetail() {
        String nickname = etProfileName.getText() != null ? etProfileName.getText().toString().trim() : "";
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_PROFILE_NICKNAME, nickname)
                .putString(KEY_PROFILE_AVATAR, pendingAvatarUri != null ? pendingAvatarUri.toString() : getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_PROFILE_AVATAR, null))
                .apply();
        pendingAvatarUri = null;
        Toast.makeText(this, R.string.action_save_profile, Toast.LENGTH_SHORT).show();
        navigateBackFromProfileDetail();
    }

    private String getDailySlogan() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String[] slogans = getResources().getStringArray(R.array.slogan_pool);
        if (slogans.length == 0) return "";
        String today = java.time.LocalDate.now().toString();
        String savedDate = prefs.getString(KEY_SLOGAN_DATE, null);
        String savedText = prefs.getString(KEY_SLOGAN_TEXT, null);
        // 如果缓存的标语不在有效列表中（旧乱码缓存），强制重新选取
        if (savedDate != null && savedDate.equals(today) && savedText != null) {
            for (String s : slogans) {
                if (s.equals(savedText)) return savedText;
            }
        }
        // 重新随机选取
        String slogan = slogans[new java.util.Random().nextInt(slogans.length)];
        prefs.edit().putString(KEY_SLOGAN_DATE, today).putString(KEY_SLOGAN_TEXT, slogan).apply();
        return slogan;
    }

    private void updateDetailPage() {
        if (currentWardrobe == null) {
            if (!wardrobes.isEmpty()) {
                currentWardrobe = wardrobes.get(0);
            }
            return;
        }
        // detail 页面可能还没 inflate，跳过 UI 更新
        if (rvDressItems == null) return;

        // ⚡ 异步拉取数据前，清空数据，切断旧数据残影
        if (rvDressItems != null && dressItemAdapter != null) {
            dressItemAdapter.submitList(null);
        }

        viewModel.fetchDressesByWardrobe(currentWardrobe.getId());
    }

    private void updateOverview() {
        // Overview 由 OverviewFragment 独立管理，通过 ViewModel Flow 自动刷新
    }

    private String getSelectedFilter() {
        if (chipGroup == null) return "全部";
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipDaiQiang) return "待抢";
        if (checkedId == R.id.chipFuYixiang) return "付意向";
        if (checkedId == R.id.chipFuDingjin) return "付定金";
        if (checkedId == R.id.chipBuWeikuan) return "补尾款";
        if (checkedId == R.id.chipDaiFahuo) return "待发货";
        if (checkedId == R.id.chipDaoShou) return "已到手";
        return "全部";
    }


    private void saveAddDressItem() {
        boolean isEdit = currentDressItem != null;

        if (!isEdit && currentWardrobe == null) {
            Toast.makeText(this, "请先选择一个柜子", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etNameAdd.getText() == null ? "" : etNameAdd.getText().toString().trim();
        String store = etStoreAdd.getText() == null ? "" : etStoreAdd.getText().toString().trim();
        String channel = etChannelAdd.getText() == null ? "" : etChannelAdd.getText().toString().trim();
        String buyDate = etBuyDateAdd.getText() == null ? "" : etBuyDateAdd.getText().toString().trim();
        String status = getCheckedStatusString();
        String earnestText = etEarnestMoneyAdd.getText() == null ? "" : etEarnestMoneyAdd.getText().toString().trim();
        String fullPaymentText = etFullPaymentAmountAdd.getText() == null ? "" : etFullPaymentAmountAdd.getText().toString().trim();
        String tailPaymentText = etTailPaymentAdd.getText() == null ? "" : etTailPaymentAdd.getText().toString().trim();

        if (name.isEmpty() || status.isEmpty()) {
            Toast.makeText(this, "请补全必填字段：裙子名称、裙子状态", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!buyDate.isEmpty()) {
            try {
                LocalDate.parse(buyDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                Toast.makeText(this, "购入时间格式不正确，请通过选择器选择日期", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Gather per-status dates
        String daiqiangDate = cbDaiQiang.isChecked() && etDaiqiangDate.getText() != null ? etDaiqiangDate.getText().toString().trim() : "";
        String yixiangDate = cbFuYixiang.isChecked() && etYixiangDate.getText() != null ? etYixiangDate.getText().toString().trim() : "";
        String dingjinDate = cbFuDingjin.isChecked() && etDingjinDate.getText() != null ? etDingjinDate.getText().toString().trim() : "";
        String buweikuanDate = cbBuWeikuan.isChecked() && etBuweikuanDate.getText() != null ? etBuweikuanDate.getText().toString().trim() : "";

        // Validate date formats
        if (!validateOptionalDate(daiqiangDate) || !validateOptionalDate(yixiangDate) || !validateOptionalDate(dingjinDate) || !validateOptionalDate(buweikuanDate)) {
            return;
        }

        boolean isFull = cbFullPayment.isChecked();
        double earnestMoney;
        double fullPaymentAmount = 0.0;
        double tailPayment = 0.0;
        double price;
        double deposit;
        try {
            earnestMoney = earnestText.isEmpty() ? 0.0 : Double.parseDouble(earnestText);
            String depositText = etDepositAdd.getText() == null ? "" : etDepositAdd.getText().toString().trim();
            deposit = depositText.isEmpty() ? 0.0 : Double.parseDouble(depositText);

            if (isFull) {
                if (!fullPaymentText.isEmpty()) {
                    fullPaymentAmount = Double.parseDouble(fullPaymentText);
                }
                price = fullPaymentAmount > 0 ? fullPaymentAmount : (earnestMoney > 0 ? earnestMoney : 0);
            } else {
                if (!tailPaymentText.isEmpty()) {
                    tailPayment = Double.parseDouble(tailPaymentText);
                }
                double nonFullTotal = earnestMoney + deposit + tailPayment;
                price = nonFullTotal > 0 ? nonFullTotal : 0;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式不正确，请检查输入", Toast.LENGTH_SHORT).show();
            return;
        }

        String shippingFee = etShippingFeeAdd.getText() == null ? DEFAULT_SHIPPING : etShippingFeeAdd.getText().toString().trim();
        if (shippingFee.isEmpty()) shippingFee = DEFAULT_SHIPPING;

        String wardrobeId = isEdit ? currentDressItem.getWardrobeId() : currentWardrobe.getId();
        String itemId = isEdit ? currentDressItem.getId() : UUID.randomUUID().toString();
        String remark = isEdit ? currentDressItem.getRemark() : "";
        String remindAt = isEdit ? currentDressItem.getRemindAt() : "";

        DressItem item = new DressItem(
                itemId, wardrobeId,
                name, store, channel, price, buyDate, status, remindAt, remark
        );
        item.setEarnestMoney(earnestMoney);
        item.setFullPayment(isFull);
        item.setFullPaymentAmount(fullPaymentAmount);
        item.setTailPayment(tailPayment);
        // Kotlin 非空类型不能赋 null，使用空字符串代替
        item.setDaiqiangDate(daiqiangDate != null ? daiqiangDate : "");
        item.setYixiangDate(yixiangDate != null ? yixiangDate : "");
        item.setDingjinDate(dingjinDate != null ? dingjinDate : "");
        item.setBuweikuanDate(buweikuanDate != null ? buweikuanDate : "");

        if (pendingMainImageUri != null) {
            item.setImageUri(pendingMainImageUri.toString());
        } else if (isEdit) {
            item.setImageUri(currentDressItem.getImageUri());
        }
        item.setShippingFee(shippingFee);
        item.setDeposit(deposit);
        // Preserve pinned/sortOrder when editing
        if (isEdit) {
            item.setPinned(currentDressItem.isPinned());
            item.setSortOrder(currentDressItem.getSortOrder());
        }

        if (isEdit) {
            viewModel.launchUpdateDressItem(item);
            Toast.makeText(this, "已更新裙子记录", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.launchAddDressItem(item);
            Toast.makeText(this, "已保存裙子记录", Toast.LENGTH_SHORT).show();
        }

        // Schedule or cancel alarms based on item status dates（使用 IO 线程避免阻塞主线程）
        boolean notificationsEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_NOTIFICATIONS, true);
        ioExecutor.execute(() -> {
            AlarmScheduler.cancelReminder(MainActivity.this, item);
            if (notificationsEnabled) {
                AlarmScheduler.scheduleAllPendingReminders(MainActivity.this, repository);
            }
        });

        clearAddDressForm();
        loadData();

        if (isEdit) {
            currentDressItem = item;
            navigationManager.showPage("page_dress_detail", "裙子详情", true, false);
            updateDressDetailPage();
        } else {
            navigateBackFromAddDress();
        }
    }

    private boolean validateOptionalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return true;
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "日期格式不正确，请通过选择器选择日期", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void clearAddDressForm() {
        if (etNameAdd == null) return; // page not yet inflated
        etNameAdd.setText("");
        etStoreAdd.setText("");
        etChannelAdd.setText("");
        etBuyDateAdd.setText("");
        etEarnestMoneyAdd.setText("");
        etShippingFeeAdd.setText(DEFAULT_SHIPPING);
        // Clear all status checkboxes and hide date fields
        cbDaiQiang.setChecked(false);
        cbFuYixiang.setChecked(false);
        cbFuDingjin.setChecked(false);
        cbBuWeikuan.setChecked(false);
        cbDaiFahuo.setChecked(false);
        cbDaoShou.setChecked(false);
        layoutDaiqiangDate.setVisibility(View.GONE);
        layoutYixiangDate.setVisibility(View.GONE);
        layoutDingjinDate.setVisibility(View.GONE);
        layoutBuweikuanDate.setVisibility(View.GONE);
        etDaiqiangDate.setText("");
        etYixiangDate.setText("");
        etDingjinDate.setText("");
        etBuweikuanDate.setText("");
        cbFullPayment.setChecked(false);
        etDepositAdd.setText("");
        layoutDepositAdd.setVisibility(View.VISIBLE);
        layoutFullPaymentAmount.setVisibility(View.GONE);
        layoutTailPayment.setVisibility(View.VISIBLE);
        etFullPaymentAmountAdd.setText("");
        etTailPaymentAdd.setText("");
        pendingMainImageUri = null;
        ivMainImagePreview.setVisibility(View.GONE);
        layoutUploadPlaceholder.setVisibility(View.VISIBLE);
    }

    private void showAddWardrobeDialog() {
        pendingWardrobeCover = null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_wardrobe, null);
        dialogCoverPreview = dialogView.findViewById(R.id.ivCoverPreview);
        TextInputEditText etName = dialogView.findViewById(R.id.etWardrobeName);
        MaterialButton btnPick = dialogView.findViewById(R.id.btnPickCover);

        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.cy.loxia.ui.ImagePickerActivity.class);
            intent.putExtra("crop_type", 1);
            wardrobeCoverPickerLauncher.launch(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("添加新柜子")
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", (d, which) -> {
                pendingWardrobeCover = null;
                dialogCoverPreview = null;
            })
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入柜子名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                String coverUri = pendingWardrobeCover != null ? pendingWardrobeCover.toString() : "";
                Wardrobe wardrobe = new Wardrobe(UUID.randomUUID().toString(), name, 0, System.currentTimeMillis(), false, coverUri, 0);
                viewModel.launchAddWardrobe(wardrobe);
                pendingWardrobeCover = null;
                dialogCoverPreview = null;
                loadData();
                Toast.makeText(MainActivity.this, "柜子已添加", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showWardrobeContextMenu(Wardrobe wardrobe, int position) {
        String[] options = {"删除柜子", "上移", "下移"};
        new AlertDialog.Builder(this)
            .setTitle(wardrobe.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showDeleteWardrobeDialog(wardrobe);
                        break;
                    case 1:
                        if (position > 0) {
                            viewModel.launchMoveWardrobeUp(wardrobe.getId());
                            loadData();
                            Toast.makeText(MainActivity.this, "已上移", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "已经是第一个了", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        if (position < wardrobes.size() - 1) {
                            viewModel.launchMoveWardrobeDown(wardrobe.getId());
                            loadData();
                            Toast.makeText(MainActivity.this, "已下移", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "已经是最后一个了", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showDressItemContextMenu(DressItem item, int position) {
        String pinLabel = item.isPinned() ? "取消置顶" : "置顶";
        String[] options = {pinLabel, "上移一步", "下移一步", "移至顶部", "移至底部", "删除"};
        new AlertDialog.Builder(this)
            .setTitle(item.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: {
                        boolean newPinned = !item.isPinned();
                        viewModel.launchPinDressItem(item.getId(), newPinned);
                        viewModel.fetchDressesByWardrobe(item.getWardrobeId());
                        getWindow().getDecorView().performHapticFeedback(
                                HapticFeedbackConstants.CONFIRM);
                        Toast.makeText(MainActivity.this,
                                newPinned ? "已置顶" : "已取消置顶", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 1: {
                        viewModel.launchGetSortedItemsForWardrobe(item.getWardrobeId(), sorted -> {
                            runOnUiThread(() -> {
                                int idx = -1;
                                for (int i = 0; i < sorted.size(); i++) {
                                    if (sorted.get(i).getId().equals(item.getId())) { idx = i; break; }
                                }
                                if (idx > 0) {
                                    viewModel.launchMoveDressItemUp(item.getId(), item.getWardrobeId());
                                    viewModel.fetchDressesByWardrobe(item.getWardrobeId());
                                    getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                                    Toast.makeText(MainActivity.this, "已上移", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "已经是第一个了", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return kotlin.Unit.INSTANCE;
                        });
                        break;
                    }
                    case 2: {
                        viewModel.launchGetSortedItemsForWardrobe(item.getWardrobeId(), sorted -> {
                            runOnUiThread(() -> {
                                int idx = -1;
                                for (int i = 0; i < sorted.size(); i++) {
                                    if (sorted.get(i).getId().equals(item.getId())) { idx = i; break; }
                                }
                                if (idx < sorted.size() - 1) {
                                    viewModel.launchMoveDressItemDown(item.getId(), item.getWardrobeId());
                                    viewModel.fetchDressesByWardrobe(item.getWardrobeId());
                                    getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                                    Toast.makeText(MainActivity.this, "已下移", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "已经是最后一个了", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return kotlin.Unit.INSTANCE;
                        });
                        break;
                    }
                    case 3:
                        viewModel.launchMoveDressItemToTop(item.getId(), item.getWardrobeId());
                        viewModel.fetchDressesByWardrobe(item.getWardrobeId());
                        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                        Toast.makeText(MainActivity.this, "已移至顶部", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        viewModel.launchMoveDressItemToBottom(item.getId(), item.getWardrobeId());
                        viewModel.fetchDressesByWardrobe(item.getWardrobeId());
                        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                        Toast.makeText(MainActivity.this, "已移至底部", Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("确认删除")
                            .setMessage("确定要删除「" + item.getName() + "」吗？删除后不可恢复")
                            .setPositiveButton("删除", (delDialog, delWhich) -> {
                                viewModel.launchDeleteDressItem(item.getId());
                                loadData();  // 刷新所有数据包括衣橱预览图
                                getWindow().getDecorView().performHapticFeedback(
                                        HapticFeedbackConstants.CONFIRM);
                                Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // --- Notification settings ---

    private void navigateToNotifications() {
        navigationManager.showPage("page_notifications", getString(R.string.title_notifications), true, false);
        if (swNotificationEnabled == null || btnReminderTime == null || sliderAdvanceDays == null) return;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        swNotificationEnabled.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));

        notifTimeHour = prefs.getInt(KEY_NOTIF_TIME_HOUR, 8);
        notifTimeMinute = prefs.getInt(KEY_NOTIF_TIME_MINUTE, 0);
        btnReminderTime.setText(String.format("%02d:%02d", notifTimeHour, notifTimeMinute));

        sliderAdvanceDays.setValue(prefs.getInt(KEY_NOTIF_ADVANCE_DAYS, 3));
    }

    private void navigateBackFromNotifications() {
        hideAllPages();
        restoreMainContent();
        showPage(R.id.nav_profile);
        currentMainNavId = R.id.nav_profile;
        updateNavSelection();
    }

    private void showTimePickerDialog() {
        android.app.TimePickerDialog dialog = new android.app.TimePickerDialog(
                this, (view, hourOfDay, minute) -> {
            notifTimeHour = hourOfDay;
            notifTimeMinute = minute;
            btnReminderTime.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, notifTimeHour, notifTimeMinute, true);
        dialog.show();
    }

    private void saveNotificationSettings() {
        boolean enabled = swNotificationEnabled.isChecked();
        int advanceDays = (int) sliderAdvanceDays.getValue();

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_NOTIFICATIONS, enabled)
                .putInt(KEY_NOTIF_TIME_HOUR, notifTimeHour)
                .putInt(KEY_NOTIF_TIME_MINUTE, notifTimeMinute)
                .putInt(KEY_NOTIF_ADVANCE_DAYS, advanceDays)
                .apply();

        // 更新所有柜子的提醒（使用 IO 线程避免阻塞主线程）
        ioExecutor.execute(() -> {
            AlarmScheduler.cancelAllReminders(MainActivity.this, repository);
            if (enabled) {
                // 统一设置所有状态的提前天数
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putInt(KEY_NOTIF_DAIQIANG, advanceDays)
                        .putInt(KEY_NOTIF_YIXIANG, advanceDays)
                        .putInt(KEY_NOTIF_DINGJIN, advanceDays)
                        .putInt(KEY_NOTIF_BUWEIKUAN, advanceDays)
                        .apply();
                AlarmScheduler.scheduleAllPendingReminders(MainActivity.this, repository);
            }
        });

        Toast.makeText(this, "提醒设置已保存", Toast.LENGTH_SHORT).show();
        navigateBackFromNotifications();
    }

    // --- Data import ---

    private void navigateToDataImport() {
        navigationManager.showPage("page_data_import", getString(R.string.title_data_import), true, false);
        if (etImportText != null) etImportText.setText("");
        if (cardImportPreview != null) cardImportPreview.setVisibility(View.GONE);
        pendingImportItems = null;
        selectedImportWardrobeId = null;
    }

    private void navigateBackFromDataImport() {
        hideAllPages();
        restoreMainContent();
        showPage(R.id.nav_profile);
        currentMainNavId = R.id.nav_profile;
        updateNavSelection();
    }

    private void parseImportText() {
        String text = etImportText.getText() != null ? etImportText.getText().toString().trim() : "";
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.import_none_parsed, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingImportItems = DataBackupManager.parsePlainText(text);
        if (pendingImportItems == null || pendingImportItems.isEmpty()) {
            Toast.makeText(this, R.string.import_none_parsed, Toast.LENGTH_SHORT).show();
            cardImportPreview.setVisibility(View.GONE);
            return;
        }
        tvImportPreview.setText(String.format(getString(R.string.import_parsed_count), pendingImportItems.size()));
        selectImportWardrobe(null);
        cardImportPreview.setVisibility(View.VISIBLE);
    }

    private void showWardrobePickerForImport() {
        viewModel.launchGetAllWardrobes(wardrobeList -> {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (wardrobeList.isEmpty()) {
                    Toast.makeText(this, "请先创建一个柜子", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = new String[wardrobeList.size()];
                for (int i = 0; i < wardrobeList.size(); i++) {
                    names[i] = wardrobeList.get(i).getName();
                }
                new AlertDialog.Builder(this)
                    .setTitle("选择目标柜子")
                    .setItems(names, (dialog, which) -> {
                        selectImportWardrobe(names[which]);
                        selectedImportWardrobeId = wardrobeList.get(which).getId();
                    })
                    .show();
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void selectImportWardrobe(String name) {
        if (name != null) {
            tvImportWardrobeLabel.setText("导入到柜子：" + name);
        } else {
            viewModel.launchGetAllWardrobes(wardrobeList -> {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!wardrobeList.isEmpty()) {
                        selectedImportWardrobeId = wardrobeList.get(0).getId();
                        tvImportWardrobeLabel.setText("导入到柜子：" + wardrobeList.get(0).getName());
                    } else {
                        tvImportWardrobeLabel.setText("导入到柜子：");
                    }
                });
                return kotlin.Unit.INSTANCE;
            });
        }
    }

    private void confirmImport() {
        if (pendingImportItems == null || pendingImportItems.isEmpty()) {
            Toast.makeText(this, R.string.import_none_parsed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImportWardrobeId == null) {
            Toast.makeText(this, "请先选择一个柜子", Toast.LENGTH_SHORT).show();
            return;
        }

        // 直接传递 Domain Model，ViewModel 内部处理 wardrobeId 和 addTime
        viewModel.importParsedData(selectedImportWardrobeId, pendingImportItems);
    }

    private void navigateToThemeSettings() {
        navigationManager.showPage("page_theme_settings", getString(R.string.title_theme_settings), true, false);
        if (rgThemeMode == null) return;

        // Restore current mode selection
        String currentMode = repository.getThemeMode();
        switch (currentMode) {
            case THEME_LIGHT: rgThemeMode.check(R.id.rbThemeLight); break;
            case THEME_DARK: rgThemeMode.check(R.id.rbThemeDark); break;
            default: rgThemeMode.check(R.id.rbThemeSystem); break;
        }
    }

    private void navigateBackFromThemeSettings() {
        hideAllPages();
        restoreMainContent();
        showPage(R.id.nav_profile);
        currentMainNavId = R.id.nav_profile;
        updateNavSelection();
    }

    private void saveAndApplyTheme() {
        int checkedId = rgThemeMode.getCheckedRadioButtonId();
        String newMode;
        if (checkedId == R.id.rbThemeLight) {
            newMode = THEME_LIGHT;
        } else if (checkedId == R.id.rbThemeDark) {
            newMode = THEME_DARK;
        } else {
            newMode = THEME_SYSTEM;
        }
        repository.setThemeMode(newMode);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString("theme_mode", newMode)
                .putInt("last_main_nav", currentMainNavId)
                .putBoolean(KEY_RESTORE_NAV, true)
                .apply();

        applyThemeMode(newMode);
        Toast.makeText(this, R.string.theme_save, Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void showDeleteWardrobeDialog(Wardrobe wardrobe) {
        new AlertDialog.Builder(this)
            .setTitle("删除柜子")
            .setMessage("确定要删除「" + wardrobe.getName() + "」吗？其中的裙子记录也会被删除")
            .setPositiveButton("删除", (dialog, which) -> {
                viewModel.launchDeleteWardrobe(wardrobe.getId());
                loadData();
                if (currentWardrobe != null && currentWardrobe.getId().equals(wardrobe.getId())) {
                    currentWardrobe = null;
                    hideAllPages();
                    restoreMainContent();
                    showPage(R.id.nav_detail);
                }
                Toast.makeText(MainActivity.this, "柜子已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void hideDeletedCategoryChips() {
        List<String> hidden = repository.getHiddenCategories();
        if (hidden.contains("待抢")) findViewById(R.id.chipDaiQiang).setVisibility(View.GONE);
        if (hidden.contains("付意向")) findViewById(R.id.chipFuYixiang).setVisibility(View.GONE);
        if (hidden.contains("付定金")) findViewById(R.id.chipFuDingjin).setVisibility(View.GONE);
        if (hidden.contains("补尾款")) findViewById(R.id.chipBuWeikuan).setVisibility(View.GONE);
        if (hidden.contains("待发货")) findViewById(R.id.chipDaiFahuo).setVisibility(View.GONE);
        if (hidden.contains("已到手")) findViewById(R.id.chipDaoShou).setVisibility(View.GONE);
    }

    private void showPage(int itemId) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        androidx.fragment.app.Fragment home = getSupportFragmentManager().findFragmentByTag("home");
        androidx.fragment.app.Fragment overview = getSupportFragmentManager().findFragmentByTag("overview");
        androidx.fragment.app.Fragment profile = getSupportFragmentManager().findFragmentByTag("profile");
        
        if (home != null) transaction.hide(home);
        if (overview != null) transaction.hide(overview);
        if (profile != null) transaction.hide(profile);
        
        androidx.fragment.app.Fragment target;
        if (itemId == R.id.nav_detail) {
            target = findOrCreateFragment("home", HomeFragment::new);
            if (!target.isAdded()) transaction.add(R.id.main_content_container, target, "home");
            transaction.show(target);
            currentMainNavId = R.id.nav_detail;
        } else if (itemId == R.id.nav_overview) {
            target = findOrCreateFragment("overview", OverviewFragment::new);
            if (!target.isAdded()) transaction.add(R.id.main_content_container, target, "overview");
            transaction.show(target);
            currentMainNavId = R.id.nav_overview;
        } else {
            target = findOrCreateFragment("profile", ProfileFragment::new);
            if (!target.isAdded()) transaction.add(R.id.main_content_container, target, "profile");
            transaction.show(target);
            currentMainNavId = R.id.nav_profile;
        }
        
        transaction.commit();
        updateNavSelection();
    }

    private androidx.fragment.app.Fragment findOrCreateFragment(String tag, java.util.function.Supplier<androidx.fragment.app.Fragment> factory) {
        androidx.fragment.app.Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        return existing != null ? existing : factory.get();
    }

    private void updateNavSelection() {
        int pink = ContextCompat.getColor(this, R.color.pink_primary);
        int grey = ContextCompat.getColor(this, R.color.nav_unselected);

        // 主页
        boolean isHome = currentMainNavId == R.id.nav_detail;
        ivNavHome.setColorFilter(isHome ? pink : grey);
        tvNavHome.setTextColor(isHome ? pink : grey);
        tvNavHome.setTypeface(null, isHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // 总览
        boolean isOverview = currentMainNavId == R.id.nav_overview;
        ivNavOverview.setColorFilter(isOverview ? pink : grey);
        tvNavOverview.setTextColor(isOverview ? pink : grey);
        tvNavOverview.setTypeface(null, isOverview ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // 我的
        boolean isProfile = currentMainNavId == R.id.nav_profile;
        ivNavProfile.setColorFilter(isProfile ? pink : grey);
        tvNavProfile.setTextColor(isProfile ? pink : grey);
        tvNavProfile.setTypeface(null, isProfile ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // 滑动指示器
        View targetTab;
        if (isHome) targetTab = navHome;
        else if (isOverview) targetTab = navOverview;
        else targetTab = navProfile;

        // 等布局完成后再获取位置再动画
        navIndicator.post(() -> {
            int tabW = targetTab.getWidth();

            // 设置指示器宽度与 Tab 一致
            if (navIndicator.getWidth() != tabW) {
                android.view.ViewGroup.LayoutParams lp = navIndicator.getLayoutParams();
                lp.width = tabW;
                navIndicator.setLayoutParams(lp);
            }

            // 使用 translationX 实现滑动动画
            float targetX = targetTab.getLeft();
            navIndicator.animate()
                .translationX(targetX)
                .setDuration(200)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                .start();
            navIndicator.setVisibility(View.VISIBLE);
        });
    }

    private void navigateToStats() {
        navigationManager.showPage("page_stats", "收藏日程", true, false);
    }

    private void navigateBackFromStats() {
        hideAllPages();
        restoreMainContent();
        showPage(R.id.nav_profile);
        currentMainNavId = R.id.nav_profile;
        updateNavSelection();
    }


    private void updateStatsPage() {
        statsPageManager.updatePage();
    }





    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); }

    // Copy image from content URI to app internal storage, returns absolute path
    private String copyImageToInternalStorage(Uri sourceUri) {
        if (sourceUri == null) return null;
        try {
            File imagesDir = new File(getFilesDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();
            String fileName = UUID.randomUUID().toString() + ".jpg";
            File destFile = new File(imagesDir, fileName);
            try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
                if (in == null) return null;
                try (OutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "图片保存失败，请重试", Toast.LENGTH_SHORT).show());
            return null;
        }
    }

    /**
     * 创建 UCrop 配置选项，统一主题样式
     */
    private UCrop.Options createUCropOptions() {
        UCrop.Options options = new UCrop.Options();
        // 设置主题颜色
        options.setToolbarColor(ContextCompat.getColor(this, R.color.pink_primary));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.pink_primary));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.pink_primary));
        // 设置裁剪框样式
        options.setCropFrameStrokeWidth(2);
        options.setCropGridRowCount(2);
        options.setCropGridColumnCount(2);
        // 启用自由裁剪（允许用户手动调整比例）
        options.setFreeStyleCropEnabled(true);
        // 设置图片默认展示方式为 fitCenter（完整显示图片）
        options.setImageToCropBoundsAnimDuration(300);
        // 隐藏底部工具栏（删除滑动条缩放，使用双指捏合缩放）
        options.setHideBottomControls(true);
        // 设置压缩格式和质量
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        return options;
    }

    private File createCropTempFile() {
        File imagesDir = new File(getFilesDir(), "images");
        if (!imagesDir.exists()) imagesDir.mkdirs();
        return new File(imagesDir, "crop_" + UUID.randomUUID().toString() + ".jpg");
    }

    private void handleCroppedImage(int targetWidth, int targetHeight,
            java.util.function.BiConsumer<String, Bitmap> onUiThread) {
        final Uri cropUri = pendingCropTargetUri;
        if (cropUri == null) return;

        // 使用 IO 线程处理图片
        ioExecutor.execute(() -> {
            String localPath = copyImageToInternalStorage(cropUri);
            if (localPath != null) {
                Bitmap bitmap = ImageUtils.decodeSampledBitmap(localPath, targetWidth, targetHeight);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    onUiThread.accept(localPath, bitmap);
                });
            }
        });
    }

    private void handleCroppedAvatar() {
        handleCroppedImage(512, 512, (localPath, bitmap) -> {
            pendingAvatarUri = Uri.parse(localPath);
            if (ivProfileAvatar != null) {
                ImageUtils.loadIntoView(this, localPath, ivProfileAvatar, R.drawable.bg_image_placeholder);
            }
        });
    }

    private void handleCroppedCover() {
        handleCroppedImage(512, 512, (localPath, bitmap) -> {
            pendingWardrobeCover = Uri.parse(localPath);
            if (dialogCoverPreview != null) {
                ImageUtils.loadIntoView(this, localPath, dialogCoverPreview, R.drawable.wardrobe_default);
            }
        });
    }

    private void handleCroppedMainImage() {
        handleCroppedImage(800, 600, (localPath, bitmap) -> {
            pendingMainImageUri = Uri.parse(localPath);
            if (bitmap != null && ivMainImagePreview != null) {
                ivMainImagePreview.setImageBitmap(bitmap);
                ivMainImagePreview.setVisibility(View.VISIBLE);
                if (layoutUploadPlaceholder != null) {
                    layoutUploadPlaceholder.setVisibility(View.GONE);
                }
            }
        });
    }

    // Run migration for all existing content URIs on startup (已在后台线程)
    private void migrateExistingImages() {
        try {
            List<DressItem> allItems = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getDressItemsAsync(continuation)
            );
            boolean itemsChanged = false;
            for (DressItem item : allItems) {
                String uri = item.getImageUri();
                if (uri != null && !uri.isEmpty() && (uri.startsWith("content://"))) {
                    String newPath = copyImageToInternalStorage(Uri.parse(uri));
                    if (newPath != null) {
                        item.setImageUri(newPath);
                        itemsChanged = true;
                    }
                }
            }
            if (itemsChanged) {
                kotlinx.coroutines.BuildersKt.runBlocking(
                    kotlinx.coroutines.Dispatchers.getIO(),
                    (scope, continuation) -> {
                        repository.updateAllDressItems(allItems, continuation);
                        return kotlin.Unit.INSTANCE;
                    }
                );
            }
            List<Wardrobe> allWardrobes = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getWardrobesAsync(continuation)
            );
            boolean wChanged = false;
            List<Wardrobe> updatedWardrobes = new ArrayList<>();
            for (Wardrobe w : allWardrobes) {
                String cover = w.getCover();
                if (cover != null && !cover.isEmpty() && cover.startsWith("content://")) {
                    String newPath = copyImageToInternalStorage(Uri.parse(cover));
                    if (newPath != null) {
                        updatedWardrobes.add(new Wardrobe(w.getId(), w.getName(), w.getCount(), w.getUpdatedAt(), w.isDemo(), newPath, w.getSortOrder()));
                        wChanged = true;
                    } else {
                        updatedWardrobes.add(w);
                    }
                } else {
                    updatedWardrobes.add(w);
                }
            }
            if (wChanged) {
                kotlinx.coroutines.BuildersKt.runBlocking(
                    kotlinx.coroutines.Dispatchers.getIO(),
                    (scope, continuation) -> {
                        repository.updateAllWardrobes(updatedWardrobes, continuation);
                        return kotlin.Unit.INSTANCE;
                    }
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("MainActivity", "migrateExistingImages interrupted", e);
        }
    }


    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                hideSoftInput(v.getWindowToken());
                v.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isShouldHideInput(View v, android.view.MotionEvent event) {
        if (v != null && (v instanceof android.widget.EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private void hideSoftInput(android.os.IBinder token) {
        if (token != null) {
            android.view.inputmethod.InputMethodManager im = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (im != null) {
                im.hideSoftInputFromWindow(token, android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // 停止 IO 线程池，防止在已销毁的 Activity 上回调逻辑
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

}
