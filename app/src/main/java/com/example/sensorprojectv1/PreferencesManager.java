package com.example.sensorprojectv1;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "SafeWalkPreferences";
    private static final String KEY_SOUND_ALERT = "sound_alert";
    private static final String KEY_VIBRATION_ALERT = "vibration_alert";
    private static final String KEY_PARTICIPATE = "participate";
    private static final String KEY_USER_LOGGED_IN = "user_logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    private SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Alertas de sonido
    public void setSoundAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND_ALERT, enabled).apply();
    }

    public boolean isSoundAlertEnabled() {
        return preferences.getBoolean(KEY_SOUND_ALERT, true);
    }

    // Alertas de vibración
    public void setVibrationAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIBRATION_ALERT, enabled).apply();
    }

    public boolean isVibrationAlertEnabled() {
        return preferences.getBoolean(KEY_VIBRATION_ALERT, true);
    }

    // Participación en el estudio
    public void setParticipateEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PARTICIPATE, enabled).apply();
    }

    public boolean isParticipateEnabled() {
        return preferences.getBoolean(KEY_PARTICIPATE, false);
    }

    // Usuario
    public void setUserLoggedIn(boolean loggedIn) {
        preferences.edit().putBoolean(KEY_USER_LOGGED_IN, loggedIn).apply();
    }

    public boolean isUserLoggedIn() {
        return preferences.getBoolean(KEY_USER_LOGGED_IN, false);
    }

    public void setUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    public void logout() {
        preferences.edit()
                .putBoolean(KEY_USER_LOGGED_IN, false)
                .putString(KEY_USER_NAME, "")
                .putString(KEY_USER_EMAIL, "")
                .apply();
    }
}
