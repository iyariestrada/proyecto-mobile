package com.example.sensorprojectv1;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "SafeWalkPreferences";
    private static final String KEY_SOUND_ALERT = "sound_alert";
    private static final String KEY_VIBRATION_ALERT = "vibration_alert";
    private static final String KEY_PARTICIPATE = "participate";
    private static final String KEY_USER_LOGGED_IN = "user_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_DEVICE_UUID = "device_uuid";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_SESSION_START = "session_start";

    private SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setSoundAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND_ALERT, enabled).apply();
    }

    public boolean isSoundAlertEnabled() {
        return preferences.getBoolean(KEY_SOUND_ALERT, true);
    }

    public void setVibrationAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIBRATION_ALERT, enabled).apply();
    }

    public boolean isVibrationAlertEnabled() {
        return preferences.getBoolean(KEY_VIBRATION_ALERT, true);
    }

    public void setParticipateEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PARTICIPATE, enabled).apply();
    }

    public boolean isParticipateEnabled() {
        return preferences.getBoolean(KEY_PARTICIPATE, false);
    }

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

    public void setUserId(int id) {
        preferences.edit().putInt(KEY_USER_ID, id).apply();
    }

    public int getUserId() {
        return preferences.getInt(KEY_USER_ID, -1);
    }

    public void setUserToken(String token) {
        preferences.edit().putString(KEY_USER_TOKEN, token).apply();
    }

    public String getUserToken() {
        return preferences.getString(KEY_USER_TOKEN, "");
    }

    public void setUserType(String type) {
        preferences.edit().putString(KEY_USER_TYPE, type).apply();
    }

    public String getUserType() {
        return preferences.getString(KEY_USER_TYPE, "");
    }

    public void setDeviceId(long id) {
        preferences.edit().putLong(KEY_DEVICE_ID, id).apply();
    }

    public long getDeviceId() {
        return preferences.getLong(KEY_DEVICE_ID, -1);
    }

    public void setDeviceUUID(String uuid) {
        preferences.edit().putString(KEY_DEVICE_UUID, uuid).apply();
    }

    public String getDeviceUUID() {
        return preferences.getString(KEY_DEVICE_UUID, "");
    }

    public void setSessionId(long id) {
        preferences.edit().putLong(KEY_SESSION_ID, id).apply();
    }

    public long getSessionId() {
        return preferences.getLong(KEY_SESSION_ID, -1);
    }

    public void setSessionStart(long timestamp) {
        preferences.edit().putLong(KEY_SESSION_START, timestamp).apply();
    }

    public long getSessionStart() {
        return preferences.getLong(KEY_SESSION_START, 0);
    }

    public void logout() {
        preferences.edit()
                .putBoolean(KEY_USER_LOGGED_IN, false)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_NAME, "")
                .putString(KEY_USER_EMAIL, "")
                .putString(KEY_USER_TOKEN, "")
                .putString(KEY_USER_TYPE, "")
                .putLong(KEY_DEVICE_ID, -1)
                .putString(KEY_DEVICE_UUID, "")
                .putLong(KEY_SESSION_ID, -1)
                .putLong(KEY_SESSION_START, 0)
                .apply();
    }
}
