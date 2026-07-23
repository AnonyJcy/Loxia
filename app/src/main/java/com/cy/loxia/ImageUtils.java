package com.cy.loxia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    // 内存缓存：最大可用内存的 1/6（更保守，防止 OOM）
    private static final int MAX_CACHE_KB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 6);
    private static final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(MAX_CACHE_KB) {
        @Override protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
        @Override protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if (evicted && oldValue != null && !oldValue.isRecycled()) {
                oldValue.recycle();
            }
        }
    };

    // 磁盘缓存目录
    private static File diskCacheDir;
    private static final long DISK_CACHE_MAX_SIZE = 50 * 1024 * 1024; // 50MB

    // 缩略图尺寸（更小，节省内存）
    private static final int THUMB_MAX_W = 320;
    private static final int THUMB_MAX_H = 320;

    // 专用 tag key，避免覆盖 View 已有的 tag
    private static final int TAG_KEY_LOADING_PATH = 0xFF000001;

    // 双线程后台解码器（平衡并发与内存峰值）
    private static final ExecutorService decodeExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ImageDecode");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 初始化磁盘缓存目录（在 Application.onCreate 中调用）
     */
    public static void initDiskCache(Context context) {
        diskCacheDir = new File(context.getCacheDir(), "image_cache");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        // 清理过期缓存
        cleanExpiredCache();
    }

    /**
     * 异步加载图片到 ImageView，使用 setTag 防止错图
     */
    public static void loadIntoView(Context context, String path, ImageView imageView, int placeholderRes) {
        if (path == null || path.isEmpty()) {
            imageView.setImageResource(placeholderRes);
            return;
        }

        // 内存缓存命中 → 直接设置
        Bitmap cached = bitmapCache.get(path);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            return;
        }

        // 标记当前 ImageView 正在加载的路径，防止错图（使用专用 key 避免覆盖其他 tag）
        imageView.setTag(TAG_KEY_LOADING_PATH, path);
        imageView.setImageResource(placeholderRes);

        // 后台解码 — 使用 ApplicationContext 避免 Activity 泄漏
        Context appContext = context.getApplicationContext();
        decodeExecutor.execute(() -> {
            // 在后台线程再做一次缓存检查（可能在排队期间被其他请求加载了）
            Bitmap bitmap = bitmapCache.get(path);
            if (bitmap == null || bitmap.isRecycled()) {
                // 先尝试磁盘缓存
                bitmap = loadFromDiskCache(path);
                if (bitmap == null || bitmap.isRecycled()) {
                    bitmap = decodeBitmap(appContext, path);
                    // 保存到磁盘缓存
                    if (bitmap != null) {
                        saveToDiskCache(path, bitmap);
                    }
                }
            }

            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                // 移除 isAttachedToWindow 检查，因为 RecyclerView onBindViewHolder 时可能还未 attach
                // 检查 ImageView 的 tag 是否仍然匹配，防止错图
                Object tag = imageView.getTag(TAG_KEY_LOADING_PATH);
                if (tag == null || !tag.equals(path)) return;

                if (finalBitmap != null) {
                    bitmapCache.put(path, finalBitmap);
                    android.graphics.drawable.Drawable oldDrawable = imageView.getDrawable();
                    if (oldDrawable != null) {
                        android.graphics.drawable.BitmapDrawable newDrawable =
                                new android.graphics.drawable.BitmapDrawable(imageView.getResources(), finalBitmap);
                        android.graphics.drawable.TransitionDrawable td =
                                new android.graphics.drawable.TransitionDrawable(
                                        new android.graphics.drawable.Drawable[]{oldDrawable, newDrawable});
                        td.setCrossFadeEnabled(true);
                        imageView.setImageDrawable(td);
                        td.startTransition(150);
                    } else {
                        imageView.setImageBitmap(finalBitmap);
                    }
                } else {
                    imageView.setImageResource(placeholderRes);
                }
            });
        });
    }

    /**
     * 同步解码（用于需要立即拿到 Bitmap 的场景，如裁剪回调）
     */
    public static Bitmap decodeBitmap(Context context, String path) {
        if (path == null || path.isEmpty()) return null;

        // Try as file path first
        File file = new File(path);
        if (file.exists()) {
            return decodeSampledFile(path, THUMB_MAX_W, THUMB_MAX_H);
        }

        // Try as content URI
        try {
            Uri uri = Uri.parse(path);
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    return decodeSampledStream(in, THUMB_MAX_W, THUMB_MAX_H);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to decode: " + path, e);
        }

        return null;
    }

    /**
     * 从磁盘缓存加载 Bitmap
     */
    private static Bitmap loadFromDiskCache(String path) {
        if (diskCacheDir == null) return null;
        String hash = md5(path);
        File cacheFile = new File(diskCacheDir, hash);
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                return decodeSampledFile(cacheFile.getAbsolutePath(), THUMB_MAX_W, THUMB_MAX_H);
            } catch (Exception e) {
                cacheFile.delete();
            }
        }
        return null;
    }

    /**
     * 保存 Bitmap 到磁盘缓存
     */
    private static void saveToDiskCache(String path, Bitmap bitmap) {
        if (diskCacheDir == null || bitmap == null) return;
        String hash = md5(path);
        File cacheFile = new File(diskCacheDir, hash);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (Exception e) {
            Log.w(TAG, "Failed to save to disk cache", e);
        }
    }

    /**
     * 清理过期的磁盘缓存（超过 7 天）
     */
    private static void cleanExpiredCache() {
        if (diskCacheDir == null || !diskCacheDir.exists()) return;
        long expireTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        File[] files = diskCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < expireTime) {
                    file.delete();
                }
            }
        }
    }

    /**
     * 清理所有缓存
     */
    public static void clearCache() {
        bitmapCache.evictAll();
        if (diskCacheDir != null && diskCacheDir.exists()) {
            File[] files = diskCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * 计算字符串的 MD5 哈希值
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static Bitmap decodeSampledFile(String filePath, int reqW, int reqH) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH);
        opts.inJustDecodeBounds = false;
        // 使用 RGB_565 节省内存（无需透明通道，内存减半）
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(filePath, opts);
    }

    private static Bitmap decodeSampledStream(InputStream in, int reqW, int reqH) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        try {
            byte[] headerBytes = new byte[32 * 1024];
            java.io.ByteArrayOutputStream headerOut = new java.io.ByteArrayOutputStream();
            int totalRead = 0;
            int bytesRead;
            byte[] buffer = new byte[8192];
            while (totalRead < headerBytes.length && (bytesRead = in.read(buffer)) != -1) {
                headerOut.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            byte[] data = headerOut.toByteArray();
            BitmapFactory.decodeByteArray(data, 0, data.length, opts);

            opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH);
            opts.inJustDecodeBounds = false;
            try (java.io.SequenceInputStream joined = new java.io.SequenceInputStream(
                    new java.io.ByteArrayInputStream(data), in)) {
                return BitmapFactory.decodeStream(joined, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options opts, int reqW, int reqH) {
        int h = opts.outHeight;
        int w = opts.outWidth;
        int inSampleSize = 1;
        if (h > reqH || w > reqW) {
            int halfH = h / 2;
            int halfW = w / 2;
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * 公共方法：采样解码图片
     * @param filePath 图片路径
     * @param reqW 目标宽度
     * @param reqH 目标高度
     * @return 采样后的 Bitmap
     */
    public static Bitmap decodeSampledBitmap(String filePath, int reqW, int reqH) {
        return decodeSampledFile(filePath, reqW, reqH);
    }
}
