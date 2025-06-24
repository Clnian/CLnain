package com.example.clnain;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
// import androidx.appcompat.app.AppCompatActivity; // 如果想用Toolbar和AppCompat主题
// import androidx.appcompat.widget.Toolbar;

import com.example.clnain.smartfactory.tools.SmartFactoryApplication;

public class SettingActivity extends Activity {

    private static final String TAG = "SettingActivity";

    // 定义所有输入框控件
    private EditText etServerAddress, etProjectLabel, etCloudAccount, etCloudPassword, etCameraAddress,
            etTempSensorId, etTempThresholdValue, etHumSensorId, etHumThresholdValue, etLightSensorId,
            etLightThresholdValue, etBodySensorId, etLightControllerId, etVentilationControllerId, etAirControllerId;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sharedPref = getSharedPreferences(SmartFactoryApplication.PREFS_NAME, Context.MODE_PRIVATE);

        initViewAndLoadSettings();
    }

    private void initViewAndLoadSettings() {
        Log.d(TAG, "Initializing views and loading settings...");
        etServerAddress = findViewById(R.id.et_server_address);
        etProjectLabel = findViewById(R.id.et_project_label);
        etCloudAccount = findViewById(R.id.et_cloud_account);
        etCloudPassword = findViewById(R.id.et_cloud_account_password); // 正确初始化 etCloudPassword
        etCameraAddress = findViewById(R.id.et_camera_address);
        etTempSensorId = findViewById(R.id.et_temp_sensor_id);
        etTempThresholdValue = findViewById(R.id.et_temp_threshold_value);
        etHumSensorId = findViewById(R.id.et_hum_sensor_id);
        etHumThresholdValue = findViewById(R.id.et_hum_threshold_value);
        etLightSensorId = findViewById(R.id.et_light_sensor_id);
        etLightThresholdValue = findViewById(R.id.et_light_threshold_value);
        etBodySensorId = findViewById(R.id.et_body_sensor_id);
        etLightControllerId = findViewById(R.id.et_light_controller_id);
        etVentilationControllerId = findViewById(R.id.et_ventilation_controller_id);
        etAirControllerId = findViewById(R.id.et_air_controller_id);

        etServerAddress.setText(sharedPref.getString(SmartFactoryApplication.KEY_SERVER_ADDRESS, "api.nlecloud.com"));
        etProjectLabel.setText(sharedPref.getString(SmartFactoryApplication.KEY_PROJECT_LABEL, "1233118"));
        etCloudAccount.setText(sharedPref.getString(SmartFactoryApplication.KEY_CLOUD_ACCOUNT, "19839757907"));
        etCloudPassword.setText(sharedPref.getString(SmartFactoryApplication.KEY_CLOUD_PASSWORD, "Cl026397"));
        etCameraAddress.setText(sharedPref.getString(SmartFactoryApplication.KEY_CAMERA_ADDRESS, "192.168.0.1"));
        etTempSensorId.setText(sharedPref.getString(SmartFactoryApplication.KEY_TEMP_SENSOR_ID, "z_t"));
        etTempThresholdValue.setText(sharedPref.getString(SmartFactoryApplication.KEY_TEMP_THRESHOLD_VALUE, ""));
        etHumSensorId.setText(sharedPref.getString(SmartFactoryApplication.KEY_HUM_SENSOR_ID, "z_h"));
        etHumThresholdValue.setText(sharedPref.getString(SmartFactoryApplication.KEY_HUM_THRESHOLD_VALUE, ""));
        etLightSensorId.setText(sharedPref.getString(SmartFactoryApplication.KEY_LIGHT_SENSOR_ID, "z_l"));
        etLightThresholdValue.setText(sharedPref.getString(SmartFactoryApplication.KEY_LIGHT_THRESHOLD_VALUE, ""));
        etBodySensorId.setText(sharedPref.getString(SmartFactoryApplication.KEY_BODY_SENSOR_ID, ""));
        etLightControllerId.setText(sharedPref.getString(SmartFactoryApplication.KEY_LIGHT_CONTROLLER_ID, ""));
        etVentilationControllerId.setText(sharedPref.getString(SmartFactoryApplication.KEY_VENTILATION_CONTROLLER_ID, ""));
        etAirControllerId.setText(sharedPref.getString(SmartFactoryApplication.KEY_AIR_CONTROLLER_ID, ""));

        Log.d(TAG, "Settings loaded into EditText fields.");
    }

    // (*** 修改位置 1: 移除此处多余的、未初始化的 etCloudAccountPassword 成员变量声明 ***)
    // 在您的代码中，这里可能有一行类似 private EditText etCloudAccountPassword; 的声明，请删除它。
    // 如果这行注释本身就是您代码中的那一行，那它本身就是错误的。
    // 正确的 etCloudPassword 已经在类的顶部与其他 EditText 一起声明了。

    public void onClickSave(View view) {
        Log.d(TAG, "Save button clicked.");

        // 获取用户输入的数据
        String serverAddressValue = etServerAddress.getText().toString().trim();
        String projectLabelValue = etProjectLabel.getText().toString().trim();
        String cloudAccountValue = etCloudAccount.getText().toString().trim();
        // (*** 修改位置 2: 修正获取密码文本的变量名 ***)
        // 原因: 原代码中此处可能错误地使用了未初始化的成员变量 etCloudAccountPassword (如果存在该错误声明的话)，
        //       或者如果不存在错误的重复声明，但报错日志指示第93行出错，那么问题就在于此行使用的变量。
        //       应使用在 initViewAndLoadSettings() 中正确初始化的 etCloudPassword。
        String cloudAccountPasswordValue = etCloudPassword.getText().toString().trim(); // 使用 etCloudPassword
        String cameraAddressValue = etCameraAddress.getText().toString().trim();

        String tempSensorIdValue = etTempSensorId.getText().toString().trim();
        String tempThresholdValueValue = etTempThresholdValue.getText().toString().trim();
        String humSensorIdValue = etHumSensorId.getText().toString().trim();
        String humThresholdValueValue = etHumThresholdValue.getText().toString().trim();
        String lightSensorIdValue = etLightSensorId.getText().toString().trim();
        String lightThresholdValueValue = etLightThresholdValue.getText().toString().trim();
        String bodySensorIdValue = etBodySensorId.getText().toString().trim();
        String lightControllerIdValue = etLightControllerId.getText().toString().trim();
        String ventilationControllerIdValue = etVentilationControllerId.getText().toString().trim();
        String airControllerIdValue = etAirControllerId.getText().toString().trim();

        if (!CheckInput(serverAddressValue, projectLabelValue, cloudAccountValue, cloudAccountPasswordValue)) {
            return;
        }

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(SmartFactoryApplication.KEY_SERVER_ADDRESS, serverAddressValue);
        editor.putString(SmartFactoryApplication.KEY_PROJECT_LABEL, projectLabelValue);
        editor.putString(SmartFactoryApplication.KEY_CLOUD_ACCOUNT, cloudAccountValue);
        editor.putString(SmartFactoryApplication.KEY_CLOUD_PASSWORD, cloudAccountPasswordValue);
        editor.putString(SmartFactoryApplication.KEY_CAMERA_ADDRESS, cameraAddressValue);
        editor.putString(SmartFactoryApplication.KEY_TEMP_SENSOR_ID, tempSensorIdValue);
        editor.putString(SmartFactoryApplication.KEY_TEMP_THRESHOLD_VALUE, tempThresholdValueValue);
        editor.putString(SmartFactoryApplication.KEY_HUM_SENSOR_ID, humSensorIdValue);
        editor.putString(SmartFactoryApplication.KEY_HUM_THRESHOLD_VALUE, humThresholdValueValue);
        editor.putString(SmartFactoryApplication.KEY_LIGHT_SENSOR_ID, lightSensorIdValue);
        editor.putString(SmartFactoryApplication.KEY_LIGHT_THRESHOLD_VALUE, lightThresholdValueValue);
        editor.putString(SmartFactoryApplication.KEY_BODY_SENSOR_ID, bodySensorIdValue);
        editor.putString(SmartFactoryApplication.KEY_LIGHT_CONTROLLER_ID, lightControllerIdValue);
        editor.putString(SmartFactoryApplication.KEY_VENTILATION_CONTROLLER_ID, ventilationControllerIdValue);
        editor.putString(SmartFactoryApplication.KEY_AIR_CONTROLLER_ID, airControllerIdValue);

        editor.apply();

        Log.d(TAG, "Parameters saved to SharedPreferences.");
        showToast(R.string.save_params_success);

        setResult(Activity.RESULT_OK);
        finish();
    }

    private boolean CheckInput(String serverAddress, String projectLabel, String cloudAccount,
                               String cloudAccountPassword) {
        if (serverAddress.isEmpty()) {
            showToast(R.string.server_address_empty);
            return false;
        }
        if (projectLabel.isEmpty()) {
            showToast(R.string.cloud_project_label);
            return false;
        }
        if (cloudAccount.isEmpty()) {
            showToast(R.string.cloud_account_empty);
            return false;
        }
        if (cloudAccountPassword.isEmpty()) {
            showToast(R.string.cloud_account_password_empty);
            return false;
        }
        return true;
    }

    private void showToast(int resId) {
        Toast toast = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}