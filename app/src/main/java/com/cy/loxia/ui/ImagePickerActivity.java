package com.cy.loxia.ui;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cy.loxia.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePickerActivity extends AppCompatActivity {

    private RecyclerView rvImages;
    private TextView tvEmpty;
    private ImagePickerAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        Intent data = new Intent();
                        data.setData(resultUri);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
                    Throwable error = com.yalantis.ucrop.UCrop.getError(result.getData());
                    if (error != null) {
                        Toast.makeText(this, "裁剪失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadImages();
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "需要存储权限才能读取相册", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_picker);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

        rvImages = findViewById(R.id.rvImages);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImagePickerAdapter();
        adapter.setOnImageClickListener(uri -> {
            int cropType = getIntent().getIntExtra("crop_type", 2);
            java.io.File cropDest = createCropTempFile();
            Uri pendingCropTargetUri = Uri.fromFile(cropDest);
            com.yalantis.ucrop.UCrop.Options options = createUCropOptions();
            com.yalantis.ucrop.UCrop uCrop = com.yalantis.ucrop.UCrop.of(uri, pendingCropTargetUri)
                    .withOptions(options);
            if (cropType == 0) { // avatar
                options.setCircleDimmedLayer(true);
                options.setShowCropGrid(false);
                uCrop.withAspectRatio(1, 1).withMaxResultSize(512, 512);
            } else if (cropType == 1) { // cover
                uCrop.withAspectRatio(3, 4).withMaxResultSize(768, 1024);
            } else { // main
                uCrop.withAspectRatio(3, 4).withMaxResultSize(1536, 2048);
            }
            cropLauncher.launch(uCrop.getIntent(ImagePickerActivity.this));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        rvImages.setAdapter(adapter);

        checkPermissionAndLoadImages();
    }

    private void checkPermissionAndLoadImages() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadImages();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void loadImages() {
        executor.execute(() -> {
            List<Uri> imageUris = new ArrayList<>();
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = new String[] {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED
            };

            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(
                    collection,
                    projection,
                    null,
                    null,
                    sortOrder
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        imageUris.add(contentUri);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (imageUris.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.submitList(imageUris);
                }
            });
        });
    }

    private java.io.File createCropTempFile() {
        java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
        if (!imagesDir.exists()) imagesDir.mkdirs();
        return new java.io.File(imagesDir, "crop_" + java.util.UUID.randomUUID().toString() + ".jpg");
    }

    private com.yalantis.ucrop.UCrop.Options createUCropOptions() {
        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setToolbarColor(androidx.core.content.ContextCompat.getColor(this, R.color.bg_soft));
        options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.bg_soft));
        options.setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(this, R.color.pink_primary));
        options.setCropFrameStrokeWidth(2);
        options.setCropGridRowCount(2);
        options.setCropGridColumnCount(2);
        options.setFreeStyleCropEnabled(false);
        options.setImageToCropBoundsAnimDuration(300);
        options.setHideBottomControls(true);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        return options;
    }
}
