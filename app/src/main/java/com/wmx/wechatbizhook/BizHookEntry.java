package com.wmx.wechatbizhook;

import com.wmx.wechatbizhook.hook.ActivityHook;
import com.wmx.wechatbizhook.hook.ApplicationHook;
import com.wmx.wechatbizhook.hook.LogHook;
import com.wmx.wechatbizhook.hook.WebViewClientHook;
import com.wmx.wechatbizhook.hook.WebViewHook;
import com.wmx.wechatbizhook.utils.LogWriter;
import com.wmx.wechatbizhook.utils.WeChatUtil;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by wangmingxing on 18-3-5.
 */

public class BizHookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "BizHookEntry";


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.tencent.mm".equals(lpparam.packageName)) {
            return;
        }

        GlobalConfig.mAppClassLoader = lpparam.classLoader;
        GlobalConfig.mIsMainProcess = WeChatUtil.isMainProcess(lpparam.processName);
        GlobalConfig.mIsWebViewProcess = WeChatUtil.isWebViewProcess(lpparam.processName);
        if (!GlobalConfig.mIsMainProcess && !GlobalConfig.mIsWebViewProcess) {
            return;
        }

        LogWriter.i(TAG, "Loaded app:" + lpparam.processName);
        new ApplicationHook(lpparam).hook();
        new ActivityHook(lpparam).hook();
        //new LogHook(lpparam).hook();
        //new WebViewHook(lpparam).hook();
        new WebViewClientHook(lpparam).hook();
    }
}
