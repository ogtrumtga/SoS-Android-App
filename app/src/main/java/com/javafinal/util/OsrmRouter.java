package com.javafinal.util;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Truy vấn lộ trình đường giao thông thực tế từ OSRM (Open Source Routing Machine - dữ liệu OpenStreetMap).
 * Sử dụng OkHttpClient độc lập để không ảnh hưởng tới interceptor kiểm thử của ApiClient.
 */
public final class OsrmRouter {

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build();

    private static final Map<String, RouteResult> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private OsrmRouter() {
    }

    public static final class RouteResult {
        private final List<double[]> points;
        private final double distanceKm;
        private final double durationMinutes;

        public RouteResult(List<double[]> points, double distanceKm, double durationMinutes) {
            this.points = points == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(points));
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }

        public List<double[]> getPoints() {
            return points;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public double getDurationMinutes() {
            return durationMinutes;
        }
    }

    public static String cacheKey(double startLat, double startLng, double endLat, double endLng) {
        return String.format(Locale.US, "%.4f,%.4f->%.4f,%.4f", startLat, startLng, endLat, endLng);
    }

    public static RouteResult getCached(double startLat, double startLng, double endLat, double endLng) {
        if (!SosMapData.valid(startLat, startLng) || !SosMapData.valid(endLat, endLng)) return null;
        return CACHE.get(cacheKey(startLat, startLng, endLat, endLng));
    }

    /**
     * Tìm đường đi bộ/lái xe ngắn nhất trên mạng lưới đường giao thông thực tế giữa 2 tọa độ.
     * Nếu đã có trong bộ nhớ đệm (di chuyển < ~11m), trả kết quả ngay lập tức.
     */
    public static void fetchRoute(
            double startLat,
            double startLng,
            double endLat,
            double endLng,
            Consumer<RouteResult> callback
    ) {
        if (callback == null) return;
        if (!SosMapData.valid(startLat, startLng) || !SosMapData.valid(endLat, endLng)) return;
        if (Math.abs(startLat - endLat) < 1e-6 && Math.abs(startLng - endLng) < 1e-6) return;

        String key = cacheKey(startLat, startLng, endLat, endLng);
        RouteResult cached = CACHE.get(key);
        if (cached != null) {
            deliver(callback, cached);
            return;
        }

        if (!IN_FLIGHT.add(key)) {
            return;
        }

        String url = String.format(
                Locale.US,
                "%s%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                OSRM_BASE_URL,
                startLng, startLat,
                endLng, endLat
        );

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "SOS-Android-Client/1.0")
                .get()
                .build();

        HTTP.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                IN_FLIGHT.remove(key);
            }

            @Override
            public void onResponse(Call call, Response response) {
                IN_FLIGHT.remove(key);
                try (Response res = response) {
                    if (!res.isSuccessful() || res.body() == null) return;
                    String body = res.body().string();
                    RouteResult parsed = parseOsrmResponse(body);
                    if (parsed != null) {
                        CACHE.put(key, parsed);
                        deliver(callback, parsed);
                    }
                } catch (Exception ignored) {
                    // Fallback về đường thẳng Haversine nếu lỗi mạng hoặc JSON
                }
            }
        });
    }

    public static RouteResult parseOsrmResponse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("code") || !"Ok".equalsIgnoreCase(root.get("code").getAsString())) {
                return null;
            }
            JsonArray routes = root.getAsJsonArray("routes");
            if (routes == null || routes.isEmpty()) return null;

            JsonObject route = routes.get(0).getAsJsonObject();
            double distanceMeters = route.has("distance") ? route.get("distance").getAsDouble() : 0.0;
            double durationSeconds = route.has("duration") ? route.get("duration").getAsDouble() : 0.0;

            JsonObject geometry = route.getAsJsonObject("geometry");
            if (geometry == null || !geometry.has("coordinates")) return null;
            JsonArray coordinates = geometry.getAsJsonArray("coordinates");
            if (coordinates == null || coordinates.size() < 2) return null;

            List<double[]> points = new ArrayList<>(coordinates.size());
            for (int i = 0; i < coordinates.size(); i++) {
                JsonArray pair = coordinates.get(i).getAsJsonArray();
                if (pair != null && pair.size() >= 2) {
                    double lng = pair.get(0).getAsDouble();
                    double lat = pair.get(1).getAsDouble();
                    if (SosMapData.valid(lat, lng)) {
                        points.add(new double[]{lat, lng});
                    }
                }
            }
            if (points.size() < 2) return null;
            return new RouteResult(points, distanceMeters / 1000.0, durationSeconds / 60.0);
        } catch (Exception e) {
            return null;
        }
    }

    private static void deliver(Consumer<RouteResult> callback, RouteResult result) {
        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper != null) {
            if (Looper.myLooper() == mainLooper) {
                callback.accept(result);
            } else {
                new Handler(mainLooper).post(() -> callback.accept(result));
            }
        } else {
            callback.accept(result);
        }
    }
}
