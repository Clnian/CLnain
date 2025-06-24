package com.example.clnain;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

// 导入项目工具类
import com.example.clnain.smartfactory.tools.CloudHelper;
import com.example.clnain.smartfactory.tools.DatabaseHelper;
import com.example.clnain.smartfactory.tools.SmartFactoryApplication;

// 导入NLE SDK中的User实体类，用于登录回调
import cn.com.newland.nle_sdk.responseEntity.User;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 日志TAG
    private static final String TAG = "MainActivity";
    // 启动SettingActivity的请求码
    static final private int REQUEST_CODE_SETTINGS = 1001;
    // Handler消息类型：更新UI和数据库
    private static final int MSG_UPDATE_UI_AND_DB = 1;

    // UI控件变量
    private TextView tvLightValue, tvTempValue, tvHumValue; // 显示传感器数值的TextView
    private Spinner spVentilation, spAc, spLight;          // 控制设备的Spinner
    private ImageView imgFan, imgAc, imgLightView;         // 显示设备状态的ImageView

    // 动画变量
    private Animation rotateAnimation;                     // 风扇旋转动画
    private AnimationDrawable acFrameAnimation;            // 空调帧动画
    private ObjectAnimator lightPropertyAnimator;          // 灯光属性动画（例如闪烁）

    // 数据和辅助类变量
    private String lightValue = "N/A", tempValue = "N/A", humValue = "N/A"; // 传感器数据，默认为"N/A"
    private CloudHelper cloudHelper;                       // 云平台交互助手
    private SmartFactoryApplication smartFactoryApp;       // 自定义Application类实例，用于获取全局配置
    private DatabaseHelper databaseHelper;                 // 数据库助手
    private Timer dataRefreshTimer;                        // 定时器，用于周期性获取传感器数据

    // UI更新处理器，运行在主线程
    private final Handler uiUpdateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_UPDATE_UI_AND_DB) {
                updateSensorUI(); // 更新UI上显示的传感器数据
                // 只有当所有传感器值都有效时才插入数据库 (不为null, "N/A", 或 "Err")
                if (tempValue != null && !tempValue.equals("N/A") && !tempValue.equals("Err") &&
                        humValue != null && !humValue.equals("N/A") && !humValue.equals("Err") &&
                        lightValue != null && !lightValue.equals("N/A") && !lightValue.equals("Err")) {
                    if (databaseHelper != null) {
                        // 调用DatabaseHelper的insert方法保存数据
                        databaseHelper.insert(MainActivity.this, tempValue, humValue, lightValue);
                        Log.d(TAG, "数据已尝试存入数据库: T=" + tempValue + ", H=" + humValue + ", L=" + lightValue);
                    }
                } else {
                    Log.w(TAG, "部分传感器数据无效或未获取，不存入数据库. T:" + tempValue + " H:" + humValue + " L:" + lightValue);
                }
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置布局文件
        Log.d(TAG, "onCreate: Activity正在启动.");

        // 初始化操作
        initializeUI();           // 初始化UI控件
        initializeHelpers();      // 初始化辅助类 (Application, CloudHelper, DatabaseHelper)
        initializeAnimations();   // 初始化动画效果
        setupSpinners();          // 设置Spinner的适配器和监听器
        startLoadingCloudDataFlow(); // 开始加载云端数据（登录、获取传感器信息）
        Log.d(TAG, "onCreate: 初始化完成.");
    }

    /**
     * 初始化UI控件，并为需要点击事件的TextView设置监听器。
     */
    private void initializeUI() {
        Log.d(TAG, "正在初始化UI组件...");
        tvLightValue = findViewById(R.id.tv_light_value);
        tvTempValue = findViewById(R.id.tv_temp_value);
        tvHumValue = findViewById(R.id.tv_humility_value); // XML中湿度TextView的ID是tv_humility_value
        imgFan = findViewById(R.id.img_fan);
        imgAc = findViewById(R.id.img_ac);
        imgLightView = findViewById(R.id.img_light); // XML中灯光ImageView的ID是img_light
        spVentilation = findViewById(R.id.sp_ventilation_control);
        spAc = findViewById(R.id.sp_air_control);
        spLight = findViewById(R.id.sp_light_control);

        // 为显示传感器数据的TextView设置点击监听器，用于跳转到ChartActivity
        if (tvLightValue != null) tvLightValue.setOnClickListener(this);
        if (tvTempValue != null) tvTempValue.setOnClickListener(this);
        if (tvHumValue != null) tvHumValue.setOnClickListener(this);

        // 检查是否有UI控件未找到，打印错误日志
        if (tvLightValue == null || tvTempValue == null || tvHumValue == null ||
                imgFan == null || imgAc == null || imgLightView == null ||
                spVentilation == null || spAc == null || spLight == null) {
            Log.e(TAG, "UI初始化错误: 一个或多个View未找到!");
            showToast("UI组件初始化失败!"); // 提示用户
        }
    }

    /**
     * 初始化辅助类：SmartFactoryApplication, CloudHelper, DatabaseHelper。
     */
    private void initializeHelpers() {
        Log.d(TAG, "正在初始化辅助类...");
        try {
            // 获取自定义的Application实例
            smartFactoryApp = (SmartFactoryApplication) getApplication();
        } catch (ClassCastException e) {
            Log.e(TAG, "严重错误: Application类不是SmartFactoryApplication类型! 请检查AndroidManifest.xml中application标签的android:name属性。", e);
            showToast("应用配置错误!");
            finish(); // 退出Activity，因为后续操作依赖smartFactoryApp
            return;
        }
        // 创建CloudHelper和DatabaseHelper实例
        cloudHelper = new CloudHelper();
        databaseHelper = new DatabaseHelper(this); // DatabaseHelper需要Context
    }

    /**
     * 初始化各种动画效果。
     */
    private void initializeAnimations() {
        Log.d(TAG, "正在初始化动画...");
        // 风扇旋转动画
        rotateAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator()); // 线性插值器，匀速旋转
        rotateAnimation.setDuration(1500); // 旋转一周的时间
        rotateAnimation.setRepeatCount(Animation.INFINITE); // 无限循环
        rotateAnimation.setFillAfter(true); // 动画结束后保持最终状态

        // 空调帧动画
        if (imgAc != null) {
            try {
                imgAc.setImageResource(R.drawable.frame_anim); // 设置帧动画资源
                Drawable frameDrawable = imgAc.getDrawable();
                if (frameDrawable instanceof AnimationDrawable) {
                    acFrameAnimation = (AnimationDrawable) frameDrawable;
                    acFrameAnimation.stop(); // 默认停止动画
                    imgAc.setImageResource(R.drawable.ac1); // 设置空调关闭时的静态图
                } else {
                    acFrameAnimation = null;
                    Log.e(TAG, "imgAc的drawable不是AnimationDrawable类型! 请检查frame_anim.xml文件。");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "空调的动画资源未找到 (frame_anim 或 ac1)", e);
                acFrameAnimation = null; // 确保即使资源找不到也不会崩溃
            }
        } else {
            Log.w(TAG, "imgAc为空，无法初始化空调动画。");
        }

        // 灯光属性动画（示例：透明度闪烁）
        if (imgLightView != null) {
            lightPropertyAnimator = ObjectAnimator.ofFloat(imgLightView, "alpha", 1f, 0.2f, 1f); // 从不透明到半透明再到不透明
            lightPropertyAnimator.setDuration(2000); // 动画周期
            lightPropertyAnimator.setRepeatCount(ObjectAnimator.INFINITE); // 无限循环
            lightPropertyAnimator.setRepeatMode(ObjectAnimator.REVERSE); // 反向重复动画
            lightPropertyAnimator.addListener(new AnimatorListenerAdapter() {}); // 可以添加监听器处理动画事件
            // lightPropertyAnimator.start(); // 按需启动，例如灯关闭时可以启动闪烁
        } else {
            Log.w(TAG, "imgLightView为空，无法初始化灯光动画。");
        }
    }

    /**
     * 设置三个控制Spinner的适配器和选项选择监听器。
     */
    private void setupSpinners() {
        Log.d(TAG, "正在设置Spinners...");
        // 确保Spinner和smartFactoryApp不为空
        if (spVentilation == null || spAc == null || spLight == null || smartFactoryApp == null) {
            Log.e(TAG, "一个或多个Spinner或smartFactoryApp为空，跳过Spinner设置。");
            return;
        }

        Resources res = getResources();
        String[] controlStatus; // 控制状态数组，例如 {"打开", "关闭"}
        try {
            // 从 R.array.control_status 资源获取控制状态 (需在 arrays.xml 或 strings.xml 中定义)
            controlStatus = res.getStringArray(R.array.control_status);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "字符串数组资源 R.array.control_status 未找到!", e);
            showToast("控制状态资源加载失败!");
            return;
        }

        // 创建ArrayAdapter
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, controlStatus);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // 设置下拉列表样式

        // 创建Spinner选项选择监听器
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = adapter.getItem(position); // 获取选中的状态 ("打开" 或 "关闭")
                if (selectedStatus == null) return;

                int parentId = parent.getId(); // 获取触发事件的Spinner的ID
                // 根据Spinner的ID调用相应的控制处理方法
                if (parentId == R.id.sp_ventilation_control) {
                    handleVentilationControl(selectedStatus);
                } else if (parentId == R.id.sp_air_control) {
                    handleAcControl(selectedStatus);
                } else if (parentId == R.id.sp_light_control) {
                    handleLightControl(selectedStatus);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* 未选择任何项时的回调，此处不处理 */ }
        };

        // 为每个Spinner设置适配器、监听器和默认选项
        spVentilation.setAdapter(adapter);
        spVentilation.setOnItemSelectedListener(listener);
        spVentilation.setSelection(1, false); // 默认选中第二项 (假设是"关闭", 索引从0开始)

        spAc.setAdapter(adapter);
        spAc.setOnItemSelectedListener(listener);
        spAc.setSelection(1, false);

        spLight.setAdapter(adapter);
        spLight.setOnItemSelectedListener(listener);
        spLight.setSelection(1, false);
    }

    /**
     * 通用的设备控制指令发送方法。
     * @param deviceName 设备名称 (用于日志)
     * @param controllerIdKey SharedPreferences中存储该设备控制器ID的键
     * @param deviceImageView 关联的ImageView (用于动画)
     * @param status 控制状态 ("打开" 或 "关闭")
     * @param animation 旋转动画 (用于风扇)
     * @param frameAnimation 帧动画 (用于空调)
     * @param propertyAnimation 属性动画 (用于灯)
     */
    private void sendControlCommand(String deviceName, String controllerIdKey, ImageView deviceImageView, String status,
                                    Animation animation, AnimationDrawable frameAnimation, ObjectAnimator propertyAnimation) {
        // 检查smartFactoryApp和cloudHelper是否已初始化
        if (smartFactoryApp == null || cloudHelper == null) {
            Log.e(TAG, "smartFactoryApp 或 cloudHelper 未初始化。无法为 " + deviceName + " 发送指令");
            showToast("应用配置错误，无法控制设备");
            return;
        }

        String controllerId; // 设备的API Tag或ID
        // 根据传入的controllerIdKey从smartFactoryApp获取具体的控制器ID
        switch (controllerIdKey) {
            case SmartFactoryApplication.KEY_VENTILATION_CONTROLLER_ID:
                controllerId = smartFactoryApp.getVentilationControllerId();
                break;
            case SmartFactoryApplication.KEY_AIR_CONTROLLER_ID:
                controllerId = smartFactoryApp.getAirControllerId();
                break;
            case SmartFactoryApplication.KEY_LIGHT_CONTROLLER_ID:
                controllerId = smartFactoryApp.getLightControllerId();
                break;
            default:
                Log.e(TAG, "未知的控制器ID键: " + controllerIdKey);
                showToast("未知的控制器类型");
                return;
        }

        Log.d(TAG, deviceName + " 的控制器ID (来自设置): " + controllerId);

        // 检查控制器ID是否已配置
        if (isEmpty(controllerId)) {
            showToast(deviceName + " 控制器ID未配置，请前往设置！");
            Log.w(TAG, deviceName + " 的控制器ID为空。");
            // 可选：如果用户尝试“打开”但ID未配置，则将Spinner重置为“关闭”状态
            // 例如，对于通风Spinner:
            // if ("打开".equals(status) && spVentilation != null && controllerIdKey.equals(SmartFactoryApplication.KEY_VENTILATION_CONTROLLER_ID)) {
            //    spVentilation.setSelection(1, false); // 假设索引1是“关闭”
            // }
            return;
        }

        int command = "打开".equals(status) ? 1 : 0; // 将"打开"/"关闭"映射为指令 (通常1为开, 0为关)

        String serverAddress = smartFactoryApp.getServerAddress();
        String projectLabel = smartFactoryApp.getProjectLabel();

        // 检查Token是否存在，确保用户已登录
        if (!isEmpty(cloudHelper.getToken())) {
            Log.d(TAG, "正在为 " + deviceName + " 调用onOff: 地址=" + serverAddress + ", 项目=" + projectLabel + ", ID=" + controllerId + ", 指令=" + command);
            // 调用CloudHelper的onOff方法发送控制指令
            cloudHelper.onOff(this, serverAddress, projectLabel, controllerId, command);
        } else {
            showToast("用户未登录或云服务助手配置错误！无法控制 " + deviceName);
            Log.e(TAG, "CloudHelper的token为空，无法控制 " + deviceName);
            // 可选：重置Spinner状态
            return;
        }

        // --- UI动画更新 ---
        // 注意：这部分UI更新是“乐观更新”，即发送指令后立即更新UI。
        // 更健壮的做法是根据cloudHelper.onOff的成功/失败回调结果来更新UI。
        // 为简化当前修改，暂时保持乐观更新。

        if (deviceImageView != null) { // 确保ImageView不为空才操作动画
            if (deviceName.equals("通风系统")) {
                if ("打开".equals(status)) {
                    if (animation != null) deviceImageView.startAnimation(animation); // 开启动画
                } else {
                    deviceImageView.clearAnimation(); // 清除动画
                }
            } else if (deviceName.equals("空调系统")) {
                if ("打开".equals(status)) {
                    if (frameAnimation != null) {
                        // 可能需要重新设置Drawable资源以确保动画从头开始，或检查是否已在运行
                        deviceImageView.setImageResource(R.drawable.frame_anim);
                        Drawable d = deviceImageView.getDrawable();
                        if (d instanceof AnimationDrawable) {
                            ((AnimationDrawable) d).start(); // 启动帧动画
                        }
                    }
                } else {
                    if (frameAnimation != null) frameAnimation.stop(); // 停止帧动画
                    deviceImageView.setImageResource(R.drawable.ac1); // 设置为关闭状态的静态图
                }
            } else if (deviceName.equals("照明系统")) {
                if ("打开".equals(status)) {
                    if (propertyAnimation != null && propertyAnimation.isStarted()) {
                        propertyAnimation.cancel(); // 如果之前在闪烁（关闭状态），则取消
                    }
                    deviceImageView.setImageResource(R.drawable.light_on); // 设置为灯亮图片
                    deviceImageView.setAlpha(1f); // 完全不透明
                } else {
                    // 如果灯光关闭时也用同一个属性动画来闪烁，可以在这里启动它
                    // if (propertyAnimation != null && !propertyAnimation.isStarted()) propertyAnimation.start();
                    // 否则，直接设置为灯灭图片
                    if (propertyAnimation != null && propertyAnimation.isStarted()) {
                        propertyAnimation.cancel(); // 如果之前在闪烁（打开状态），则取消
                    }
                    deviceImageView.setImageResource(R.drawable.light_off); // 设置为灯灭图片
                    deviceImageView.setAlpha(1f); // 完全不透明
                }
            }
        } else {
            Log.w(TAG, deviceName + " 的ImageView为空，跳过动画更新。");
        }
    }

    /**
     * 处理通风系统控制。
     * @param status "打开" 或 "关闭"
     */
    private void handleVentilationControl(String status) {
        Log.d(TAG, "通风控制: " + status);
        sendControlCommand("通风系统", SmartFactoryApplication.KEY_VENTILATION_CONTROLLER_ID, imgFan, status, rotateAnimation, null, null);
    }

    /**
     * 处理空调系统控制。
     * @param status "打开" 或 "关闭"
     */
    private void handleAcControl(String status) {
        Log.d(TAG, "空调控制: " + status);
        sendControlCommand("空调系统", SmartFactoryApplication.KEY_AIR_CONTROLLER_ID, imgAc, status, null, acFrameAnimation, null);
    }

    /**
     * 处理照明系统控制。
     * @param status "打开" 或 "关闭"
     */
    private void handleLightControl(String status) {
        Log.d(TAG, "照明控制: " + status);
        sendControlCommand("照明系统", SmartFactoryApplication.KEY_LIGHT_CONTROLLER_ID, imgLightView, status, null, null, lightPropertyAnimator);
    }

    /**
     * 开始加载云平台数据的流程：检查配置、登录、获取传感器数据。
     */
    private void startLoadingCloudDataFlow() {
        Log.i(TAG, "正在启动云数据加载流程...");
        // 确保 smartFactoryApp 和 cloudHelper 已初始化
        if (smartFactoryApp == null || cloudHelper == null) {
            Log.e(TAG, "SmartFactoryApplication或CloudHelper未初始化!");
            showToast("应用内部配置错误。");
            return;
        }
        // 从Application类获取云平台配置参数
        String address = smartFactoryApp.getServerAddress();
        String account = smartFactoryApp.getCloudAccount();
        String password = smartFactoryApp.getCloudAccountPassword();
        String projectLabel = smartFactoryApp.getProjectLabel();

        // 检查核心配置参数是否完整
        if (isEmpty(address) || isEmpty(account) || isEmpty(password) || isEmpty(projectLabel)) {
            showToast("云平台参数未完整配置，请前往设置。");
            Log.w(TAG, "云平台参数未配置完全。地址:" + address + ", 账户:" + account + ", 项目:" + projectLabel);
            updateSensorUI(); // 更新UI显示为N/A
            return;
        }

        // 如果没有有效的Token，则尝试登录
        if (isEmpty(cloudHelper.getToken())) {
            Log.i(TAG, "无有效Token，正在尝试登录...");
            showToast("正在登录云平台...");
            cloudHelper.signIn(this, address, account, password, new CloudHelper.DCallback<User>() {
                @Override
                public void onSuccess(User userResult) {
                    Log.i(TAG, "登录成功。Token: " + cloudHelper.getToken());
                    showToast("登录成功");
                    scheduleDataRefresh(); // 登录成功后，开始定时刷新传感器数据
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e(TAG, "登录失败: " + errorMsg);
                    showToast("登录失败: " + errorMsg); // 显示来自CloudHelper的具体错误信息
                    updateSensorUI(); // 更新UI显示错误状态
                }
            });
        } else {
            // 如果已有Token，直接开始数据刷新
            Log.i(TAG, "已有Token (" + cloudHelper.getToken().substring(0, Math.min(cloudHelper.getToken().length(),10)) + "...), 直接开始数据刷新。");
            scheduleDataRefresh();
        }
    }

    /**
     * 检查字符串是否为null或空。
     * @param s 待检查的字符串
     * @return 如果字符串为null或去除首尾空格后为空，则返回true；否则返回false。
     */
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 安排定时任务以周期性获取传感器数据。
     */
    private void scheduleDataRefresh() {
        // 如果已有定时器，先取消
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null; // 设置为null，以便可以重新创建
        }
        dataRefreshTimer = new Timer(); // 创建新的Timer实例
        Log.i(TAG, "正在安排数据刷新任务，每5秒执行一次。");
        dataRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetchSensorDataFromCloud(); // 定时执行获取云端数据的操作
            }
        }, 0, 5000); // 0表示立即执行第一次，5000表示之后每5秒执行一次
    }

    /**
     * 从云平台获取所有配置的传感器的数据。
     */
    private void fetchSensorDataFromCloud() {
        // 检查依赖项和登录状态
        if (smartFactoryApp == null || cloudHelper == null || isEmpty(cloudHelper.getToken())) {
            Log.w(TAG, "无法获取传感器数据：应用/云助手未就绪或未登录。");
            // 可选：尝试重新登录或引导用户
            // runOnUiThread(() -> showToast("无法获取数据，请检查登录状态和网络"));
            return;
        }
        Log.d(TAG, "正在从云端获取传感器数据...");

        String address = smartFactoryApp.getServerAddress();
        String projectLabel = smartFactoryApp.getProjectLabel();

        // 从Application类获取各个传感器的API Tag
        String lightSensorApiTag = smartFactoryApp.getLightSensorId();
        String tempSensorApiTag = smartFactoryApp.getTempSensorId();
        String humSensorApiTag = smartFactoryApp.getHumSensorId();

        // 如果所有传感器ID都未配置，则不进行获取
        if (isEmpty(lightSensorApiTag) && isEmpty(tempSensorApiTag) && isEmpty(humSensorApiTag)) {
            Log.w(TAG, "所有传感器的API Tag均未在SmartFactoryApplication中配置。");
            // runOnUiThread(() -> showToast("所有传感器ID均未配置")); // 通知用户
            // 同时更新UI为未配置状态
            lightValue = "N/CFG"; tempValue = "N/CFG"; humValue = "N/CFG";
            uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
            return;
        }

        // 获取光照传感器数据
        if (!isEmpty(lightSensorApiTag)) {
            cloudHelper.getSensorData(this, address, projectLabel, lightSensorApiTag, new CloudHelper.DCallback<String>() {
                @Override public void onSuccess(String value) {
                    Log.d(TAG, "光照数据接收: " + value);
                    lightValue = value;
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB); // 通知Handler更新UI和数据库
                }
                @Override public void onFailure(String errorMsg) {
                    Log.e(TAG, "获取光照数据失败 (" + lightSensorApiTag + "): " + errorMsg);
                    lightValue = "Err"; // 设置为错误状态
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
                }
            });
        } else {
            Log.w(TAG, "光照传感器ID未配置。");
            lightValue = "N/CFG"; // Not Configured
            uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB); // 仍然更新UI以显示未配置
        }

        // 获取温度传感器数据
        if (!isEmpty(tempSensorApiTag)) {
            cloudHelper.getSensorData(this, address, projectLabel, tempSensorApiTag, new CloudHelper.DCallback<String>() {
                @Override public void onSuccess(String value) {
                    Log.d(TAG, "温度数据接收: " + value);
                    tempValue = value;
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
                }
                @Override public void onFailure(String errorMsg) {
                    Log.e(TAG, "获取温度数据失败 (" + tempSensorApiTag + "): " + errorMsg);
                    tempValue = "Err";
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
                }
            });
        } else {
            Log.w(TAG, "温度传感器ID未配置。");
            tempValue = "N/CFG";
            uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
        }

        // 获取湿度传感器数据
        if (!isEmpty(humSensorApiTag)) {
            cloudHelper.getSensorData(this, address, projectLabel, humSensorApiTag, new CloudHelper.DCallback<String>() {
                @Override public void onSuccess(String value) {
                    Log.d(TAG, "湿度数据接收: " + value);
                    humValue = value;
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
                }
                @Override public void onFailure(String errorMsg) {
                    Log.e(TAG, "获取湿度数据失败 (" + humSensorApiTag + "): " + errorMsg);
                    humValue = "Err";
                    uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
                }
            });
        } else {
            Log.w(TAG, "湿度传感器ID未配置。");
            humValue = "N/CFG";
            uiUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI_AND_DB);
        }
    }

    /**
     * 更新UI上显示的传感器数值。
     */
    private void updateSensorUI() {
        Log.d(TAG, "正在更新UI: 温度=" + tempValue + ", 湿度=" + humValue + ", 光照=" + lightValue);
        if (tvTempValue != null) tvTempValue.setText(tempValue);
        if (tvHumValue != null) tvHumValue.setText(humValue);
        if (tvLightValue != null) tvLightValue.setText(lightValue);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu); // 加载菜单资源 R.menu.menu_main
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        // 如果点击的是设置菜单项 (R.id.action_setting)
        if (itemId == R.id.action_setting) {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS); // 启动SettingActivity并等待结果
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 检查是否是从SettingActivity返回，并且结果是OK
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "从设置返回，重新加载云数据流程。");
            if (cloudHelper != null) cloudHelper.clearToken(); // 清除旧Token，强制重新登录以应用新配置
            if (dataRefreshTimer != null) {
                dataRefreshTimer.cancel(); // 取消旧的定时器
                dataRefreshTimer = null;   // 设置为null，以便scheduleDataRefresh可以创建新的
            }
            // 重新启动数据加载流程，它会读取最新的配置（如服务器地址、账户等）
            startLoadingCloudDataFlow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity已恢复。");
        // 每次Activity恢复时，都尝试启动数据加载流程。
        // startLoadingCloudDataFlow内部会判断是否已登录或需要重新登录。
        // 如果是从SettingActivity成功返回，onActivityResult已经处理了重载。
        // 这个调用主要确保如果应用只是暂停后恢复（非从设置返回），数据流能继续或在需要时重新初始化。
        if (smartFactoryApp != null && cloudHelper != null) {
            startLoadingCloudDataFlow();
        } else {
            // 这是一个防御性措施，理论上onCreate应该已经初始化了它们。
            // 但如果由于某些异常生命周期事件导致它们为null，尝试重新初始化。
            Log.e(TAG, "onResume: smartFactoryApp或cloudHelper为空，可能需要重新初始化。正在尝试...");
            initializeHelpers(); // 谨慎调用，确保此操作是幂等的或安全的
            if (smartFactoryApp != null && cloudHelper != null) { // 再次检查
                startLoadingCloudDataFlow();
            } else {
                showToast("应用核心组件加载失败，请重启应用");
                Log.e(TAG, "onResume: 重新初始化后，smartFactoryApp或cloudHelper仍然为空。");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity已暂停。");
        // Activity暂停时，取消数据刷新定时器以节省资源
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            // dataRefreshTimer = null; // 如果不设置为null，onResume时若timer未完成，可能不会创建新的。
            // 但由于scheduleDataRefresh在开始时会检查并取消旧的，所以设为null更保险。
            Log.d(TAG, "数据刷新定时器已在onPause中取消。");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity正在销毁。");
        // Activity销毁时，彻底清理资源
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }
        if (databaseHelper != null) {
            databaseHelper.close(); // 关闭数据库连接
            Log.d(TAG, "DatabaseHelper已在onDestroy中关闭。");
        }
        // 可在此处释放其他资源，如取消正在进行的动画等
    }

    /**
     * 显示Toast消息，确保在UI线程执行。
     * @param message 要显示的消息
     */
    private void showToast(final String message) {
        if (message == null || message.isEmpty()) return; // 避免显示空消息

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 如果当前已在主线程，直接显示
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        } else {
            // 如果在其他线程，则切换到主线程显示
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 处理TextView（传感器数值）的点击事件。
     * @param view 被点击的View
     */
    @Override
    public void onClick(View view) {
        // 创建启动ChartActivity的Intent
        Intent intent = new Intent(MainActivity.this, ChartActivity.class);
        int viewId = view.getId(); // 获取被点击View的ID

        // 根据被点击的TextView，设置传递给ChartActivity的数据类型 ("type")
        if (viewId == R.id.tv_temp_value) {
            intent.putExtra("type", "温度");
            startActivity(intent); // 启动ChartActivity
        } else if (viewId == R.id.tv_humility_value) {
            intent.putExtra("type", "湿度");
            startActivity(intent);
        } else if (viewId == R.id.tv_light_value) {
            intent.putExtra("type", "光照");
            startActivity(intent);
        }
    }
}