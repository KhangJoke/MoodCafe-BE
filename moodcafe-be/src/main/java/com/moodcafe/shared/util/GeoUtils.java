package com.moodcafe.shared.util;

import java.math.BigDecimal;

public final class GeoUtils {

    public static final double MAX_VERIFICATION_DISTANCE_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0; // WGS84

    private GeoUtils() {
    }

    /**
     * Calculates distance between two coordinates in meters using the Haversine formula.
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    public static double calculateDistanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new IllegalArgumentException("Coordinates must not be null");
        }
        return calculateDistanceMeters(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    public static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
