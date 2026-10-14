package pt.ha.gpstracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureConfig {
    final String endpoint;
    final String device;
    final int minutes;

    SecureConfig(String endpoint, String device, int minutes) {
        this.endpoint = endpoint;
        this.device = device;
        this.minutes = minutes;
    }

    static SecureConfig load(Context context) throws Exception {
        SharedPreferences p = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return new SecureConfig(decrypt(p.getString("endpoint", "")),
            p.getString("device", "telemovel"), p.getInt("minutes", 5));
    }

    void save(Context context) throws Exception {
        validate();
        boolean ok = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("endpoint", encrypt(endpoint))
            .putString("device", device)
            .putInt("minutes", minutes).commit();
        if (!ok) throw new IllegalStateException("Não foi possível guardar a configuração.");
    }

    void validate() throws Exception {
        URI u = new URI(endpoint);
        if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null
            || u.getUserInfo() != null || u.getRawFragment() != null
            || u.getPath() == null || u.getPath().length() < 10
            || endpoint.endsWith("/"))
            throw new IllegalArgumentException("Cola o URL HTTPS COMPLETO, incluindo o ID do webhook.");
        if (!device.matches("[A-Za-z0-9_-]{1,40}"))
            throw new IllegalArgumentException("Identificador inválido (letras, números, _ ou -).");
        if (minutes < 1 || minutes > 1440)
            throw new IllegalArgumentException("Intervalo entre 1 e 1440 minutos.");
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias("gps_tracker_ha_key"))
            return (SecretKey) store.getKey("gps_tracker_ha_key", null);
        KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder("gps_tracker_ha_key",
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return g.generateKey();
    }

    private static String encrypt(String s) throws Exception {
        if (s == null || s.isEmpty()) return "";
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        return Base64.encodeToString(c.getIV(), Base64.NO_WRAP) + ":" +
            Base64.encodeToString(c.doFinal(s.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }

    private static String decrypt(String s) throws Exception {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Segredo guardado inválido.");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128,
            Base64.decode(parts[0], Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
}
