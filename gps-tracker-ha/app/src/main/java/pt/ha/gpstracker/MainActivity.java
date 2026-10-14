package pt.ha.gpstracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private EditText url, device, interval;
    private TextView state, detail;
    private Button start, stop;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (state != null) {
                boolean running = TrackingService.running;
                state.setText(running ? "● RASTREAMENTO ATIVO" : "● RASTREAMENTO PARADO");
                detail.setText(getSharedPreferences("runtime", MODE_PRIVATE)
                    .getString("status", "Ainda não há envios."));
                start.setEnabled(!running);
                stop.setEnabled(running);
            }
            handler.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        SecureConfig config;
        try { config = SecureConfig.load(this); }
        catch (Exception e) { config = new SecureConfig("", "telemovel", 5); }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF111A29);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(19);
        l.setPadding(p, dp(24), p, dp(30));
        scroll.addView(l);
        setContentView(scroll);
        addText(l, "GPS TRACKER HA", 27, true, 0xFFF2F7FA);
        addText(l, "Localização por webhook HTTPS para o Home Assistant", 14, false, 0xFFA0B9C9);
        state = addText(l, "● RASTREAMENTO PARADO", 17, true, 0xFF47DDC3);
        detail = addText(l, "A aguardar...", 13, false, 0xFFB9C8D0);
        addText(l, "URL HTTPS completo do webhook", 15, true, 0xFFF2F7FA);
        url = field(l, config.endpoint,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addText(l, "Não precisas de user/password. Guarda o URL como uma password: o ID é secreto.",
            12, false, 0xFFA0B9C9);
        addText(l, "Identificador do telemóvel", 15, true, 0xFFF2F7FA);
        device = field(l, config.device, InputType.TYPE_CLASS_TEXT);
        addText(l, "Intervalo de envio (minutos)", 15, true, 0xFFF2F7FA);
        interval = field(l, Integer.toString(config.minutes), InputType.TYPE_CLASS_NUMBER);
        Button save = button(l, "Guardar configuração");
        save.setOnClickListener(v -> save());
        start = button(l, "▶ Iniciar rastreamento");
        start.setOnClickListener(v -> begin());
        stop = button(l, "■ Parar rastreamento");
        stop.setOnClickListener(v -> {
            startService(new Intent(this, TrackingService.class)
                .setAction(TrackingService.STOP));
        });
        addText(l, "Quando ativo, o Android mostra uma notificação permanente com botão Parar. " +
            "Os envios são aproximados e podem ser atrasados pela poupança de bateria. " +
            "O rastreamento só começa depois de tocares em Iniciar.", 12, false, 0xFFA0B9C9);
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    private TextView addText(LinearLayout l, String content, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(content);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, dp(9), 0, dp(5));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        l.addView(t);
        return t;
    }
    private EditText field(LinearLayout l, String value, int kind) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setInputType(kind);
        e.setText(value);
        e.setTextColor(0xFFFFFFFF);
        e.setHintTextColor(0xFFABC0CF);
        e.setSelectAllOnFocus(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF47DDC3));
        l.addView(e);
        return e;
    }
    private Button button(LinearLayout l, String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        l.addView(b);
        return b;
    }

    private boolean save() {
        try {
            SecureConfig c = new SecureConfig(url.getText().toString().trim(),
                device.getText().toString().trim(),
                Integer.parseInt(interval.getText().toString().trim()));
            c.save(this);
            Toast.makeText(this, "Configuração guardada", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            alert(e.getMessage() == null ? "Erro ao guardar" : e.getMessage());
            return false;
        }
    }
    private void begin() {
        if (!save()) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            return;
        }
        try {
            startForegroundService(new Intent(this, TrackingService.class)
                .setAction(TrackingService.START));
        } catch (Exception e) { alert("Não foi possível iniciar: " + e.getMessage()); }
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(code, perms, grants);
        if (code == 10 || code == 11) {
            if (code == 10
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                alert("É preciso permitir a localização.");
                return;
            }
            if (code == 10 && Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            } else begin();
        }
    }
    private void alert(String s) {
        new AlertDialog.Builder(this).setTitle("GPS Tracker HA")
            .setMessage(s).setPositiveButton("OK", null).show();
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
