package com.example.womensafety.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Use your computer's actual Wi-Fi address so your phone/emulator can find it
    private static final String BASE_URL = "http://10.83.44.50:5000/api/"; 
    // Use '10.0.2.2' ONLY if you are using the Android Studio Emulator and the above fails
    // private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
