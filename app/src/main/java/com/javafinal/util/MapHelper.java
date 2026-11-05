package com.javafinal.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.javafinal.R;
import com.javafinal.model.SosRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.PolygonOptions;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.maps.UiSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapHelper {

    public static final int LAYER_STREET_OSM = 0;
    public static final int LAYER_VECTOR_OPENFREEMAP = 1;
    public static final int LAYER_SATELLITE_ESRI = 2;
    public static final int LAYER_DARK_TACTICAL = 3;

    private static int currentLayerIndex = LAYER_STREET_OSM;
    private static Context appContext;

    private static final ExecutorService ROUTE_EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<String, List<double[]>> ROUTE_CACHE = new ConcurrentHashMap<>();

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build();

    // =========================================================================
    // ĐỒ THỊ ĐƯỜNG GIAO THÔNG THỰC TẾ QUẬN 1, TP.HCM (Dùng tức thời & Offline)
    // Toàn bộ tọa độ là các ngã tư thực tế trên trục Trần Hưng Đạo, Lê Lợi,
    // Nguyễn Thái Học, Pasteur, Nam Kỳ Khởi Nghĩa, Hai Bà Trưng, Đồng Khởi,
    // Lý Tự Trọng, Lê Thánh Tôn, Nguyễn Thị Minh Khai, Đinh Tiên Hoàng, Cô Giang.
    // =========================================================================
    private static final double[][] ROAD_NODES = new double[][]{
            {10.76802, 106.69536}, // 0: Ngã tư Trần Hưng Đạo - Nguyễn Thái Học (Trụ sở Đội cứu hộ)
            {10.76975, 106.69682}, // 1: Trần Hưng Đạo - Ký Con
            {10.77110, 106.69795}, // 2: Công trường Quách Thị Trang (Trần Hưng Đạo - Lê Lợi - Hàm Nghi)
            {10.77205, 106.69918}, // 3: Lê Lợi - Nam Kỳ Khởi Nghĩa
            {10.77456, 106.70042}, // 4: Lê Lợi - Pasteur
            {10.77605, 106.70198}, // 5: Lê Lợi - Nguyễn Huệ / Đồng Khởi
            {10.77745, 106.70395}, // 6: Đồng Khởi - Lê Thánh Tôn
            {10.77825, 106.70342}, // 7: Lê Thánh Tôn - Hai Bà Trưng (Vị trí SOS 1)
            {10.77735, 106.69758}, // 8: Pasteur - Lý Tự Trọng
            {10.77842, 106.69645}, // 9: Pasteur - Lê Thánh Tôn
            {10.78115, 106.69362}, // 10: Pasteur - Nguyễn Thị Minh Khai
            {10.78385, 106.69725}, // 11: Nguyễn Thị Minh Khai - Hai Bà Trưng
            {10.78535, 106.69920}, // 12: Nguyễn Thị Minh Khai - Mạc Đĩnh Chi
            {10.78685, 106.70115}, // 13: Nguyễn Thị Minh Khai - Đinh Tiên Hoàng (Vị trí SOS 2)
            {10.76625, 106.69695}, // 14: Nguyễn Thái Học - Cô Giang
            {10.76415, 106.69415}, // 15: Cô Giang - Đề Thám
            {10.76325, 106.69285}, // 16: Cô Giang - Hồ Hảo Hớn (Vị trí SOS 3)
            {10.76975, 106.69365}, // 17: Nguyễn Thái Học - Phạm Ngũ Lão
            {10.77125, 106.69215}, // 18: Vòng xoay Phù Đổng (Cách Mạng Tháng Tám - Nguyễn Trãi)
            {10.77265, 106.69010}, // 19: Nguyễn Thị Minh Khai - Tôn Thất Tùng
            {10.77385, 106.68825}, // 20: Nguyễn Thị Minh Khai - Cống Quỳnh (Vị trí SOS 4)
            {10.77550, 106.69520}, // 21: Lý Tự Trọng - Trương Định
            {10.77980, 106.69980}, // 22: Hai Bà Trưng - Nguyễn Du
            {10.78220, 106.70480}  // 23: Lê Thánh Tôn - Tôn Đức Thắng
    };

    private static final int[][] ROAD_EDGES = new int[][]{
            {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7},
            {4, 8}, {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13},
            {0, 14}, {14, 15}, {15, 16},
            {0, 17}, {17, 18}, {18, 19}, {19, 20},
            {2, 18}, {18, 21}, {21, 8}, {9, 22}, {22, 7}, {22, 11},
            {7, 23}, {23, 13}
    };

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    public static void initMapLibre(Context context) {
        appContext = context.getApplicationContext();
        MapLibre.getInstance(appContext);
    }

    public static String getRasterStyleJson(String tileUrl, String bgHex) {
        return "{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"RescueSOS Map\",\n" +
                "  \"sources\": {\n" +
                "    \"raster-tiles\": {\n" +
                "      \"type\": \"raster\",\n" +
                "      \"tiles\": [\n" +
                "        \"" + tileUrl + "\"\n" +
                "      ],\n" +
                "      \"tileSize\": 256,\n" +
                "      \"attribution\": \"© OpenStreetMap\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"background\",\n" +
                "      \"type\": \"background\",\n" +
                "      \"paint\": {\n" +
                "        \"background-color\": \"" + bgHex + "\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"simple-tiles\",\n" +
                "      \"type\": \"raster\",\n" +
                "      \"source\": \"raster-tiles\",\n" +
                "      \"minzoom\": 0,\n" +
                "      \"maxzoom\": 20\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public static void applyMapStyle(MapLibreMap map, int layerIndex, Style.OnStyleLoaded callback) {
        if (map == null) return;
        UiSettings ui = map.getUiSettings();
        ui.setLogoEnabled(false);
        ui.setAttributionEnabled(false);
        ui.setCompassEnabled(false);
        setupMinimalInfoWindow(map);
        currentLayerIndex = layerIndex;
        Style.Builder builder = new Style.Builder();
        switch (layerIndex) {
            case LAYER_VECTOR_OPENFREEMAP:
                builder.fromUri("https://tiles.openfreemap.org/styles/liberty");
                break;
            case LAYER_SATELLITE_ESRI:
                builder.fromJson(getRasterStyleJson(
                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
                        "#132232"
                ));
                break;
            case LAYER_DARK_TACTICAL:
                builder.fromJson(getRasterStyleJson(
                        "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png?key=cb1_52ik_1_1ef32e1ab2c6e505e4c47879",
                        "#14161A"
                ));
                break;
            case LAYER_STREET_OSM:
            default:
                builder.fromJson(getRasterStyleJson(
                        "https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png?key=cb1_52ik_1_1ef32e1ab2c6e505e4c47879",
                        "#F4F4F6"
                ));
                break;
        }
        if (callback != null) {
            map.setStyle(builder, callback);
        } else {
            map.setStyle(builder);
        }
    }

    public static void applyDefaultStyle(MapLibreMap map, Style.OnStyleLoaded callback) {
        applyMapStyle(map, currentLayerIndex, callback);
    }

    private static void setupMinimalInfoWindow(MapLibreMap map) {
        if (appContext == null) return;
        map.setInfoWindowAdapter(marker -> {
            LinearLayout box = new LinearLayout(appContext);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundResource(R.drawable.bg_info_window);
            box.setPadding(dp(appContext, 12), dp(appContext, 8), dp(appContext, 12), dp(appContext, 8));

            if (marker.getTitle() != null && !marker.getTitle().isEmpty()) {
                TextView title = new TextView(appContext);
                title.setText(marker.getTitle());
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                title.setTextColor(Color.parseColor("#111111"));
                box.addView(title);
            }
            if (marker.getSnippet() != null && !marker.getSnippet().isEmpty()) {
                TextView sub = new TextView(appContext);
                sub.setText(marker.getSnippet());
                sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                sub.setTextColor(Color.parseColor("#6B6B70"));
                box.addView(sub);
            }
            return box;
        });
    }

    public static void showLayerSwitchDialog(Context context, MapLibreMap map, Runnable onReloadAnnotations) {
        if (map == null) {
            if (context instanceof Activity) {
                NavigationHelper.showSnack((Activity) context, "Bản đồ đang khởi tạo...");
            }
            return;
        }
        String[][] options = new String[][]{
                {"Đường phố", "Mặc định"},
                {"Bản đồ 3D", "Vector"},
                {"Vệ tinh", "Thực địa"},
                {"Giao diện tối", "Ban đêm"}
        };

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout root = NavigationHelper.createSheetRoot(
                context,
                null,
                "Kiểu bản đồ",
                null
        );

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundResource(R.drawable.bg_box);
        list.setPadding(dp(context, 14), dp(context, 4), dp(context, 14), dp(context, 4));
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        listLp.topMargin = dp(context, 16);
        listLp.bottomMargin = dp(context, 8);
        list.setLayoutParams(listLp);

        for (int i = 0; i < options.length; i++) {
            final int which = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(context, 4), dp(context, 12), dp(context, 4), dp(context, 12));

            LinearLayout col = new LinearLayout(context);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t1 = new TextView(context);
            t1.setText(options[i][0]);
            t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t1.setTextColor(ContextCompat.getColor(context, R.color.ink));
            col.addView(t1);

            TextView t2 = new TextView(context);
            t2.setText(options[i][1]);
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t2.setTextColor(ContextCompat.getColor(context, R.color.ink_tertiary));
            col.addView(t2);

            row.addView(col);

            if (i == currentLayerIndex) {
                TextView activeTag = new TextView(context);
                activeTag.setText("Đang dùng");
                activeTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                activeTag.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                activeTag.setTextColor(ContextCompat.getColor(context, R.color.white));
                activeTag.setBackgroundResource(R.drawable.bg_pill);
                activeTag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ink)));
                activeTag.setPadding(dp(context, 10), dp(context, 3), dp(context, 10), dp(context, 3));
                row.addView(activeTag);
            }

            row.setOnClickListener(v -> {
                dialog.dismiss();
                applyMapStyle(map, which, style -> {
                    if (onReloadAnnotations != null) {
                        onReloadAnnotations.run();
                    }
                });
            });
            list.addView(row);

            if (i < options.length - 1) {
                View div = new View(context);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)));
                div.setBackgroundColor(ContextCompat.getColor(context, R.color.line));
                list.addView(div);
            }
        }

        root.addView(list);

        MaterialButton btnClose = NavigationHelper.createSheetButton(context, "Đóng", false, false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        dialog.show();
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void enableScrollViewTouch(MapView mapView) {
        if (mapView == null) return;
        mapView.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        parent.requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        parent.requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }
            return false;
        });
    }

    public static Icon createPinIcon(Context context, int fillColor, String badgeText) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(42 * density);
        int height = Math.round(32 * density);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(40, 0, 0, 0));

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(fillColor);
        bgPaint.setStyle(Paint.Style.FILL);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.8f * density);

        float pillLeft = 2.5f * density;
        float pillTop = 2f * density;
        float pillRight = width - 2.5f * density;
        float pillBottom = height - 7.5f * density;
        float corner = (pillBottom - pillTop) / 2f;

        RectF shadowRect = new RectF(pillLeft, pillTop + 1.2f * density, pillRight, pillBottom + 1.2f * density);
        canvas.drawRoundRect(shadowRect, corner, corner, shadowPaint);

        Path pointer = new Path();
        float centerX = width / 2f;
        pointer.moveTo(centerX - 4 * density, pillBottom - 1f * density);
        pointer.lineTo(centerX + 4 * density, pillBottom - 1f * density);
        pointer.lineTo(centerX, height - 1.5f * density);
        pointer.close();
        canvas.drawPath(pointer, bgPaint);

        RectF rect = new RectF(pillLeft, pillTop, pillRight, pillBottom);
        canvas.drawRoundRect(rect, corner, corner, bgPaint);
        canvas.drawRoundRect(rect, corner, corner, strokePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(9.5f * density);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(badgeText, rect.centerX(), textY, textPaint);

        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }

    public static void moveCamera(MapLibreMap map, double lat, double lng, double zoom) {
        if (map == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoom), 750);
    }

    /**
     * Tìm lộ trình ngắn nhất theo mạng lưới đường phố thực tế (Dijkstra) khi chờ OSRM hoặc mất mạng.
     */
    public static List<double[]> computeStreetGraphRoute(double startLat, double startLng, double endLat, double endLng) {
        int n = ROAD_NODES.length;
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            dist[i][i] = 0;
        }
        for (int[] edge : ROAD_EDGES) {
            int u = edge[0];
            int v = edge[1];
            double w = SosRequest.computeHaversineKm(
                    ROAD_NODES[u][0], ROAD_NODES[u][1],
                    ROAD_NODES[v][0], ROAD_NODES[v][1]
            );
            dist[u][v] = w;
            dist[v][u] = w;
        }

        int startNode = findNearestRoadNode(startLat, startLng);
        int endNode = findNearestRoadNode(endLat, endLng);

        double[] minDist = new double[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(minDist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        minDist[startNode] = 0;

        for (int step = 0; step < n; step++) {
            int u = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                if (!visited[i] && minDist[i] < best) {
                    best = minDist[i];
                    u = i;
                }
            }
            if (u == -1 || u == endNode) break;
            visited[u] = true;
            for (int v = 0; v < n; v++) {
                if (!visited[v] && dist[u][v] < Double.POSITIVE_INFINITY) {
                    double alt = minDist[u] + dist[u][v];
                    if (alt < minDist[v]) {
                        minDist[v] = alt;
                        prev[v] = u;
                    }
                }
            }
        }

        List<double[]> path = new ArrayList<>();
        path.add(new double[]{startLat, startLng});

        List<Integer> nodeChain = new ArrayList<>();
        int curr = endNode;
        while (curr != -1) {
            nodeChain.add(0, curr);
            curr = prev[curr];
        }

        for (int idx : nodeChain) {
            double[] pt = ROAD_NODES[idx];
            if (SosRequest.computeHaversineKm(startLat, startLng, pt[0], pt[1]) > 0.03
                    && SosRequest.computeHaversineKm(endLat, endLng, pt[0], pt[1]) > 0.03) {
                path.add(new double[]{pt[0], pt[1]});
            }
        }

        path.add(new double[]{endLat, endLng});
        return subdividePathForSmoothTracking(path);
    }

    private static int findNearestRoadNode(double lat, double lng) {
        int bestIdx = 0;
        double bestDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ROAD_NODES.length; i++) {
            double d = SosRequest.computeHaversineKm(lat, lng, ROAD_NODES[i][0], ROAD_NODES[i][1]);
            if (d < bestDist) {
                bestDist = d;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    /**
     * Chia nhỏ các đoạn đường thẳng trên trục phố thành các điểm cách nhau ~80m
     * để đội cứu hộ di chuyển mượt mà dọc theo mặt đường.
     */
    private static List<double[]> subdividePathForSmoothTracking(List<double[]> raw) {
        if (raw.size() < 2) return raw;
        List<double[]> result = new ArrayList<>();
        for (int i = 0; i < raw.size() - 1; i++) {
            double[] a = raw.get(i);
            double[] b = raw.get(i + 1);
            result.add(a);
            double segKm = SosRequest.computeHaversineKm(a[0], a[1], b[0], b[1]);
            int parts = Math.max(1, (int) Math.ceil(segKm / 0.08));
            for (int p = 1; p < parts; p++) {
                double t = (double) p / parts;
                result.add(new double[]{
                        a[0] + (b[0] - a[0]) * t,
                        a[1] + (b[1] - a[1]) * t
                });
            }
        }
        result.add(raw.get(raw.size() - 1));
        return result;
    }

    /**
     * Vẽ lộ trình đường đi thực tế cho một SosRequest (kết hợp OSRM Routing API + Đồ thị đường phố).
     */
    public static void drawRescueRoute(
            Context context,
            MapLibreMap map,
            SosRequest sos,
             double fallbackStartLat,
            double fallbackStartLng,
            String rescuerTitle,
            String victimTitle,
            Runnable onRouteUpdated
    ) {
        if (map == null || sos == null) return;

        double startLat = sos.getTeamId() != null ? sos.getRescuerLat() : fallbackStartLat;
        double startLng = sos.getTeamId() != null ? sos.getRescuerLng() : fallbackStartLng;
        double endLat = sos.getVictimLat();
        double endLng = sos.getVictimLng();

        if (sos.getRoutePoints().isEmpty()) {
            String key = routeCacheKey( fallbackStartLat, fallbackStartLng, endLat, endLng);
            List<double[]> cached = ROUTE_CACHE.get(key);
            if (cached != null && !cached.isEmpty()) {
                sos.setRoutePoints(cached, true);
            } else {
                sos.setRoutePoints(computeStreetGraphRoute(startLat, startLng, endLat, endLng), false);
            }
        }

        List<double[]> remaining = sos.getRemainingRoutePoints();
        renderRouteOverlay(context, map, remaining, startLat, startLng, endLat, endLng, rescuerTitle, victimTitle);

        if (!sos.isOsrmLoaded()) {
            double originLat = fallbackStartLat;
            double originLng = fallbackStartLng;
            fetchOsrmRouteAsync(originLat, originLng, endLat, endLng, osrmPoints -> {
                if (osrmPoints != null && osrmPoints.size() >= 2) {
                    sos.setRoutePoints(osrmPoints, true);
                    map.clear();
                    List<double[]> updatedRemaining = sos.getRemainingRoutePoints();
                    double curLat = sos.getTeamId() != null ? sos.getRescuerLat() : originLat;
                    double curLng = sos.getTeamId() != null ? sos.getRescuerLng() : originLng;
                    renderRouteOverlay(context, map, updatedRemaining, curLat, curLng, endLat, endLng, rescuerTitle, victimTitle);
                    if (onRouteUpdated != null) {
                        onRouteUpdated.run();
                    }
                }
            });
        }
    }

    /**
     * Giữ hàm tương thích cho các lời gọi truyền tọa độ trực tiếp.
     */
    public static void drawRescueRoute(
            Context context,
            MapLibreMap map,
            double rescuerLat,
            double rescuerLng,
            double victimLat,
            double victimLng,
            String rescuerTitle,
            String victimTitle
    ) {
        if (map == null) return;
        String key = routeCacheKey(rescuerLat, rescuerLng, victimLat, victimLng);
        List<double[]> route = ROUTE_CACHE.get(key);
        if (route == null || route.isEmpty()) {
            route = computeStreetGraphRoute(rescuerLat, rescuerLng, victimLat, victimLng);
        }
        renderRouteOverlay(context, map, route, rescuerLat, rescuerLng, victimLat, victimLng, rescuerTitle, victimTitle);

        if (!ROUTE_CACHE.containsKey(key)) {
            fetchOsrmRouteAsync(rescuerLat, rescuerLng, victimLat, victimLng, osrmPoints -> {
                if (osrmPoints != null && osrmPoints.size() >= 2) {
                    map.clear();
                    renderRouteOverlay(context, map, osrmPoints, rescuerLat, rescuerLng, victimLat, victimLng, rescuerTitle, victimTitle);
                }
            });
        }
    }

    private static void renderRouteOverlay(
            Context context,
            MapLibreMap map,
            List<double[]> routePoints,
            double rescuerLat,
            double rescuerLng,
            double victimLat,
            double victimLng,
            String rescuerTitle,
            String victimTitle
    ) {
        List<LatLng> latLngList = new ArrayList<>();
        if (routePoints != null && !routePoints.isEmpty()) {
            for (double[] pt : routePoints) {
                latLngList.add(new LatLng(pt[0], pt[1]));
            }
        } else {
            latLngList.add(new LatLng(rescuerLat, rescuerLng));
            latLngList.add(new LatLng(victimLat, victimLng));
        }

        // Vẽ đường viền điều hướng (Xanh đậm bên dưới + Xanh chuẩn chỉ đường bên trên)
        map.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .color(Color.parseColor("#1E40AF"))
                .width(7.0f));

        map.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .color(Color.parseColor("#3B82F6"))
                .width(4.2f));

        LatLng startPos = latLngList.get(0);
        LatLng endPos = latLngList.get(latLngList.size() - 1);

        Icon rescuerIcon = createPinIcon(context, Color.parseColor("#111111"), "CỨU HỘ");
        Icon victimIcon = createPinIcon(context, Color.parseColor("#E5383B"), "SOS");

        map.addMarker(new MarkerOptions()
                .position(startPos)
                .title(rescuerTitle)
                .icon(rescuerIcon));

        map.addMarker(new MarkerOptions()
                .position(endPos)
                .title(victimTitle)
                .icon(victimIcon));

        fitRouteBounds(map, latLngList, 110);
    }

    public interface OnRouteFetchedListener {
        void onRouteFetched(List<double[]> points);
    }

    private static String routeCacheKey(double lat1, double lng1, double lat2, double lng2) {
        return String.format(Locale.US, "%.4f,%.4f->%.4f,%.4f", lat1, lng1, lat2, lng2);
    }

    private static void fetchOsrmRouteAsync(
            double startLat,
            double startLng,
            double endLat,
            double endLng,
            OnRouteFetchedListener listener
    ) {
        String key = routeCacheKey(startLat, startLng, endLat, endLng);
        List<double[]> cached = ROUTE_CACHE.get(key);
        if (cached != null) {
            MAIN_HANDLER.post(() -> listener.onRouteFetched(cached));
            return;
        }

        ROUTE_EXECUTOR.execute(() -> {
            String[] endpoints = new String[]{
                    String.format(Locale.US,
                            "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                            startLng, startLat, endLng, endLat),
                    String.format(Locale.US,
                            "https://routing.openstreetmap.de/routed-car/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                            startLng, startLat, endLng, endLat)
            };

            for (String url : endpoints) {
                try {
                    Request req = new Request.Builder()
                            .url(url)
                            .header("User-Agent", "SosRescueAndroid/1.0")
                            .build();
                    try (Response resp = HTTP_CLIENT.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            String body = resp.body().string();
                            JSONObject json = new JSONObject(body);
                            JSONArray routes = json.optJSONArray("routes");
                            if (routes != null && routes.length() > 0) {
                                JSONObject geom = routes.getJSONObject(0).optJSONObject("geometry");
                                if (geom != null) {
                                    JSONArray coords = geom.optJSONArray("coordinates");
                                    if (coords != null && coords.length() >= 2) {
                                        List<double[]> parsed = new ArrayList<>();
                                        for (int i = 0; i < coords.length(); i++) {
                                            JSONArray pair = coords.getJSONArray(i);
                                            double lng = pair.getDouble(0);
                                            double lat = pair.getDouble(1);
                                            parsed.add(new double[]{lat, lng});
                                        }
                                        List<double[]> dense = subdividePathForSmoothTracking(parsed);
                                        ROUTE_CACHE.put(key, dense);
                                        MAIN_HANDLER.post(() -> listener.onRouteFetched(dense));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static void addFloodZonePolygon(MapLibreMap map, double centerLat, double centerLng, double radiusDeg) {
        if (map == null) return;
        List<LatLng> circlePoints = new ArrayList<>();
        int steps = 28;
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            double dLat = radiusDeg * Math.sin(angle);
            double dLng = radiusDeg * Math.cos(angle);
            circlePoints.add(new LatLng(centerLat + dLat, centerLng + dLng));
        }
        map.addPolygon(new PolygonOptions()
                .addAll(circlePoints)
                .fillColor(Color.argb(32, 229, 56, 59))
                .strokeColor(Color.parseColor("#E5383B")));
    }

    public static void fitRouteBounds(MapLibreMap map, List<LatLng> points, int paddingPx) {
        if (map == null || points == null || points.isEmpty()) return;
        if (points.size() == 1) {
            moveCamera(map, points.get(0).getLatitude(), points.get(0).getLongitude(), 15.2);
            return;
        }
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : points) {
                builder.include(p);
            }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), paddingPx), 750);
        } catch (Exception e) {
            LatLng p1 = points.get(0);
            LatLng p2 = points.get(points.size() - 1);
            moveCamera(map, (p1.getLatitude() + p2.getLatitude()) / 2.0,
                    (p1.getLongitude() + p2.getLongitude()) / 2.0, 14.5);
        }
    }

    public static void fitTwoPoints(MapLibreMap map, LatLng p1, LatLng p2, int paddingPx) {
        if (map == null) return;
        fitRouteBounds(map, Arrays.asList(p1, p2), paddingPx);
    }
}
