package com.wmx.wechatbizhook.hook;

import android.webkit.CookieManager;

import com.wmx.wechatbizhook.GlobalConfig;
import com.wmx.wechatbizhook.utils.LogWriter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by 王明兴 on 2018/3/11.
 */

public class ActivityHook extends BaseHook {
    private static final String TAG = "BizActivityHook";

    public ActivityHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        findAndHookMethod("android.app.Activity",
                mLoadPackageParam.classLoader,
                "onResume",
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                LogWriter.i(TAG, "onResume:" + param.thisObject);
                if (param.thisObject.getClass().getSimpleName().equals("LauncherUI")) {
                    return;
                }

                if (param.thisObject.getClass().getSimpleName().equals("WebViewUI")) {
                    GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dumpCookie();
                        }
                    }, 5000);
                }
            }
        });
    }

    private void dumpCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie("https://mp.weixin.qq.com");
        LogWriter.i(TAG, "cookie:" + cookie);
    }
}
