package com.abc.def.googlelogin;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class AndroidBriage {

    public static String WEB_CLIENT_ID = "356292913803-rrvg04f8u59sqg2srr4dfi3ps4jrl975.apps.googleusercontent.com"; 
    private static final String TAG = "GoogleLog";
 
    // 初始化
    public static void init() {
        Log.d(TAG, "开始执行 init 方法");

        Activity mActivity = getCurrentActivity();
        if (mActivity == null) {
            Log.e(TAG, "getCurrentActivity() 返回 null，初始化失败！");
            return;
        }

        Context context = getApplication().getApplicationContext();
        if (context == null) {
            Log.e(TAG, "getApplication() 返回 null，初始化失败！");
            return;
        }

        Log.d(TAG, "当前 Activity：" + mActivity.getClass().getName());
        Log.d(TAG, "开始初始化 GoogleCredentialManagerSign");

        GoogleCredentialManagerSign.init(mActivity, context);

        Log.d(TAG, "init 方法执行完成");
    }

    // 登录
    public static void googleLogin() {
        Log.d(TAG, "执行 googleLogin()");
        GoogleCredentialManagerSign.googleLogin();
    }

    // 登出
    public static void googleLoginOut() {
        Log.d(TAG, "执行 googleLoginOut()");
        GoogleCredentialManagerSign.googleLoginOut();
    }

    // 向 Unity 发送登录结果
    public static void SendInfo(String result) {
        Log.d(TAG, "SendInfo 调用，发送给 Unity 的内容是：" + result);
        UnityPlayer.UnitySendMessage("sdkobj", "OnGoogleLoginMessage", result);
    }

    public static Activity getCurrentActivity() {
        Log.d(TAG, "调用 getCurrentActivity()");
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<?, ?> activities = (Map<?, ?>) activitiesField.get(activityThread);

            for (Object activityRecord : activities.values()) {
                Class<?> activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    Log.d(TAG, "获取到当前未暂停的 Activity：" + activity.getClass().getName());
                    return activity;
                }
            }

            Log.e(TAG, "未找到未暂停的 Activity");
        } catch (Exception e) {
            Log.e(TAG, "getCurrentActivity() 异常", e);
        }
        return null;
    }

    public static Application getApplication() {
        Log.d(TAG, "调用 getApplication()");
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            Application application = (Application) getApplicationMethod.invoke(activityThread);
            Log.d(TAG, "成功获取 Application：" + application.getPackageName());
            return application;
        } catch (Exception e) {
            Log.e(TAG, "getApplication() 异常", e);
        }
        return null;
    }
}
