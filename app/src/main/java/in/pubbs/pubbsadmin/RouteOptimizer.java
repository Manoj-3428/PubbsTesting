package in.pubbs.pubbsadmin;

import android.util.Log;
import in.pubbs.pubbsadmin.Model.StationData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 2-opt Improvement Algorithm with Capacity Constraints
 * 
 * This algorithm tries to improve the route by swapping route segments,
 * but only accepts swaps that maintain valid vehicle capacity constraints.
 * 
 * How it works:
 * 1. Try swapping segments between positions i and j (reverse the segment)
 * 2. For each swapped route, validate that capacity constraints are satisfied
 * 3. Only accept the swap if:
 *    - The route is valid (capacity constraints satisfied at each step)
 *    - The route is shorter than the current best route
 * 4. Repeat until no improvements can be made
 */
public class RouteOptimizer {
    
    private static final String TAG = "RouteOptimizer";
    
    /**
     * Optimize route using 2-opt algorithm
     */
    public static List<StationData> optimizeRouteWith2Opt(List<StationData> route, 
                                                           Map<String, Double> roadDistanceCache) {
        if (route.size() < 4) return route; // Need at least 4 stations for 2-opt
        
        List<StationData> improvedRoute = new ArrayList<>(route);
        boolean improved = true;
        int maxIterations = 20; // Reduced from 100 to speed up (2-opt is expensive)
        int iterations = 0;
        
        while (improved && iterations < maxIterations) {
            improved = false;
            iterations++;
            
            double bestDistance = RouteUtils.calculateRouteDistance(improvedRoute, roadDistanceCache);
            
            // Try 2-opt swaps (reduced search space for performance)
            // Only try swaps that are likely to improve the route
            for (int i = 1; i < improvedRoute.size() - 2; i++) {
                for (int j = i + 2; j < improvedRoute.size() && j < i + 5; j++) { // Limit j to nearby stations
                    // Create new route with swapped segment
                    List<StationData> newRoute = twoOptSwap(improvedRoute, i, j);
                    
                    // Check if the swapped route maintains capacity constraints
                    if (RouteValidator.isValidRoute(newRoute)) {
                        double newDistance = RouteUtils.calculateRouteDistance(newRoute, roadDistanceCache);
                        
                        // If new route is shorter and valid, use it
                        if (newDistance < bestDistance * 0.99) { // Only accept if at least 1% improvement
                            improvedRoute = newRoute;
                            bestDistance = newDistance;
                            improved = true;
                            break; // Restart from beginning after improvement
                        }
                    }
                }
                if (improved) break;
            }
        }
        
        return improvedRoute;
    }
    
    /**
     * Perform 2-opt swap: reverse the segment between positions i and j
     * 
     * Example:
     * Original: A → B → C → D → E → F
     * Swap(1, 3): A → D → C → B → E → F
     * (segment B-C-D is reversed)
     */
    private static List<StationData> twoOptSwap(List<StationData> route, int i, int j) {
        List<StationData> newRoute = new ArrayList<>();
        
        // Add route[0] to route[i-1] in order
        for (int k = 0; k < i; k++) {
            newRoute.add(route.get(k));
        }
        
        // Add route[i] to route[j] in reverse order
        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }
        
        // Add route[j+1] to route[end] in order
        for (int k = j + 1; k < route.size(); k++) {
            newRoute.add(route.get(k));
        }
        
        return newRoute;
    }
}

