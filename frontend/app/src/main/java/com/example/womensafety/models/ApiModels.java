package com.example.womensafety.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiModels {

    public static class RegisterRequest {
        public String name;
        public String phone;
        public List<String> contacts;
        
        public RegisterRequest(String name, String phone, List<String> contacts) {
            this.name = name;
            this.phone = phone;
            this.contacts = contacts;
        }
    }

    public static class RegisterResponse {
        public String message;
        public String user_id;
        public String error;
    }

    public static class AlertRequest {
        public String user_id;
        public Double latitude;
        public Double longitude;
        
        public AlertRequest(String user_id, Double latitude, Double longitude) {
            this.user_id = user_id;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static class AlertResponse {
        public String message;
        public String alert_id;
        public String error;
    }

    public static class LocationUpdateRequest {
        public String alert_id;
        public Double latitude;
        public Double longitude;
        
        public LocationUpdateRequest(String alert_id, Double latitude, Double longitude) {
            this.alert_id = alert_id;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static class LocationUpdateResponse {
        public String message;
        public String error;
    }

    public static class AnalyzeRequest {
        public String text;
        
        public AnalyzeRequest(String text) {
            this.text = text;
        }
    }

    public static class AnalyzeResponse {
        public String text_analyzed;
        public boolean is_emergency;
        public boolean action_required;
        public String error;
    }

    public static class RouteRequest {
        @SerializedName("origin_lat")
        public double startLat;
        @SerializedName("origin_lng")
        public double startLng;
        @SerializedName("dest_lat")
        public double endLat;
        @SerializedName("dest_lng")
        public double endLng;

        public RouteRequest() {}

        public RouteRequest(double startLat, double startLng, double endLat, double endLng) {
            this.startLat = startLat;
            this.startLng = startLng;
            this.endLat = endLat;
            this.endLng = endLng;
        }
    }

    public static class RouteSafetyResponse {
        public List<SafeRoute> routes;
        public String status;
        public String error;

        public RouteSafetyResponse() {}
    }

    public static class SafeRoute {
        @SerializedName("route_id")
        public String routeId;
        @SerializedName("safety_score")
        public double safetyScore;
        public String reason;
        public List<LatLng> waypoints;

        public SafeRoute() {}
    }

    public static class LatLng {
        public double lat;
        public double lng;
        
        public LatLng() {}

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
