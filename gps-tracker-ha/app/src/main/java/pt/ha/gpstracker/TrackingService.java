package pt.ha.gpstracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.*;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.CancellationTokenSource;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class TrackingService extends Service {
    static final String START = "pt.ha.gpstracker.START";
    static final String STOP = "pt.ha.gpstracker.STOP";
    static volatile boolean running = false;
    private static final String CHANNEL = "gps_tracker_location";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient locationClient;
    private CancellationTokenSource cancellation;
    private SecureConfig settings;
    private boolean active = false, busy = false;
    private final Runnable periodic = new Runnable() {
        @Override public void run() {
            if (!active) return;
            collect();
            handler.postDelayed(this, settings.minutes * 60000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel(CHANNEL, "Localização GPS", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (STOP.equals(action)) { status("Parado pelo utilizador."); stopSelf(); return START_NOT_STICKY; }
        if (!START.equals(action) || active) return START_NOT_STICKY;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status("Permissão de localização em falta."); stopSelf(); return START_NOT_STICKY;
        }
        try {
            settings = SecureConfig.load(this);
            settings.validate();
            Notification n = notification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(31, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else startForeground(31, n);
            active = true; running = true;
            status("A obter posição...");
            handler.post(periodic);
        } catch (Exception e) { status("Falha ao iniciar: " + e.getMessage()); stopSelf(); }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
            new Intent(this, TrackingService.class).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("GPS Tracker HA ativo")
            .setContentText("Envio a cada " + settings.minutes + " minutos")
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .addAction(new Notification.Action.Builder(R.drawable.ic_location, "Parar", stop).build())
            .build();
    }
    private void collect() {
        if (!active || busy) return;
        busy = true;
        cancellation = new CancellationTokenSource();
        try {
            CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(20000)
                .setDurationMillis(25000)
                .build();
            locationClient.getCurrentLocation(request, cancellation.getToken())
                .addOnSuccessListener(fix -> {
                    if (!active) { busy = false; return; }
                    if (fix == null) { status("Não foi possível obter localização."); busy = false; return; }
                    long age = (SystemClock.elapsedRealtimeNanos() - fix.getElapsedRealtimeNanos()) / 1000000L;
                    if (age > 60000 || age < -1000) {
                        status("Localização demasiado antiga; ignorada."); busy = false; return;
                    }
                    transmit(fix);
                })
                .addOnFailureListener(e -> { status("Erro GPS: " + e.getClass().getSimpleName()); busy = false; })
                .addOnCanceledListener(() -> busy = false);
        } catch (Exception e) { status("Erro GPS: " + e.getMessage()); busy = false; }
    }
    private void transmit(Location fix) {
        network.execute(() -> {
            try {
                if (!active) return;
                JSONObject j = new JSONObject();
                j.put("device", settings.device);
                j.put("latitude", fix.getLatitude());
                j.put("longitude", fix.getLongitude());
                j.put("lat", fix.getLatitude());
                j.put("lng", fix.getLongitude());
                if (fix.hasAccuracy()) {
                    j.put("gps_accuracy", fix.getAccuracy());
                    j.put("accuracy_m", fix.getAccuracy());
                }
                if (fix.hasSpeed()) j.put("speed_m_s", fix.getSpeed());
                j.put("timestamp", Instant.ofEpochMilli(fix.getTime()).toString());
                j.put("provider", fix.getProvider() == null ? "fused" : fix.getProvider());
                Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery != null) {
                    int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (level >= 0 && scale > 0) j.put("battery_pct", 100.0 * level / scale);
                }
                HttpURLConnection conn = (HttpURLConnection) new URL(settings.endpoint).openConnection();
                try {
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    byte[] data = j.toString().getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(data.length);
                    try (OutputStream os = conn.getOutputStream()) { os.write(data); }
                    int response = conn.getResponseCode();
                    if (response < 200 || response >= 300)
                        throw new IOException("HTTP " + response);
                } finally { conn.disconnect(); }
                status("Enviado ✓  " + String.format(Locale.US, "%.6f, %.6f",
                    fix.getLatitude(), fix.getLongitude()));
            } catch (Exception e) {
                status("Falha de envio: " + e.getClass().getSimpleName() +
                    (e.getMessage() == null ? "" : " - " + e.getMessage()));
            } finally { handler.post(() -> busy = false); }
        });
    }
    private void status(String text) {
        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-PT"))
            .format(new Date());
        getSharedPreferences("runtime", MODE_PRIVATE).edit()
            .putString("status", "[" + now + "] " + text).apply();
    }
    @Override public void onDestroy() {
        active = false; running = false;
        handler.removeCallbacks(periodic);
        if (cancellation != null) cancellation.cancel();
        network.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
