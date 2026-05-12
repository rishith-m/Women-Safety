package com.example.womensafety.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.womensafety.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class SafetyForegroundService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "SafetyServiceChannel";
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int stepCount = 0;
    private float distance = 0f;
    private static final float STEP_LENGTH_IN_METERS = 0.762f; // Average step length

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initStepCounter();
    }

    private void initStepCounter() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++;
            distance = (stepCount * STEP_LENGTH_IN_METERS) / 1000f; // in km
            broadcastStats();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void broadcastStats() {
        Intent intent = new Intent("com.example.womensafety.STEP_UPDATE");
        intent.putExtra("steps", stepCount);
        intent.putExtra("distance", distance);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Women Safety App")
                .setContentText("Background protection is active. Listening for 'help'.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("ai_enabled", true)) {
            initSpeechRecognizer();
        }

        return START_STICKY;
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {
                    // Restart listening on error
                    speechRecognizer.startListening(recognizerIntent);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null) {
                        for (String match : matches) {
                            Log.d("Voice", "Heard: " + match);
                            if (match.toLowerCase().contains("help") || match.toLowerCase().contains("sos")) {
                                triggerEmergencyProtocol();
                                break;
                            }
                        }
                    }
                    // Continue listening
                    speechRecognizer.startListening(recognizerIntent);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
                @Override
                public void onSegmentResults(android.os.Bundle segmentResults) {}
                @Override
                public void onEndOfSegmentedSession() {}
                @Override
                public void onLanguageDetection(android.os.Bundle results) {}
            });

            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void triggerEmergencyProtocol() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String contactsStr = prefs.getString("emer_contacts", "");
        
        if (contactsStr.isEmpty()) return;

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                String locationLink = "Location not available.";
                if (location != null) {
                    locationLink = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                }

                String message = "Voice SOS! I need help. My location: " + locationLink;
                SmsManager smsManager = SmsManager.getDefault();
                String[] contacts = contactsStr.split("\\s*,\\s*");
                for (String contact : contacts) {
                    if (!contact.isEmpty()) {
                        try {
                            smsManager.sendTextMessage(contact, null, message, null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safety Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
