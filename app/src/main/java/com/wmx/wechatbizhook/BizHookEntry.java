package com.wmx.wechatbizhook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Process;
import android.webkit.CookieManager;

import com.wmx.wechatbizhook.Bean.BizInfo;
import com.wmx.wechatbizhook.utils.LogWriter;
import com.wmx.wechatbizhook.utils.WeChatUtil;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by wangmingxing on 18-3-5.
 */

public class BizHookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "BizHookEntry";
    private List<BizInfo> mBizInfos = new ArrayList();
    private Handler mUIThreadHandler;
    private boolean mIsMainProcess, mIsWebViewProcess;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.tencent.mm".equals(lpparam.packageName)) {
            return;
        }

        mIsMainProcess = WeChatUtil.isMainProcess(lpparam.processName);
        mIsWebViewProcess = WeChatUtil.isWebViewProcess(lpparam.processName);
        if (!mIsMainProcess && !mIsWebViewProcess) {
            return;
        }

        LogWriter.i(TAG, "Loaded app:" + lpparam.processName);

        findAndHookMethod("android.app.Application",
                lpparam.classLoader,
                "onCreate",
                new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mUIThreadHandler = new Handler();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                LogWriter.i(TAG, "After Application.onCreate");
                setDefaultCrashHandler();
                GlobalConfig.mAppContext = (Context) param.thisObject;
                if (mIsMainProcess) {
                    dumpBizTable();
                }
            }
        });

        findAndHookMethod("android.app.Activity",
                lpparam.classLoader,
                "onResume",
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                LogWriter.i(TAG, "onResume:" + param.thisObject);
                if (param.thisObject.getClass().getSimpleName().equals("LauncherUI")) {
                    return;
                }

                if (param.thisObject.getClass().getSimpleName().equals("WebViewUI")) {
                    mUIThreadHandler.postDelayed(new Runnable() {
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

    private void dumpBizTable() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                preloadNativeLibrary();
                File dbFile = copyDatabase(WeChatUtil.getContactDbFile());
                if (dbFile != null) {
                    LogWriter.i(TAG, "Found contact database file " + dbFile.getPath());
                    String passwd = WeChatUtil.getDbPasswd();
                    LogWriter.i(TAG, "Database passwd:" + passwd);

                    SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
                        public void preKey(SQLiteDatabase database) {

                        }

                        public void postKey(SQLiteDatabase database) {
                            database.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库
                        }
                    };

                    SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, passwd, null, hook);
                    Cursor cursor = db.rawQuery(
                            "SELECT openid, bizinfo.username, rcontact.nickname, extinfo, appId, brandInfo " +
                                "FROM BizKF " +
                                "JOIN rcontact ON BizKF.brandUsername = rcontact.username " +
                                "JOIN bizinfo ON BizKF.brandUsername = bizinfo.username", null);
                    while (cursor.moveToNext()) {
                        BizInfo bizInfo = BizInfo.buildBizInfo(cursor);
                        bizInfo.dump();
                        mBizInfos.add(bizInfo);
                        mUIThreadHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String url = mBizInfos.get(0).urls[0].url;
                                LogWriter.i(TAG, "startWebViewUI url=" + url);
                                WeChatUtil.startWebViewUI(GlobalConfig.mAppContext, url);
                            }
                        }, 5000);
                    }
                    cursor.close();
                    db.close();
                }
            }
        }).start();
    }

    private void setDefaultCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                LogWriter.e(TAG, "uncaught Exception:", throwable);
            }
        });
    }

    private void preloadNativeLibrary() {
        PackageManager pm = GlobalConfig.mAppContext.getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo("com.wmx.wechatbizhook", 0);
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            File soDir = new File(appInfo.nativeLibraryDir);
            File[] libs = soDir.listFiles();
            for (File so : libs) {
                System.load(so.getPath());
            }
        } catch (Exception e) {
            LogWriter.e(TAG, "preloadNativeLibrary error", e);
        }
    }

    private File copyDatabase(File srcDb) {
        ApplicationInfo appInfo = GlobalConfig.mAppContext.getApplicationInfo();
        File dir = new File(appInfo.dataDir, "MicroMsgHook");
        if (!dir.exists()) {
            dir.mkdir();
        }

        File destDb = new File(dir, "EnMicroMsg.db");
        WeChatUtil.copyFile(srcDb, destDb);
        return destDb;
    }
}
