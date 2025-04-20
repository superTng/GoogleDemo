package com.abc.def.googlelogin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

//谷歌凭据管理器登录
public class GoogleCredentialManagerSign {
    private static Activity mActivity = null;
    private static Context mContext = null;
    private static CredentialManager credentialManager;
    private static boolean oneTapStatus = false;

    public static void init(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
    }

    public static void googleLogin() {
        if (mContext == null || mActivity == null) {
            Log.e("GoogleLog", "Context or Activity is null. Please call init() first.");
            return;
        }

        credentialManager = CredentialManager.Companion.create(mContext);

        Log.d("GoogleLog", "开始Google登录请求");


        GetSignInWithGoogleOption googleIdOption = new GetSignInWithGoogleOption.Builder(AndroidBriage.WEB_CLIENT_ID).build();

        //实例化请求
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        android.os.CancellationSignal cancellationSignal = new android.os.CancellationSignal();
        cancellationSignal.setOnCancelListener(() -> {
            if (oneTapStatus) oneTapStatus = false;
            Log.e("GoogleLog", "准备凭据时操作被取消");
        });

        credentialManager.getCredentialAsync(
                mActivity,
                request,
                cancellationSignal,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        handleError(e);
                    }
                });
    }

    private static void handleSignIn(GetCredentialResponse result) {

        Credential credential = result.getCredential();

        Log.d("GoogleLog", "Google登录成功，凭据类型: " + credential.getClass().getSimpleName());

        if (credential instanceof PublicKeyCredential) {
            String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
            Log.d("GoogleLog", "PublicKeyCredential返回的认证响应: " + responseJson);

        } else if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            Log.d("GoogleLog", "用户名: " + username + " 密码: " + password);

        } else if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                String idToken = googleIdTokenCredential.getIdToken();

                Log.d("GoogleLog", "ID Token: " + idToken);

                try {
                    JSONObject googleLoginInfoReturn = new JSONObject();
                    googleLoginInfoReturn.put("userId", googleIdTokenCredential.getId());
                    googleLoginInfoReturn.put("displayName", googleIdTokenCredential.getDisplayName());
                    googleLoginInfoReturn.put("imageUrl", googleIdTokenCredential.getProfilePictureUri());
                    googleLoginInfoReturn.put("givenName", googleIdTokenCredential.getGivenName());
                    googleLoginInfoReturn.put("familyName", googleIdTokenCredential.getFamilyName());
                    googleLoginInfoReturn.put("phoneNumber", googleIdTokenCredential.getPhoneNumber());
                    googleLoginInfoReturn.put("IdToken", idToken);

  
                    AndroidBriage.SendInfo(googleLoginInfoReturn.toString());

                } catch (JSONException e) {
                    Log.e("GoogleLog", "处理Google登录返回信息失败: " + e.getMessage());
                }
            } else {
                Log.e("GoogleLog", "无法识别的凭据类型: " + credential.getType());
            }
        } else {
            Log.e("GoogleLog", "无法识别的凭据类型");
        }
    }

    @SuppressLint("RestrictedApi")
    private static void handleError(@NonNull Exception e) {
        String errorMessage = "Google登录失败";
        String status = "error";
    

        Log.e("GoogleLog", "异常类型 = " + e.getClass().getSimpleName());
        Log.e("GoogleLog", "异常信息：" + e.getMessage());
        
        if (e.getMessage() != null && e.getMessage().contains("Account reauth failed")) {
            errorMessage = "Google登录失败: 账户重新验证失败。请确保您已登录并授权。";
        } else {
            errorMessage = "Google登录失败: 未知错误";
        }
    

        JSONObject errorJson = new JSONObject();
        try {
            errorJson.put("status", status);
            errorJson.put("msg", errorMessage);
        } catch (JSONException jsonException) {
            Log.e("GoogleLog", "构建错误信息 JSON 失败: " + jsonException.getMessage());
        }
    

        AndroidBriage.SendInfo(errorJson.toString());
    

        Logger logger = Logger.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
        logger.log(Level.SEVERE, "Error during Google login process: " + e);
        e.printStackTrace();  
    }
    
    


    public static void googleLoginOut() {
        ClearCredentialStateRequest clearCredentialStateRequest = new ClearCredentialStateRequest();
        android.os.CancellationSignal cancellationSignal = new android.os.CancellationSignal();
        cancellationSignal.setOnCancelListener(() -> {
            if (oneTapStatus) oneTapStatus = false;
            Log.e("GoogleLog", "准备凭据时操作被取消");
        });

        if (credentialManager != null) {
            credentialManager.clearCredentialStateAsync(
                    clearCredentialStateRequest,
                    cancellationSignal,
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(Void unused) {
                            Log.d("GoogleLog", "Google注销登录成功");
                            Toast.makeText(mContext, "注销成功", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ClearCredentialException e) {
                            Log.e("GoogleLog", "注销出错: " + e.getMessage());
                        }
                    }
            );
        }
    }
}
