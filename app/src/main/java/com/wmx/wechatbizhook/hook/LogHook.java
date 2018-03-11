package com.wmx.wechatbizhook.hook;

import com.wmx.wechatbizhook.utils.LogWriter;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by 王明兴 on 2018/3/11.
 */

public class LogHook extends BaseHook {
    private static final String TAG = "BizLogHook";

    public LogHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        findAndHookMethod("com.tencent.mm.sdk.platformtools.x",
                mLoadPackageParam.classLoader,
                "i",
                String.class, String.class, Object[].class,
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    dumpLog(param, LogWriter.PRI_INFO);
                }
            }
        );

        findAndHookMethod("com.tencent.mm.sdk.platformtools.x",
                mLoadPackageParam.classLoader,
                "w",
                String.class, String.class, Object[].class,
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    dumpLog(param, LogWriter.PRI_WARN);
                }
            }
        );

        findAndHookMethod("com.tencent.mm.sdk.platformtools.x",
                mLoadPackageParam.classLoader,
                "d",
                String.class, String.class, Object[].class,
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    dumpLog(param, LogWriter.PRI_DEBUG);
                }
            }
        );

        findAndHookMethod("com.tencent.mm.sdk.platformtools.x",
                mLoadPackageParam.classLoader,
                "e",
                String.class, String.class, Object[].class,
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    dumpLog(param, LogWriter.PRI_ERRPR);
                }
            }
        );
    }

    private void dumpLog(XC_MethodHook.MethodHookParam param, int pri) {
        String tag = TAG + param.args[0];
        String msg = (String) param.args[1];

        if (param.args.length >= 3) {
            try {
                msg = String.format(msg, (Object[]) param.args[2]);
            } catch (Exception e) {
                // ignore
            }
        }

        switch (pri) {
            case LogWriter.PRI_INFO:
                LogWriter.i(tag, msg);
                break;

            case LogWriter.PRI_DEBUG:
                LogWriter.d(tag, msg);
                break;

            case LogWriter.PRI_ERRPR:
                LogWriter.e(tag, msg);
                break;

            case LogWriter.PRI_WARN:
                LogWriter.w(tag, msg);

            default:
                break;
        }
    }
}
