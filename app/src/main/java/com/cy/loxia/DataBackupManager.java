package com.cy.loxia;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBackupManager {

    private static final String TAG = "DataBackupManager";

    private static final String[] STATUS_OPTIONS = {"待抢", "付意向", "付定金", "补尾款", "待发货", "已到手"};
    private static final String[] CHANNEL_OPTIONS = {"淘宝", "拼多多", "闲鱼", "其他"};
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*元?");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})");

    // New special patterns for upgraded parsing
    private static final Pattern FULL_PAYMENT_PATTERN = Pattern.compile("全款\\s*(\\d+(\\.\\d+)?)\\s*元?");
    private static final Pattern EARNEST_MONEY_PATTERN = Pattern.compile("意向金\\s*(\\d+(\\.\\d+)?)\\s*元?");
    private static final Pattern DEPOSIT_PATTERN = Pattern.compile("定金\\s*(\\d+(\\.\\d+)?)\\s*元?");
    private static final Pattern TAIL_PAYMENT_PATTERN = Pattern.compile("尾款\\s*(\\d+(\\.\\d+)?)\\s*元?");
    private static final Pattern SHIPPING_PATTERN = Pattern.compile("运费\\s*(\\d+(\\.\\d+)?)\\s*元?");
    // Per-status date patterns
    private static final Pattern DAIQIANG_DATE_PATTERN = Pattern.compile("待抢时间[：:]\\s*(\\d{4}[-./]\\d{1,2}[-./]\\d{1,2})");
    private static final Pattern YIXIANG_DATE_PATTERN = Pattern.compile("付意向时间[：:]\\s*(\\d{4}[-./]\\d{1,2}[-./]\\d{1,2})");
    private static final Pattern DINGJIN_DATE_PATTERN = Pattern.compile("付定金时间[：:]\\s*(\\d{4}[-./]\\d{1,2}[-./]\\d{1,2})");
    private static final Pattern BUWEIKUAN_DATE_PATTERN = Pattern.compile("补尾款时间[：:]\\s*(\\d{4}[-./]\\d{1,2}[-./]\\d{1,2})");
    private static final Pattern PINNED_PATTERN = Pattern.compile("置顶[：:]\\s*(是|true|yes|1)", Pattern.CASE_INSENSITIVE);

    public static class ExportResult {
        public final boolean success;
        public final String message;

        public ExportResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class ImportResult {
        public final int wardrobesAdded;
        public final int itemsAdded;
        public final int itemsUpdated;

        public ImportResult(int wardrobesAdded, int itemsAdded, int itemsUpdated) {
            this.wardrobesAdded = wardrobesAdded;
            this.itemsAdded = itemsAdded;
            this.itemsUpdated = itemsUpdated;
        }
    }

    public static ExportResult exportToJson(Context context, DataRepository repository) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("exportedAt", LocalDate.now().toString());

            // 使用 runBlocking 在后台线程调用 suspend 函数
            List<Wardrobe> wardrobes = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getWardrobesAsync(continuation)
            );
            JSONArray wardrobeArray = new JSONArray();
            for (Wardrobe w : wardrobes) {
                wardrobeArray.put(w.toJson());
            }
            root.put("wardrobes", wardrobeArray);

            List<DressItem> items = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getDressItemsAsync(continuation)
            );
            JSONArray itemArray = new JSONArray();
            for (DressItem item : items) {
                itemArray.put(item.toJson());
            }
            root.put("dressItems", itemArray);

            String fileName = "loxia_backup_" + LocalDate.now().toString() + ".json";
            String jsonContent = root.toString(2);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    return new ExportResult(false, "无法创建文件");
                }
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out == null) {
                        return new ExportResult(false, "无法写入文件");
                    }
                    out.write(jsonContent.getBytes("UTF-8"));
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(jsonContent.getBytes("UTF-8"));
                }
            }

            return new ExportResult(true, "已导出到 Downloads/" + fileName);
        } catch (Exception e) {
            return new ExportResult(false, "导出失败：" + e.getMessage());
        }
    }

    public static ImportResult importFromJson(Context context, Uri uri, DataRepository repository) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return new ImportResult(0, 0, 0);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject root = new JSONObject(sb.toString());

            // Version check
            int version = root.optInt("version", 0);
            if (version > 1) {
                Log.w("DataBackupManager", "Unsupported backup version: " + version);
                return new ImportResult(0, 0, 0);
            }

            // Merge wardrobes (使用 runBlocking 调用 suspend 函数)
            List<Wardrobe> existingWardrobes = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getWardrobesAsync(continuation)
            );
            Map<String, Wardrobe> wardrobeMap = new HashMap<>();
            for (Wardrobe w : existingWardrobes) {
                wardrobeMap.put(w.getId(), w);
            }

            int wardrobesAdded = 0;
            if (root.has("wardrobes")) {
                JSONArray arr = root.getJSONArray("wardrobes");
                for (int i = 0; i < arr.length(); i++) {
                    Wardrobe imported = Wardrobe.fromJson(arr.getJSONObject(i));
                    if (!wardrobeMap.containsKey(imported.getId())) {
                        wardrobeMap.put(imported.getId(), imported);
                        wardrobesAdded++;
                    }
                }
            }
            // Merge dress items
            List<DressItem> existingItems = kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> repository.getDressItemsAsync(continuation)
            );
            Map<String, DressItem> itemMap = new HashMap<>();
            for (DressItem item : existingItems) {
                itemMap.put(item.getId(), item);
            }

            // Build set of valid wardrobe IDs for foreign key validation
            java.util.Set<String> validWardrobeIds = new java.util.HashSet<>(wardrobeMap.keySet());
            String fallbackWardrobeId = validWardrobeIds.isEmpty() ? "" : validWardrobeIds.iterator().next();

            int itemsAdded = 0;
            int itemsUpdated = 0;
            if (root.has("dressItems")) {
                JSONArray arr = root.getJSONArray("dressItems");
                for (int i = 0; i < arr.length(); i++) {
                    DressItem imported = DressItem.fromJson(arr.getJSONObject(i));
                    // Skip items with empty name
                    if (imported.getName() == null || imported.getName().trim().isEmpty()) continue;
                    // Validate wardrobeId: if not valid, assign to first available wardrobe
                    if (imported.getWardrobeId().isEmpty() || !validWardrobeIds.contains(imported.getWardrobeId())) {
                        if (!fallbackWardrobeId.isEmpty()) {
                            imported.setWardrobeId(fallbackWardrobeId);
                        } else {
                            continue; // No valid wardrobe to assign to
                        }
                    }
                    if (itemMap.containsKey(imported.getId())) {
                        itemsUpdated++;
                    } else {
                        itemsAdded++;
                    }
                    itemMap.put(imported.getId(), imported);
                }
            }

            // 原子性写入：在 Room 事务中同时更新衣柜和裙子，防止中途失败导致数据不一致
            final List<Wardrobe> wardrobesToImport = new ArrayList<>(wardrobeMap.values());
            final List<DressItem> itemsToImport = new ArrayList<>(itemMap.values());
            final Throwable[] error = new Throwable[1];
            kotlinx.coroutines.BuildersKt.runBlocking(
                kotlinx.coroutines.Dispatchers.getIO(),
                (scope, continuation) -> {
                    try {
                        return repository.importInTransaction(wardrobesToImport, itemsToImport, continuation);
                    } catch (Throwable t) {
                        error[0] = t;
                        return null;
                    }
                }
            );
            if (error[0] != null) throw new RuntimeException(error[0]);

            return new ImportResult(wardrobesAdded, itemsAdded, itemsUpdated);
        } catch (Exception e) {
            Log.e("DataBackupManager", "Import failed", e);
            return new ImportResult(0, 0, 0);
        }
    }

    // 单次导入最大字符数限制（防止 OOM）
    private static final int MAX_IMPORT_TEXT_LENGTH = 100 * 1024; // 100KB

    /**
     * 解析纯文本导入
     * @param text 待解析的文本
     * @return 解析结果，如果解析失败返回空列表
     */
    public static List<DressItem> parsePlainText(String text) {
        List<DressItem> items = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return items;

        // 前置校验：限制文本长度，防止恶意超长文本导致 OOM
        if (text.length() > MAX_IMPORT_TEXT_LENGTH) {
            Log.e(TAG, "Import text too long: " + text.length() + " chars (max " + MAX_IMPORT_TEXT_LENGTH + ")");
            return items;
        }

        try {
            String[] lines = text.split("\\n");
            boolean isTableFormat = false;
            boolean isSpaceSeparatedFormat = false;

            // Detect format type
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                if (firstLine.contains("\t")) {
                    // Tab-separated table format
                    if (firstLine.contains("裙子名称") || firstLine.contains("品牌") || firstLine.contains("名称")) {
                        isTableFormat = true;
                    }
                } else if (firstLine.contains(" ")) {
                    // Check if it's space-separated format with "空" placeholder
                    // Count spaces to determine if it's the new format (11 fields = 10 spaces)
                    String[] tokens = firstLine.split("\\s+");
                    if (tokens.length >= 10 && (firstLine.contains("空") || matchesStatusKeyword(tokens[1]))) {
                        isSpaceSeparatedFormat = true;
                    }
                }
            }

            for (int i = 0; i < lines.length; i++) {
                try {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;

                    // Skip header line in table format
                    if (isTableFormat && i == 0) continue;
                    // Skip separator lines (---)
                    if (line.matches("^[-=\\t]+$")) continue;

                    DressItem item = null;
                    if (isTableFormat) {
                        item = parseTableLine(line);
                    } else if (isSpaceSeparatedFormat) {
                        item = parseSpaceSeparatedLine(line);
                    } else {
                        item = parseFreeTextLine(line);
                    }
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    // 单行解析失败，跳过该行继续解析下一行
                    Log.w(TAG, "Failed to parse line " + i + ": " + lines[i], e);
                }
            }
        } catch (Exception e) {
            // 整体解析失败，返回已解析的部分结果
            Log.e(TAG, "Failed to parse import text", e);
        }

        return items;
    }

    /**
     * Check if string matches a status keyword
     */
    private static boolean matchesStatusKeyword(String s) {
        if (s == null) return false;
        for (String status : STATUS_OPTIONS) {
            if (s.contains(status)) return true;
        }
        return s.contains("已拥有") || s.contains("待补") || s.contains("待入");
    }

    /**
     * Parse space-separated format line
     * Format: 衣服名称 衣服状态 店铺名称 品牌名称 购买渠道 购入时间 运费 意向金 定金 尾款 全款
     * Empty values are represented by "空"
     */
    private static DressItem parseSpaceSeparatedLine(String line) {
        try {
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) return null;

            String name = tokens[0].trim();
            if (name.isEmpty() || name.equals("空")) return null;

            String statusStr = tokens.length > 1 ? tokens[1].trim() : "";
            String store = tokens.length > 2 ? tokens[2].trim() : "";
            String brand = tokens.length > 3 ? tokens[3].trim() : ""; // 品牌名称
            String channel = tokens.length > 4 ? tokens[4].trim() : "";
            String dateStr = tokens.length > 5 ? tokens[5].trim() : "";
            String shippingStr = tokens.length > 6 ? tokens[6].trim() : "";
            String earnestStr = tokens.length > 7 ? tokens[7].trim() : "";
            String depositStr = tokens.length > 8 ? tokens[8].trim() : "";
            String tailStr = tokens.length > 9 ? tokens[9].trim() : "";
            String fullStr = tokens.length > 10 ? tokens[10].trim() : "";

            // Replace "空" with empty string
            store = "空".equals(store) ? "" : store;
            brand = "空".equals(brand) ? "" : brand;
            channel = "空".equals(channel) ? "" : channel;
            dateStr = "空".equals(dateStr) ? "" : dateStr;
            shippingStr = "空".equals(shippingStr) ? "" : shippingStr;
            earnestStr = "空".equals(earnestStr) ? "" : earnestStr;
            depositStr = "空".equals(depositStr) ? "" : depositStr;
            tailStr = "空".equals(tailStr) ? "" : tailStr;
            fullStr = "空".equals(fullStr) ? "" : fullStr;

            // Combine store and brand if both exist
            if (!store.isEmpty() && !brand.isEmpty() && !store.equals(brand)) {
                store = store + " " + brand;
            } else if (store.isEmpty() && !brand.isEmpty()) {
                store = brand;
            }

            // Parse status
            String status = parseStatus(statusStr);

            // Parse date
            String buyDate = extractDate(dateStr);

            // Parse amounts
            double earnestMoney = parseDouble(earnestStr);
            double deposit = parseDouble(depositStr);
            double tailPayment = parseDouble(tailStr);
            double fullAmount = parseDouble(fullStr);

            // Determine price and full payment status
            double price = 0;
            boolean isFullPayment = false;
            double fullPaymentAmount = 0;

            if (fullAmount > 0 && earnestMoney == 0 && tailPayment == 0) {
                // Only full amount specified
                price = fullAmount;
                isFullPayment = true;
                fullPaymentAmount = fullAmount;
            } else if (earnestMoney > 0 || tailPayment > 0) {
                // Has earnest/tail payment
                price = earnestMoney + tailPayment;
            } else if (fullAmount > 0) {
                price = fullAmount;
            }

            // Parse shipping
            String shippingFee = "包邮";
            if (!shippingStr.isEmpty()) {
                if (shippingStr.contains("包邮")) {
                    shippingFee = "包邮";
                } else {
                    shippingFee = shippingStr;
                }
            }

            if (buyDate.isEmpty()) buyDate = LocalDate.now().toString();
            if (channel.isEmpty()) channel = "其他";

            DressItem item = new DressItem(
                    UUID.randomUUID().toString(), "", name, store, channel,
                    price, buyDate, status, "", ""
            );
            item.setEarnestMoney(earnestMoney);
            item.setDeposit(deposit);
            item.setTailPayment(tailPayment);
            item.setFullPayment(isFullPayment);
            item.setFullPaymentAmount(fullPaymentAmount);
            item.setShippingFee(shippingFee);
            return item;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse space-separated line: " + line, e);
            return null;
        }
    }

    /**
     * Parse table format line (tab-separated)
     * Format: 名称	品牌	款式	色系	尺码	价格	渠道	图片	状态	时间	备注
     */
    private static DressItem parseTableLine(String line) {
        try {
            String[] cols = line.split("\\t");
            if (cols.length < 2) return null;

            String name = cols[0].trim();
            if (name.isEmpty()) return null;

            String store = cols.length > 1 ? cols[1].trim() : "";
            // cols[2] = 款式分类, cols[3] = 色系, cols[4] = 尺码 (skip these)
            String priceStr = cols.length > 5 ? cols[5].trim() : "";
            String channel = cols.length > 6 ? cols[6].trim() : "";
            // cols[7] = 实物图片 (skip)
            String statusStr = cols.length > 8 ? cols[8].trim() : "";
            String dateStr = cols.length > 9 ? cols[9].trim() : "";
            String remark = cols.length > 10 ? cols[10].trim() : "";

            // Parse price and extract earnestMoney/tailPayment
            double price = 0;
            double earnestMoney = 0;
            double tailPayment = 0;
            boolean isFullPayment = false;
            double fullPaymentAmount = 0;

            // Match patterns like ¥377（定金131、尾款246） or ¥199
            // Also handle ¥377(定金131 尾款246) format
            Pattern complexPricePattern = Pattern.compile("¥?(\\d+(?:\\.\\d+)?)(?:[（(]\\s*定金\\s*(\\d+(?:\\.\\d+)?)\\s*[、,，]\\s*尾款\\s*(\\d+(?:\\.\\d+)?))\\s*[）)]?");
            Matcher priceMatcher = complexPricePattern.matcher(priceStr);
            if (priceMatcher.find()) {
                price = parseDouble(priceMatcher.group(1));
                if (priceMatcher.group(2) != null) {
                    earnestMoney = parseDouble(priceMatcher.group(2));
                }
                if (priceMatcher.group(3) != null) {
                    tailPayment = parseDouble(priceMatcher.group(3));
                }
            } else {
                // Try simple price pattern
                Matcher simplePrice = PRICE_PATTERN.matcher(priceStr);
                if (simplePrice.find()) {
                    price = parseDouble(simplePrice.group(1));
                }
            }

            // Check for full payment
            if (priceStr.contains("全款")) {
                isFullPayment = true;
                fullPaymentAmount = price;
            }

            // Parse status
            String status = parseStatus(statusStr);

            // Parse date
            String buyDate = extractDate(dateStr);

            // Parse channel
            if (channel.isEmpty() || channel.equals("未标注")) {
                channel = "其他";
            } else {
                boolean found = false;
                for (String c : CHANNEL_OPTIONS) {
                    if (channel.contains(c)) {
                        channel = c;
                        found = true;
                        break;
                    }
                }
                if (!found) channel = "其他";
            }

            if (buyDate.isEmpty()) buyDate = LocalDate.now().toString();

            DressItem item = new DressItem(
                    UUID.randomUUID().toString(), "", name, store, channel,
                    price, buyDate, status, "", remark
            );
            item.setEarnestMoney(earnestMoney);
            item.setTailPayment(tailPayment);
            item.setFullPayment(isFullPayment);
            item.setFullPaymentAmount(fullPaymentAmount);

            return item;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse table line: " + line, e);
            return null;
        }
    }

    /**
     * Parse status string to match app's status options
     */
    private static String parseStatus(String statusStr) {
        if (statusStr.isEmpty()) return "付意向";

        // Direct matches
        if (statusStr.contains("已到手") || statusStr.contains("全款到手") || statusStr.contains("已收到")) {
            return "已到手";
        }
        if (statusStr.contains("待补尾款") || statusStr.contains("补尾款")) {
            return "补尾款";
        }
        if (statusStr.contains("待发货")) {
            return "待发货";
        }
        if (statusStr.contains("待抢")) {
            return "待抢";
        }
        if (statusStr.contains("付意向") || statusStr.contains("待入金") || statusStr.contains("待付")) {
            return "付意向";
        }
        if (statusStr.contains("付定金") || statusStr.contains("已下单") || statusStr.contains("已付款")) {
            return "付定金";
        }

        // Check STATUS_OPTIONS
        for (String s : STATUS_OPTIONS) {
            if (statusStr.contains(s)) {
                return s;
            }
        }

        return "付意向";
    }

    /**
     * Extract date from string, support various formats
     */
    private static String extractDate(String dateStr) {
        if (dateStr.isEmpty() || dateStr.contains("未标注")) return "";

        Matcher dateMatcher = DATE_PATTERN.matcher(dateStr);
        if (dateMatcher.find()) {
            try {
                String y = dateMatcher.group(1);
                String m = String.format("%02d", Integer.parseInt(dateMatcher.group(2)));
                String d = String.format("%02d", Integer.parseInt(dateMatcher.group(3)));
                return y + "-" + m + "-" + d;
            } catch (Exception ignored) {}
        }
        return "";
    }

    /**
     * Parse free text format line (original format)
     */
    private static DressItem parseFreeTextLine(String line) {
        try {
            String[] tokens = line.split("[\\s，,、\\t]+");
            if (tokens.length == 0) return null;

            double price = 0;
            String buyDate = "";
            String status = "";
            String channel = "";
            double earnestMoney = 0;
            double deposit = 0;
            double tailPayment = 0;
            double fullPaymentAmount = 0;
            boolean isFullPayment = false;
            String shippingFee = "包邮";
            List<String> remaining = new ArrayList<>();

            // First pass: check the whole line for special patterns
            Matcher fullM = FULL_PAYMENT_PATTERN.matcher(line);
            if (fullM.find()) {
                isFullPayment = true;
                fullPaymentAmount = parseDouble(fullM.group(1));
            }

            Matcher earnestM = EARNEST_MONEY_PATTERN.matcher(line);
            if (earnestM.find()) {
                earnestMoney = parseDouble(earnestM.group(1));
            }

            Matcher depositM = DEPOSIT_PATTERN.matcher(line);
            if (depositM.find()) {
                deposit = parseDouble(depositM.group(1));
            }

            Matcher tailM = TAIL_PAYMENT_PATTERN.matcher(line);
            if (tailM.find()) {
                tailPayment = parseDouble(tailM.group(1));
            }

            Matcher shipM = SHIPPING_PATTERN.matcher(line);
            if (shipM.find()) {
                shippingFee = "运费" + shipM.group(1) + "元";
            }
            if (line.contains("包邮")) {
                shippingFee = "包邮";
            }

            for (String token : tokens) {
                token = token.trim();
                if (token.isEmpty()) continue;

                if (FULL_PAYMENT_PATTERN.matcher(token).find() ||
                    EARNEST_MONEY_PATTERN.matcher(token).find() ||
                    DEPOSIT_PATTERN.matcher(token).find() ||
                    TAIL_PAYMENT_PATTERN.matcher(token).find() ||
                    SHIPPING_PATTERN.matcher(token).find()) {
                    continue;
                }
                if (token.equals("包邮")) continue;

                if (price == 0) {
                    Matcher priceMatcher = PRICE_PATTERN.matcher(token);
                    if (priceMatcher.find()) {
                        price = parseDouble(priceMatcher.group(1));
                        continue;
                    }
                }

                if (buyDate.isEmpty()) {
                    Matcher dateMatcher = DATE_PATTERN.matcher(token);
                    if (dateMatcher.find()) {
                        buyDate = extractDate(token);
                        continue;
                    }
                }

                if (status.isEmpty()) {
                    for (String s : STATUS_OPTIONS) {
                        if (token.contains(s)) {
                            status = s;
                            break;
                        }
                    }
                    if (!status.isEmpty()) continue;
                    if (token.contains("已下单") || token.contains("已付款")) {
                        status = "付定金";
                        continue;
                    }
                    if (token.contains("已到手") || token.contains("已收到")) {
                        status = "已到手";
                        continue;
                    }
                }

                if (channel.isEmpty()) {
                    for (String c : CHANNEL_OPTIONS) {
                        if (token.contains(c)) {
                            channel = c;
                            break;
                        }
                    }
                    if (!channel.isEmpty()) continue;
                }

                remaining.add(token);
            }

            if (remaining.isEmpty()) return null;

            String name = remaining.get(0);
            String store = remaining.size() > 1 ? remaining.get(1) : "";

            if (status.isEmpty()) status = "付意向";
            if (channel.isEmpty()) channel = "其他";
            if (buyDate.isEmpty()) buyDate = LocalDate.now().toString();

            if (price == 0 && isFullPayment && fullPaymentAmount > 0) {
                price = fullPaymentAmount;
            }

            String daiqiangDate = extractStatusDate(DAIQIANG_DATE_PATTERN, line);
            String yixiangDate = extractStatusDate(YIXIANG_DATE_PATTERN, line);
            String dingjinDate = extractStatusDate(DINGJIN_DATE_PATTERN, line);
            String buweikuanDate = extractStatusDate(BUWEIKUAN_DATE_PATTERN, line);
            boolean pinned = PINNED_PATTERN.matcher(line).find();

            DressItem item = new DressItem(
                    UUID.randomUUID().toString(), "", name, store, channel,
                    price, buyDate, status, "", ""
            );
            item.setEarnestMoney(earnestMoney);
            item.setDeposit(deposit);
            item.setTailPayment(tailPayment);
            item.setFullPayment(isFullPayment);
            item.setFullPaymentAmount(fullPaymentAmount);
            item.setShippingFee(shippingFee);
            item.setDaiqiangDate(daiqiangDate);
            item.setYixiangDate(yixiangDate);
            item.setDingjinDate(dingjinDate);
            item.setBuweikuanDate(buweikuanDate);
            if (buweikuanDate.isEmpty()) {
                item.setTailPaymentDate("");
            }
            item.setPinned(pinned);
            return item;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse free text line: " + line, e);
            return null;
        }
    }

    private static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extractStatusDate(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return "";
        try {
            String y = m.group(1);
            String[] parts = y.split("[-./]");
            return parts[0] + "-" + String.format("%02d", Integer.parseInt(parts[1])) + "-" + String.format("%02d", Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return "";
        }
    }
}
