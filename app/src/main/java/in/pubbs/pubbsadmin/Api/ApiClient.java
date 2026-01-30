package in.pubbs.pubbsadmin.Api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit API client for making HTTP requests
 * Base URL will be updated when endpoint is provided
 */
public class ApiClient {
    private static final String BASE_URL = "https://api.example.com/"; // TODO: Update with actual endpoint
    private static Retrofit retrofit = null;
    private static CycleDemandApiService apiService = null;
    
    /**
     * Get Retrofit instance
     * @return Retrofit instance
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
    
    /**
     * Get API service instance
     * @return CycleDemandApiService instance
     */
    public static CycleDemandApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofitInstance().create(CycleDemandApiService.class);
        }
        return apiService;
    }
    
    /**
     * Update base URL (call this when endpoint is provided)
     * @param baseUrl The new base URL
     */
    public static void updateBaseUrl(String baseUrl) {
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(CycleDemandApiService.class);
    }
}
