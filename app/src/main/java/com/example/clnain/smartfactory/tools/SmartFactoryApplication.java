package com.example.clnain.smartfactory.tools;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SmartFactoryApplication extends Application {
    private static final String TAG = "SmartFactoryApp";
    // ★★★ 公开 SharedPreferences 文件名常量 ★★★
    public static final String PREFS_NAME = "SmartFactorySettings"; // 统一的文件名

    // ★★★ 将所有 SharedPreferences Keys 定义为 public static final 常量 ★★★
    // 和你的 activity_setting.xml 以及 SettingActivity.java 中保存的键名对应
    public static final String KEY_SERVER_ADDRESS = "server_address";
    public static final String KEY_PROJECT_LABEL = "project_label";
    public static final String KEY_CLOUD_ACCOUNT = "cloud_account";
    public static final String KEY_CLOUD_PASSWORD = "cloud_account_password";
    public static final String KEY_CAMERA_ADDRESS = "camera_address"; // 新增，根据你的SettingActivity
    public static final String KEY_TEMP_SENSOR_ID = "temp_sensor_id";
    public static final String KEY_TEMP_THRESHOLD_VALUE = "temp_threshold_value"; // 新增
    public static final String KEY_HUM_SENSOR_ID = "hum_sensor_id";
    public static final String KEY_HUM_THRESHOLD_VALUE = "hum_threshold_value"; // 新增
    public static final String KEY_LIGHT_SENSOR_ID = "light_sensor_id";
    public static final String KEY_LIGHT_THRESHOLD_VALUE = "light_threshold_value"; // 新增
    public static final String KEY_BODY_SENSOR_ID = "body_sensor_id"; // 新增
    public static final String KEY_LIGHT_CONTROLLER_ID = "light_controller_id"; // 新增
    public static final String KEY_VENTILATION_CONTROLLER_ID = "ventilation_controller_id"; // 新增
    public static final String KEY_AIR_CONTROLLER_ID = "air_controller_id"; // 新增

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initializing SharedPreferences with name: " + PREFS_NAME);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // 提供一个公共方法获取 SharedPreferences 实例，SettingActivity 可以使用
    public SharedPreferences getSharedPreferencesInstance() {
        if (sharedPreferences == null) {
            Log.w(TAG, "SharedPreferences accessed before Application onCreate or was null; Re-initializing.");
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    // --- Getter 方法 (确保这些方法MainActivity会用到) ---
    public String getServerAddress() {
        return getSharedPreferencesInstance().getString(KEY_SERVER_ADDRESS, "api.nlecloud.com"); // 默认值
    }
    public String getProjectLabel() {
        return getSharedPreferencesInstance().getString(KEY_PROJECT_LABEL, ""); // 默认空
    }
    public String getCloudAccount() {
        return getSharedPreferencesInstance().getString(KEY_CLOUD_ACCOUNT, "");
    }
    public String getCloudAccountPassword() {
        return getSharedPreferencesInstance().getString(KEY_CLOUD_PASSWORD, "");
    }
    public String getTempSensorId() {
        return getSharedPreferencesInstance().getString(KEY_TEMP_SENSOR_ID, "z_t"); // 匹配你XML中的默认值
    }
    public String getHumSensorId() {
        return getSharedPreferencesInstance().getString(KEY_HUM_SENSOR_ID, "z_h"); // 匹配你XML中的默认值
    }
    public String getLightSensorId() {
        return getSharedPreferencesInstance().getString(KEY_LIGHT_SENSOR_ID, "z_l"); // 匹配你XML中的默认值
    }
    // ★★★ 添加其他 MainActivity 可能需要的 getter (例如控制器ID，如果不在CloudHelper中硬编码) ★★★
    public String getVentilationControllerId() {
        return getSharedPreferencesInstance().getString(KEY_VENTILATION_CONTROLLER_ID, "");
    }
    public String getAirControllerId() {
        return getSharedPreferencesInstance().getString(KEY_AIR_CONTROLLER_ID, "");
    }
    public String getLightControllerId() {
        return getSharedPreferencesInstance().getString(KEY_LIGHT_CONTROLLER_ID, "");
    }


    // --- Setter 方法 (可选，如果 SettingActivity 直接编辑 SharedPreferences 则不需要) ---
    // 为了封装性，提供 Setter 是好的
    public void setString(String key, String value) {
        getSharedPreferencesInstance().edit().putString(key, value).apply();
    }
    public String getString(String key, String defaultValue) {
        return getSharedPreferencesInstance().getString(key, defaultValue);
    }
    // 你也可以为每个具体配置项写单独的 setter，例如:
    // public void setTempSensorId(String id) { setString(KEY_TEMP_SENSOR_ID, id); }
}