package com.wmx.wechatbizhook.hook;

import android.webkit.WebView;

import com.wmx.wechatbizhook.utils.LogWriter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by 王明兴 on 2018/3/11.
 */

public class WebViewHook extends BaseHook {
    private static final String TAG = "BizWebViewHook";

    public WebViewHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        XposedBridge.hookAllConstructors(WebView.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.callStaticMethod(WebView.class, "setWebContentsDebuggingEnabled", true);
                LogWriter.i(TAG, "setWebContentsDebuggingEnabled");
            }
        });

        XposedBridge.hookAllMethods(WebView.class, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = true;
                LogWriter.i(TAG, "setWebContentsDebuggingEnabled");
            }
        });

        try {
            Class wcWebViewClass = mLoadPackageParam.classLoader.loadClass("com.tencent.smtt.sdk.WebView");

            XposedBridge.hookAllConstructors(wcWebViewClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(WebView.class, "setWebContentsDebuggingEnabled", true);
                    LogWriter.i(TAG, "x5 setWebContentsDebuggingEnabled");
                }
            });

            XposedBridge.hookAllMethods(wcWebViewClass, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                    LogWriter.i(TAG, "x5 setWebContentsDebuggingEnabled");
                }
            });
        } catch (Exception e) {
            LogWriter.e(TAG, e);
        }
    }
}
