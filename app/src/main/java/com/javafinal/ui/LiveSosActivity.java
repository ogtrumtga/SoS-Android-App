package com.javafinal.ui;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.javafinal.model.LocationCurrent;
import com.javafinal.model.LocationHistory;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.SosRequest;
import com.javafinal.model.WebSocketMessage;
import com.javafinal.util.DeviceLocation;
import com.javafinal.util.NavigationHelper;
import com.javafinal.util.OsrmRouter;
import com.javafinal.util.SosMapData;
import com.javafinal.viewmodel.LocationViewModel;
import com.javafinal.viewmodel.RealtimeViewModel;
import com.javafinal.viewmodel.SosRules;
import com.javafinal.viewmodel.SosViewModel;
import java.util.Collections;
import java.util.List;

/**
 * Theo dõi dữ liệu server và truyền GPS khi màn hình đang hiển thị, dừng khi ca
 * đóng.
 */
public abstract class LiveSosActivity extends MapApiActivity {
    protected SosRequest currentSos;
    protected String sosId;
    protected SosViewModel sosVm;
    protected LocationViewModel locationVm;
    protected RealtimeViewModel realtime;
    protected DeviceLocation gps;
    protected List<LocationHistory> trail = Collections.emptyList();
    private final Handler poll = new Handler(Looper.getMainLooper());
    private boolean initialized, ended, centered, fresh;
    private long lastSent;
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            refreshSos();
            poll.postDelayed(this, 15000);
        }
    };

    protected void initLiveSos(Bundle state) {
        sosId = getIntent().getStringExtra(SOS_ID);
        if (sosId == null || sosId.trim().isEmpty()) {
            showStatus("Không có mã SOS để theo dõi.", null);
            return;
        }
        sosVm = vm(SosViewModel.class);
        locationVm = vm(LocationViewModel.class);
        realtime = vm(RealtimeViewModel.class);
        gps = new DeviceLocation(this, this::publishGps, message -> NavigationHelper.showSnack(this, message));
        bind("detail", (SosApiModels.SosSingleResponse response) -> {
            if (ended)
                return;
            if (response.getSos() == null) {
                showStatus("Không tìm thấy dữ liệu SOS.", this::refreshSos);
                return;
            }
            SosRequest previous = currentSos;
            currentSos = response.getSos();
            if (previous != null && previous.isOsrmLoaded()
                    && previous.getOsrmDistanceKm() != null && previous.getOsrmDurationMinutes() != null) {
                currentSos.setOsrmRoute(previous.getRoutePoints(), previous.getOsrmDistanceKm(),
                        previous.getOsrmDurationMinutes());
            }
            fresh = true;
            clearStatus();
            if (SosRules.isTerminal(currentSos.getStatus())) {
                endTracking();
                return;
            }
            updateOsrmRoute();
            onSosChanged();
            renderLiveMap(false);
            realtime.subscribeSos(sosId);
            if (canPublishGps())
                gps.request();
            else
                gps.stop();
            execute("locations", () -> locationVm.loadCurrent(sosId));
            execute("trail", () -> locationVm.loadTrail(sosId, "RESCUER", 100));
        }, error -> showApiError(error, this::refreshSos), loading -> {
            if (loading && currentSos == null)
                showLoading("Đang tải ca cứu hộ…");
        });
        bind("locations", (SosApiModels.LocationCurrentResponse response) -> {
            if (currentSos != null && !ended) {
                currentSos.setLocations(response.getLocations());
                updateOsrmRoute();
                onSosChanged();
                renderLiveMap(false);
            }
        }, error -> showApiError(error, this::refreshSos), null);
        bind("trail", (SosApiModels.LocationTrailResponse response) -> {
            trail = response.getTrail() == null ? Collections.emptyList() : response.getTrail();
            if (!ended)
                renderLiveMap(false);
        }, error -> showApiError(error, this::refreshSos), null);
        bind("gps", (SosApiModels.UpdateLocationResponse response) -> {
        }, error -> {
            showApiError(error, null);
            if (error.getStatusCode() == 400 || error.getStatusCode() == 403 || error.getStatusCode() == 404) {
                gps.stop();
                refreshSos();
            }
        }, null);
        realtime.getMessages().observe(this, message -> {
            if (message == null || !sosId.equals(message.getSosId()) || ended)
                return;
            if (WebSocketMessage.EVENT_SOS_COMPLETED.equals(message.getEvent())
                    || WebSocketMessage.EVENT_SOS_CANCELLED.equals(message.getEvent())) {
                endTracking();
            } else if (WebSocketMessage.EVENT_LOCATION_BROADCAST.equals(message.getEvent()) && currentSos != null) {
                SosRequest.LocationsPair locations = currentSos.getLocations();
                if (locations == null)
                    locations = new SosRequest.LocationsPair();
                LocationCurrent location = new LocationCurrent(null, sosId, message.getActor(), message.getLat(),
                        message.getLng(), message.getAccuracyM(), message.getUpdatedAt());
                if ("VICTIM".equals(message.getActor()))
                    locations.setVictim(location);
                else if ("RESCUER".equals(message.getActor()))
                    locations.setRescuer(location);
                currentSos.setLocations(locations);
                updateOsrmRoute();
                onSosChanged();
                renderLiveMap(false);
            } else if (!WebSocketMessage.EVENT_SUBSCRIBED.equals(message.getEvent()))
                refreshSos();
        });
        realtime.getConnectionState().observe(this, connection -> {
            if (connection == RealtimeViewModel.ConnectionState.CONNECTED)
                refreshSos();
        });
        realtime.getErrors().observe(this, error -> {
            if (error != null)
                showApiError(error, this::refreshSos);
        });
        initialized = true;
    }

    protected boolean canPublishGps() {
        return fresh && currentSos != null && !SosRules.isTerminal(currentSos.getStatus());
    }

    protected Location fallbackRescuerLocation() {
        return null;
    }

    protected abstract void onSosChanged();

    protected abstract void onSosEnded();

    protected void refreshSos() {
        if (initialized && !ended)
            execute("detail", () -> sosVm.loadDetail(sosId));
    }

    protected void updateOsrmRoute() {
        if (ended || currentSos == null)
            return;
        LocationCurrent victim = SosMapData.victim(currentSos);
        if (victim == null || !SosMapData.valid(victim.getLat(), victim.getLng()))
            return;
        LocationCurrent rescuer = SosMapData.rescuer(currentSos);
        double rLat = Double.NaN, rLng = Double.NaN;
        if (rescuer != null && SosMapData.valid(rescuer.getLat(), rescuer.getLng())) {
            rLat = rescuer.getLat();
            rLng = rescuer.getLng();
        } else {
            Location own = fallbackRescuerLocation();
            if (own != null && SosMapData.valid(own.getLatitude(), own.getLongitude())) {
                rLat = own.getLatitude();
                rLng = own.getLongitude();
            }
        }
        if (!SosMapData.valid(rLat, rLng))
            return;
        OsrmRouter.RouteResult cached = OsrmRouter.getCached(rLat, rLng, victim.getLat(), victim.getLng());
        if (cached != null) {
            currentSos.setOsrmRoute(cached.getPoints(), cached.getDistanceKm(), cached.getDurationMinutes());
            return;
        }
        final String targetSosId = currentSos.getId();
        OsrmRouter.fetchRoute(rLat, rLng, victim.getLat(), victim.getLng(), result -> {
            if (ended || currentSos == null)
                return;
            if (targetSosId != null && !targetSosId.equals(currentSos.getId()))
                return;
            currentSos.setOsrmRoute(result.getPoints(), result.getDistanceKm(), result.getDurationMinutes());
            onSosChanged();
            renderLiveMap(false);
        });
    }

    protected void renderLiveMap(boolean center) {
        if (!mapReady || currentSos == null || ended)
            return;
        Location fallback = fallbackRescuerLocation();
        boolean hasLocation = SosMapData.victim(currentSos) != null || SosMapData.rescuer(currentSos) != null
                || fallback != null;
        SosMapData.draw(this, map, currentSos, trail, fallback, center || !centered);
        if (hasLocation)
            centered = true;
    }

    protected void stopGps() {
        if (gps != null)
            gps.stop();
    }

    private void publishGps(Location location) {
        if (!initialized || ended || !canPublishGps())
            return;
        if (currentSos != null && SosMapData.rescuer(currentSos) == null) {
            updateOsrmRoute();
            onSosChanged();
            renderLiveMap(false);
        }
        if (SystemClock.elapsedRealtime() - lastSent < 6000)
            return;
        lastSent = SystemClock.elapsedRealtime();
        execute("gps", () -> locationVm.updateLocation(sosId, location.getLatitude(), location.getLongitude(),
                location.hasAccuracy() ? location.getAccuracy() : 0, true));
    }

    private void endTracking() {
        if (ended)
            return;
        ended = true;
        poll.removeCallbacksAndMessages(null);
        gps.stop();
        locationVm.cancelRequests();
        realtime.disconnect();
        if (mapReady)
            map.clear();
        onSosEnded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (initialized && !ended) {
            realtime.connect();
            refreshTask.run();
        }
    }

    @Override
    protected void onStop() {
        fresh = false;
        poll.removeCallbacksAndMessages(null);
        if (realtime != null)
            realtime.disconnect();
        if (locationVm != null)
            locationVm.cancelRequests();
        super.onStop();
    }
}
