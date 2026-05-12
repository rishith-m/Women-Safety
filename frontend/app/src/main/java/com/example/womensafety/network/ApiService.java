package com.example.womensafety.network;

import com.example.womensafety.models.ApiModels.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("alert/trigger")
    Call<AlertResponse> triggerAlert(@Body AlertRequest request);

    @POST("alert/location")
    Call<LocationUpdateResponse> updateLocation(@Body LocationUpdateRequest request);

    @POST("ai/analyze")
    Call<AnalyzeResponse> analyzeAudioText(@Body AnalyzeRequest request);

    @POST("route/analyze")
    Call<RouteSafetyResponse> getSafeRoutes(@Body RouteRequest request);
}
