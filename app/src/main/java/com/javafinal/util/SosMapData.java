package com.javafinal.util;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import com.javafinal.model.LocationCurrent;
import com.javafinal.model.LocationHistory;
import com.javafinal.model.PublicSosPoint;
import com.javafinal.model.SosRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;

/** Chỉ vẽ tọa độ và lộ trình server trả về, không nội suy vị trí di chuyển. */
public final class SosMapData {
    private SosMapData() { }
    public static boolean valid(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
    public static LocationCurrent victim(SosRequest sos) {
        if (sos == null) return null;
        if (sos.getLocations() != null && sos.getLocations().getVictim() != null) return sos.getLocations().getVictim();
        if (!sos.hasInitialLocation()) return null;
        return new LocationCurrent(null, sos.getId(), "VICTIM", sos.getInitialLat(), sos.getInitialLng(), 0, null);
    }
    public static LocationCurrent rescuer(SosRequest sos) {
        return sos == null || sos.getLocations() == null ? null : sos.getLocations().getRescuer();
    }
    public static String coordinates(LocationCurrent location) {
        if (location == null || !valid(location.getLat(), location.getLng())) return "Chưa có vị trí";
        return String.format(java.util.Locale.US, "%.5f, %.5f", location.getLat(), location.getLng());
    }

    /** Bản đồ công khai với tọa độ đã làm mờ dành cho USER. Trả true nếu đã căn camera. */
    public static boolean drawPublic(Context context, MapLibreMap map, List<PublicSosPoint> points,
            Location own, Predicate<PublicSosPoint> filter, boolean center) {
        map.clear();
        List<LatLng> bounds = new ArrayList<>();
        if (own != null) {
            LatLng position = new LatLng(own.getLatitude(), own.getLongitude());
            bounds.add(position);
            map.addMarker(new MarkerOptions().position(position).title("Vị trí của bạn")
                    .icon(MapHelper.createPinIcon(context, Color.BLACK, "BẠN")));
        }
        for (PublicSosPoint point : points) {
            if (point == null || !filter.test(point) || !valid(point.getAreaLat(), point.getAreaLng())) continue;
            LatLng position = new LatLng(point.getAreaLat(), point.getAreaLng());
            bounds.add(position);
            MapHelper.addFloodZonePolygon(map, point.getAreaLat(), point.getAreaLng(), 0.005);
            String createdAt = point.getCreatedAt();
            map.addMarker(new MarkerOptions().position(position).title(point.getEmergencyTypeLabel())
                    .snippet("Vị trí đã làm mờ • " + (createdAt == null || createdAt.trim().isEmpty() ? "—" : createdAt))
                    .icon(MapHelper.createPinIcon(context, Color.parseColor("#E5383B"), "SOS")));
        }
        if (center && !bounds.isEmpty()) {
            if (bounds.size() == 1) MapHelper.moveCamera(map, bounds.get(0).getLatitude(), bounds.get(0).getLongitude(), 13);
            else MapHelper.fitRouteBounds(map, bounds, 100);
            return true;
        }
        return false;
    }
    /** Bản đồ cứu hộ dùng tọa độ chính xác, gồm cả PUBLIC và PRIVATE. */
    public static boolean drawRescuer(Context context, MapLibreMap map, List<SosRequest> points,
            Location own, Predicate<SosRequest> filter, boolean center) {
        map.clear();
        List<LatLng> bounds = new ArrayList<>();
        if (own != null) {
            LatLng position = new LatLng(own.getLatitude(), own.getLongitude());
            bounds.add(position);
            map.addMarker(new MarkerOptions().position(position).title("Vị trí của bạn")
                    .icon(MapHelper.createPinIcon(context, Color.BLACK, "BẠN")));
        }
        for (SosRequest point : points) {
            if (point == null || !filter.test(point)) continue;
            LocationCurrent victim = victim(point);
            if (victim == null || !valid(victim.getLat(), victim.getLng())) continue;
            LatLng position = new LatLng(victim.getLat(), victim.getLng());
            bounds.add(position);
            boolean privateSos = SosRequest.VISIBILITY_PRIVATE.equals(point.getVisibility());
            String description = point.getDescription();
            map.addMarker(new MarkerOptions().position(position).title(point.getEmergencyTypeLabel())
                    .snippet(point.getVisibilityLabel() + " • " + point.getStatusLabel()
                            + (description == null || description.trim().isEmpty() ? "" : "\n" + description))
                    .icon(MapHelper.createPinIcon(context, Color.parseColor(privateSos ? "#C27C0E" : "#E5383B"), "SOS")));
        }
        if (center && !bounds.isEmpty()) {
            if (bounds.size() == 1) MapHelper.moveCamera(map, bounds.get(0).getLatitude(), bounds.get(0).getLongitude(), 15);
            else MapHelper.fitRouteBounds(map, bounds, 100);
            return true;
        }
        return false;
    }

    public static void draw(Context context, MapLibreMap map, SosRequest sos, List<LocationHistory> trail, boolean center) {
        if (map == null) return;
        map.clear();
        List<LatLng> bounds = new ArrayList<>();
        addMarker(context, map, victim(sos), "SOS", "Vị trí người cần cứu hộ", "#E5383B", bounds);
        addMarker(context, map, rescuer(sos), "CỨU HỘ", "Vị trí đội cứu hộ", "#111111", bounds);
        List<LatLng> points = new ArrayList<>();
        if (trail != null) for (LocationHistory item : trail) {
            if (item != null && valid(item.getLat(), item.getLng())) points.add(new LatLng(item.getLat(), item.getLng()));
        }
        if (points.size() > 1) map.addPolyline(new PolylineOptions().addAll(points).width(4).color(Color.parseColor("#3B82F6")));
        if (center && !bounds.isEmpty()) {
            if (bounds.size() == 1) MapHelper.moveCamera(map, bounds.get(0).getLatitude(), bounds.get(0).getLongitude(), 15);
            else MapHelper.fitRouteBounds(map, bounds, 100);
        }
    }
    public static void addMarker(Context context, MapLibreMap map, LocationCurrent location, String label,
            String title, String color, List<LatLng> points) {
        if (location == null || !valid(location.getLat(), location.getLng())) return;
        LatLng point = new LatLng(location.getLat(), location.getLng());
        points.add(point);
        map.addMarker(new MarkerOptions().position(point).title(title)
                .icon(MapHelper.createPinIcon(context, Color.parseColor(color), label)));
    }
}
