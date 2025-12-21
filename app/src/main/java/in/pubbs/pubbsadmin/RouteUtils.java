package in.pubbs.pubbsadmin;

import com.google.android.gms.maps.model.LatLng;
import in.pubbs.pubbsadmin.Model.StationData;
import java.util.List;
import java.util.Map;

/**
 * Utility class for distance calculations and route distance computations
 */
public class RouteUtils {
    
    /**
     * Calculate straight-line distance (Haversine formula) - ONLY for proximity checking
     * NOT used for route distance calculations - road distances are used instead
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    
    /**
     * Calculate route distance using road distances from cache
     */
    public static double calculateRouteDistance(List<StationData> route, Map<String, Double> roadDistanceCache) {
        if (route.size() < 2) return 0;
        
        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            // Use road distance from cache
            totalDistance += getRoadDistance(route.get(i), route.get(i + 1), roadDistanceCache);
        }
        return totalDistance;
    }
    
    /**
     * Get road distance from cache
     */
    private static double getRoadDistance(StationData from, StationData to, Map<String, Double> roadDistanceCache) {
        // Create cache key (always use smaller ID first for consistency)
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        Double distance = roadDistanceCache.get(cacheKey);
        if (distance != null) {
            return distance;
        }
        
        // If not in cache, return straight-line distance as fallback (shouldn't happen in normal flow)
        return calculateDistance(from.latitude, from.longitude, to.latitude, to.longitude);
    }
    
    /**
     * Calculate total distance of a path (polyline points) including straight line segments
     * This includes both road paths and straight line extensions to stations
     */
    public static double calculateTotalPathDistance(List<LatLng> pathPoints) {
        if (pathPoints.size() < 2) return 0;
        
        double totalDistance = 0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            LatLng point1 = pathPoints.get(i);
            LatLng point2 = pathPoints.get(i + 1);
            // Calculate distance between consecutive points (includes both road paths and straight lines)
            totalDistance += calculateDistance(point1.latitude, point1.longitude, 
                                              point2.latitude, point2.longitude);
        }
        return totalDistance;
    }
}

