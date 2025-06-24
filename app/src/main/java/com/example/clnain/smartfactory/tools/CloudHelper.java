package com.example.clnain.smartfactory.tools;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

// 导入NLE SDK相关类
import cn.com.newland.nle_sdk.requestEntity.SignIn;
import cn.com.newland.nle_sdk.responseEntity.User;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NCallBack;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;
import cn.com.newland.nle_sdk.responseEntity.SensorInfo;

import retrofit2.Call; // 确保这是正确的Retrofit Call类

public class CloudHelper {

    private static final String TAG = "CloudHelper"; // 日志TAG
    private String token = ""; // 存储登录后获取的AccessToken

    /**
     * 自定义回调接口，用于异步操作的结果处理。
     * @param <T> 成功时返回的数据类型
     */
    public interface DCallback<T> {
        void onSuccess(T result); // 成功回调
        void onFailure(String errorMsg); // 失败回调，错误信息为字符串
    }

    /**
     * 确保给定的服务器地址以 "https://" 开头。
     * 如果地址为null，则返回null。
     * 如果地址不包含 "http://" 或 "https://" 前缀，则默认添加 "https://"。
     * @param address 原始服务器地址
     * @return 处理后的、包含 "https://" 前缀的服务器地址
     */
    private String ensureHttps(String address) {
        if (address == null) {
            Log.w(TAG, "ensureHttps: input address is null");
            return null;
        }
        String lowerAddress = address.toLowerCase();
        if (!lowerAddress.startsWith("http://") && !lowerAddress.startsWith("https://")) {
            // 默认添加 https://，因为它更安全且是现代API的趋势
            return "https://" + address;
        }
        // 如果希望强制将 http:// 替换为 https://, 可以取消下面的注释
        // if (lowerAddress.startsWith("http://")) {
        //     Log.w(TAG, "ensureHttps: replacing http:// with https:// for address: " + address);
        //     return "https://" + address.substring("http://".length());
        // }
        return address; // 如果已经是http或https，则直接返回
    }

    /**
     * 用户登录到云平台。
     * @param c Context
     * @param address 服务器地址
     * @param account 账户名
     * @param pwd 密码
     * @param callbackParam 登录结果的回调
     */
    public void signIn(final Context c, String address, String account, String pwd, final DCallback<User> callbackParam) {
        Log.d(TAG, "signIn: 准备登录... 账户: " + account);
        // 参数校验
        if (address == null || address.isEmpty() || account == null || account.isEmpty() || pwd == null || pwd.isEmpty()) {
            Log.e(TAG, "signIn: 服务器地址、账户或密码不能为空。");
            if (callbackParam != null) callbackParam.onFailure("登录参数不能为空");
            return;
        }

        String fullAddress = ensureHttps(address); // 确保地址包含协议头
        if (fullAddress == null) { // ensureHttps可能返回null如果原始地址是null
            Log.e(TAG, "signIn: 处理后的服务器地址为空。");
            if (callbackParam != null) callbackParam.onFailure("服务器地址无效");
            return;
        }
        Log.d(TAG, "signIn: 使用地址: " + fullAddress);

        try {
            // 创建NetWorkBusiness实例，用于执行网络请求
            // 注意：NLE SDK的NetWorkBusiness构造函数可能会因URL问题抛出同步异常，所以将其置于try块内
            NetWorkBusiness nb = new NetWorkBusiness(null, fullAddress); // 第一个参数是旧的token，登录时传null
            // 调用NLE SDK的signIn方法
            nb.signIn(new SignIn(account, pwd), new NCallBack<BaseResponseEntity<User>>(c.getApplicationContext()) { // 建议使用ApplicationContext
                @Override
                protected void onResponse(BaseResponseEntity<User> response) { // 请求成功，服务器有响应
                    if (response != null && response.getStatus() == 0 && response.getResultObj() != null) {
                        User user = response.getResultObj(); // 获取User对象
                        if (user.getAccessToken() != null && !user.getAccessToken().isEmpty()) {
                            token = user.getAccessToken(); // 保存AccessToken
                            // 日志中只打印部分Token，避免敏感信息完整泄露
                            Log.i(TAG, "signIn: 登录成功。Token (前缀): " + token.substring(0, Math.min(token.length(), 10)) + "...");
                            if (callbackParam != null) callbackParam.onSuccess(user); // 调用成功回调
                        } else {
                            String error = "登录成功，但响应数据不完整 (AccessToken缺失)";
                            Log.e(TAG, "signIn: " + error);
                            if (callbackParam != null) callbackParam.onFailure(error);
                        }
                    } else {
                        // 处理登录失败或响应格式不正确的情况
                        String errorMsg = "登录失败: " + (response != null && response.getMsg() != null ? response.getMsg() : "未知错误或响应为空")
                                + (response != null ? " (Status: " + response.getStatus() + ")" : "");
                        Log.e(TAG, "signIn: " + errorMsg);
                        if (callbackParam != null) callbackParam.onFailure(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<BaseResponseEntity<User>> call, Throwable t) { // 网络请求本身失败 (如无网络连接)
                    String error = "登录请求网络失败: " + (t != null ? t.getMessage() : "未知网络错误");
                    Log.e(TAG, "signIn onFailure: " + error, t); // 打印异常堆栈
                    if (callbackParam != null) callbackParam.onFailure(error);
                }
            });
        } catch (Exception e) { // 捕获NetWorkBusiness初始化或signIn调用准备阶段的同步异常
            Log.e(TAG, "signIn: 调用登录API时发生同步异常 (例如URL格式问题)", e);
            if (callbackParam != null) callbackParam.onFailure("登录异常: " + e.getMessage());
        }
    }

    /**
     * 获取单个传感器的数据。
     * @param c Context
     * @param address 服务器地址
     * @param prjLabel 项目标识 (Project ID / Project Tag)
     * @param sensorId 传感器的API Tag
     * @param callback 获取数据的回调
     */
    public void getSensorData(final Context c, String address, String prjLabel, String sensorId, final DCallback<String> callback) {
        // 检查Token和参数
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "getSensorData: 未登录 (token为空)，无法获取传感器 " + sensorId);
            if (callback != null) callback.onFailure("用户未登录");
            return;
        }
        if (address == null || prjLabel == null || sensorId == null || address.isEmpty() || prjLabel.isEmpty() || sensorId.isEmpty()) {
            Log.e(TAG, "getSensorData: 获取传感器 " + sensorId + " 数据时参数缺失。");
            if (callback != null) callback.onFailure("获取传感器 " + sensorId + " 数据参数缺失");
            return;
        }

        String fullAddress = ensureHttps(address); // 确保地址包含协议头
        if (fullAddress == null) {
            Log.e(TAG, "getSensorData: 处理后的服务器地址为空 (" + sensorId + ").");
            if (callback != null) callback.onFailure("服务器地址无效");
            return;
        }
        Log.d(TAG, "getSensorData: 正在为传感器 " + sensorId + " 从 " + fullAddress + " (项目 " + prjLabel + ") 获取数据");

        try {
            NetWorkBusiness nb = new NetWorkBusiness(token, fullAddress); // 需要传入token
            nb.getSensor(prjLabel, sensorId, new NCallBack<BaseResponseEntity<SensorInfo>>(c.getApplicationContext()) {
                @Override
                protected void onResponse(BaseResponseEntity<SensorInfo> response) {
                    if (response != null && response.getStatus() == 0 && response.getResultObj() != null) {
                        SensorInfo sensorInfo = response.getResultObj();
                        String sensorValue = sensorInfo.getValue(); // 获取传感器数值字符串
                        if (sensorValue != null) { // 检查传感器值本身是否为null
                            Log.i(TAG, "getSensorData: 传感器 " + sensorId + " 数值: " + sensorValue);
                            if (callback != null) callback.onSuccess(sensorValue);
                        } else {
                            // 服务器成功响应，但传感器值可能就是null (例如设备离线但API仍有响应)
                            Log.w(TAG, "getSensorData: 传感器 " + sensorId + " 数据接收成功，但值为null。");
                            if (callback != null) callback.onSuccess("N/A"); // 或根据业务逻辑处理，如视为错误
                        }
                    } else {
                        String error = "获取传感器 " + sensorId + " 数据失败: "
                                + (response != null && response.getMsg() != null ? response.getMsg() : "未知响应")
                                + (response != null ? " (Status: " + response.getStatus() + ")" : "");
                        Log.e(TAG, "getSensorData: " + error);
                        if (callback != null) callback.onFailure(error);
                    }
                }

                @Override
                public void onFailure(Call<BaseResponseEntity<SensorInfo>> call, Throwable t) {
                    String error = "获取传感器 " + sensorId + " 数据请求网络失败: " + (t != null ? t.getMessage() : "未知网络错误");
                    Log.e(TAG, "getSensorData onFailure (传感器 " + sensorId + "): " + error, t);
                    if (callback != null) callback.onFailure(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getSensorData: 调用获取传感器 " + sensorId + " API时发生同步异常", e);
            if (callback != null) callback.onFailure("获取传感器 " + sensorId + " 数据异常: " + e.getMessage());
        }
    }

    /**
     * 控制设备（执行器）的开关状态。
     * @param c Context
     * @param address 服务器地址
     * @param prjLabel 项目标识
     * @param controllerId 控制器（执行器）的API Tag
     * @param state 控制状态 (例如 0 代表关, 1 代表开)
     */
    public void onOff(final Context c, String address, String prjLabel, String controllerId, int state) {
        // 检查Token和参数
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "onOff: 未登录 (token为空)，无法控制设备 " + controllerId);
            Toast.makeText(c, "请先登录", Toast.LENGTH_SHORT).show(); // Toast提示用户操作结果
            return;
        }
        if (address == null || prjLabel == null || controllerId == null || address.isEmpty() || prjLabel.isEmpty() || controllerId.isEmpty()) {
            Log.e(TAG, "onOff: 控制设备 " + controllerId + " 时参数缺失。");
            Toast.makeText(c, "控制参数缺失 (" + controllerId + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = ensureHttps(address); // 确保地址包含协议头
        if (fullAddress == null) {
            Log.e(TAG, "onOff: 处理后的服务器地址为空 (" + controllerId + ").");
            Toast.makeText(c, "服务器地址无效", Toast.LENGTH_SHORT).show();
            return;
        }
        // 打印详细的控制指令信息
        Log.d(TAG, "onOff: 控制设备 ID=" + controllerId + ", 项目=" + prjLabel + ", 地址=" + fullAddress + ", 状态=" + state + ", Token(前缀)=" + token.substring(0, Math.min(token.length(),10))+"...");

        try {
            NetWorkBusiness nb = new NetWorkBusiness(token, fullAddress);
            // 调用NLE SDK的control方法
            nb.control(prjLabel, controllerId, state, new NCallBack<BaseResponseEntity>(c.getApplicationContext()) {
                @Override
                protected void onResponse(BaseResponseEntity response) { // 此处的BaseResponseEntity不带泛型，因为控制指令通常只关心成功与否
                    if (response != null && response.getStatus() == 0) {
                        Log.i(TAG, "onOff: 控制成功。设备ID: " + controllerId + ", 状态: " + state + ", 响应消息: " + response.getMsg());
                        Toast.makeText(c, "设备 (" + controllerId + ") 操作成功", Toast.LENGTH_SHORT).show();
                    } else {
                        String error = "设备 (" + controllerId + ") 操作失败: "
                                + (response != null && response.getMsg() != null ? response.getMsg() : "未知响应")
                                + (response != null ? " (Status: " + response.getStatus() + ")" : "");
                        Log.e(TAG, "onOff: " + error);
                        Toast.makeText(c, error, Toast.LENGTH_LONG).show(); // 失败信息显示时间长一些
                    }
                }
                @Override
                public void onFailure(Call<BaseResponseEntity> call, Throwable t) {
                    String error = "设备 (" + controllerId + ") 操作请求网络失败: " + (t != null ? t.getMessage() : "未知网络错误");
                    Log.e(TAG, "onOff onFailure (设备 " + controllerId + "): " + error, t);
                    Toast.makeText(c, error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "onOff: 调用控制API (设备 " + controllerId + ") 时发生同步异常", e);
            Toast.makeText(c, "控制异常 (" + controllerId + "): " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 获取当前保存的AccessToken。
     * @return AccessToken字符串，如果未登录则为空字符串。
     */
    public String getToken() {
        return token != null ? token : "";
    }

    /**
     * 清除已保存的AccessToken (例如，在设置更改或注销时调用)。
     */
    public void clearToken() {
        this.token = "";
        Log.i(TAG, "Token已清除。");
    }
}