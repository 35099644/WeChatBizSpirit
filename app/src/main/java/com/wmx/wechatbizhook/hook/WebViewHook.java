package com.wmx.wechatbizhook.hook;

import com.wmx.wechatbizhook.utils.LogWriter;

import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by 王明兴 on 2018/3/11.
 */

public class WebViewHook extends BaseHook {
    private static final String TAG = "BizWebViewHook";
    private Map<String, String> mHttpHeader;

    public WebViewHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        try {
            final Class wcWebViewClass = mLoadPackageParam.classLoader.loadClass("com.tencent.smtt.sdk.WebView");

            XposedBridge.hookAllConstructors(wcWebViewClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(wcWebViewClass, "setWebContentsDebuggingEnabled", true);
                    LogWriter.i(TAG, "x5 setWebContentsDebuggingEnabled");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.callStaticMethod(wcWebViewClass, "setWebContentsDebuggingEnabled", true);
                }
            });

            XposedBridge.hookAllMethods(wcWebViewClass, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                    LogWriter.i(TAG, "x5 setWebContentsDebuggingEnabled");
                }
            });

            findAndHookMethod(wcWebViewClass, "loadUrl", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    LogWriter.i(TAG, "x5 loadUrl url=" + param.args[0]);
                }
            });

            findAndHookMethod(wcWebViewClass, "loadUrl", String.class, Map.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    LogWriter.i(TAG, "x5 loadUrl url=" + param.args[0] + ",headers=" + param.args[1]);
                }
            });

            final Class webViewClientClass =
                    mLoadPackageParam.classLoader.loadClass("com.tencent.smtt.sdk.WebViewClient");
            findAndHookMethod(wcWebViewClass, "setWebViewClient",
                    webViewClientClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    LogWriter.i(TAG, "setWebViewClient " + param.args[0]);
                }
            });
        } catch (Exception e) {
            LogWriter.e(TAG, e);
        }
    }
}
