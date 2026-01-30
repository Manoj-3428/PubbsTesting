package in.pubbs.pubbsadmin.Api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;
import java.util.Map;

/**
 * API Service interface for Cycle Demand operations
 * Endpoint will be provided later, using placeholder for now
 */
public interface CycleDemandApiService {
    
    /**
     * Get cycle demand for a specific station by station ID
     * @param stationId The station ID to fetch demand for
     * @return Response containing the demand value for the station
     */
    @GET("cycle-demand/{stationId}")
    Call<CycleDemandResponse> getCycleDemand(@Path("stationId") String stationId);
    
    /**
     * Get cycle demand for all stations at once
     * @return Response containing demand values for all stations
     */
    @GET("cycle-demand/all")
    Call<AllStationsDemandResponse> getAllStationsDemand();
    
    /**
     * Update cycle demand for a specific station
     * @param stationId The station ID to update
     * @param request Request body containing the demand value
     * @return Response confirming the update
     */
    @PUT("cycle-demand/{stationId}")
    Call<CycleDemandResponse> updateCycleDemand(@Path("stationId") String stationId, @Body CycleDemandRequest request);
    
    /**
     * Update cycle demand for multiple stations at once
     * @param request Map of stationId to demand value
     * @return Response confirming the updates
     */
    @PUT("cycle-demand/bulk")
    Call<BulkUpdateResponse> updateBulkCycleDemand(@Body Map<String, Integer> request);
}
