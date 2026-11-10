package com.javafinal.viewmodel;

import com.google.gson.Gson;
import com.javafinal.model.LocationCurrent;
import com.javafinal.model.SosRequest;
import com.javafinal.util.OsrmRouter;
import com.javafinal.util.SosMapData;
import org.junit.Test;
import static org.junit.Assert.*;

public class SosMapDataTest {
    @Test public void missingCoordinatesDoNotBecomeAnInventedZeroMarker() {
        SosRequest sos = new Gson().fromJson("{\"id\":\"s1\",\"status\":\"ACCEPTED\"}", SosRequest.class);
        assertFalse(sos.hasInitialLocation());
        assertNull(SosMapData.victim(sos));
        assertNull(SosMapData.rescuer(sos));
    }
    @Test public void actualZeroCoordinatesArePreservedAndCurrentLocationTakesPrecedence() {
        SosRequest sos = new Gson().fromJson("{\"initial_lat\":0,\"initial_lng\":0}", SosRequest.class);
        assertTrue(sos.hasInitialLocation());
        assertEquals(0, SosMapData.victim(sos).getLat(), 0);
        LocationCurrent current = new LocationCurrent(null, "s1", "VICTIM", 10.78, 106.7, 5, null);
        sos.setLocations(new SosRequest.LocationsPair(current, null));
        assertSame(current, SosMapData.victim(sos));
    }
    @Test public void osrmGeoJsonCoordinatesAreConvertedToLatLngAndStoredWithoutMutatingRescuer() {
        String json = "{\"code\":\"Ok\",\"routes\":[{\"distance\":2450.0,\"duration\":300.0,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[106.69,10.77],[106.695,10.775],[106.70,10.78]]}}]}";
        OsrmRouter.RouteResult result = OsrmRouter.parseOsrmResponse(json);
        assertNotNull(result);
        assertEquals(3, result.getPoints().size());
        assertEquals(10.77, result.getPoints().get(0)[0], 1e-6);
        assertEquals(106.69, result.getPoints().get(0)[1], 1e-6);
        assertEquals(2.45, result.getDistanceKm(), 1e-6);
        assertEquals(5.0, result.getDurationMinutes(), 1e-6);

        SosRequest sos = new SosRequest();
        sos.setRescuerLat(10.771);
        sos.setRescuerLng(106.691);
        sos.setOsrmRoute(result.getPoints(), result.getDistanceKm(), result.getDurationMinutes());
        assertTrue(sos.isOsrmLoaded());
        assertEquals(2.45, sos.getOsrmDistanceKm(), 1e-6);
        assertEquals(5.0, sos.getOsrmDurationMinutes(), 1e-6);
        assertEquals(10.771, sos.getRescuerLat(), 1e-6);
        assertEquals(106.691, sos.getRescuerLng(), 1e-6);
    }
}
