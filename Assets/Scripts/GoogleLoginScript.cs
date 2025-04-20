using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class GoogleLoginScript : MonoBehaviour
{
    public TMP_Text statusText;




    private static AndroidJavaObject googleSignObj;
    void Awake()
    {
       
        GoogleSignInit();
    }


    //调用登录接口
    public void OnSignIn()
    {
        googleSignObj.CallStatic("googleLogin");
        AddStatusText( DateTime.Now +" start login...");
    }

    //登录回调
    public void OnGoogleLoginMessage(string message)
    {
        Debug.Log("message: " + message);
        //开始登录逻辑
        AddStatusText("Callback:" + message);
    }

    public static AndroidJavaObject GoogleSignObj()
    {
        if (Application.platform == RuntimePlatform.Android)
        {
            if (googleSignObj == null)
            {
                googleSignObj = new AndroidJavaObject("com.abc.def.googlelogin.AndroidBriage");
                if (googleSignObj == null)
                {
                    Debug.LogError("AndroidBriage init faild");
                }
            }
            return googleSignObj;
        }
        else
        {
            return null;
        }
    }

    public static void GoogleSignInit()
    {
        try
        {
            Debug.Log("开始初始化");
            googleSignObj = GoogleSignObj();
            if (googleSignObj != null)
            {
                googleSignObj.CallStatic("init");
            }
        }
        catch (Exception e)
        {
            Debug.LogError("Failed:" + e.Message);
        }
    }

    private List<string> messages = new List<string>();
    void AddStatusText(string text)
    {
        if (messages.Count == 5)
        {
            messages.RemoveAt(0);
        }
        messages.Add(text);
        string txt = "";
        foreach (string s in messages)
        {
            txt +="\n" + s;
        }
        statusText.text = txt;
    }
}

