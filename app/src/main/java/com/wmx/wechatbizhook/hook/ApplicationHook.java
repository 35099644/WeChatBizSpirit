package com.wmx.wechatbizhook.hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.android.volley.toolbox.Volley;
import com.wmx.wechatbizhook.GlobalConfig;
import com.wmx.wechatbizhook.bean.BizInfo;
import com.wmx.wechatbizhook.utils.LogWriter;
import com.wmx.wechatbizhook.utils.WeChatUtil;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by 王明兴 on 2018/3/10.
 */

public class ApplicationHook extends BaseHook {
    private static final String TAG = "BizApplicationHook";
    private static final int BIZ_HOME_LUNCH_INTERVAL = 5000;

    public ApplicationHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        findAndHookMethod("android.app.Application",
                mLoadPackageParam.classLoader,
                "onCreate",
                new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                GlobalConfig.mUIThreadHandler = new Handler();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                LogWriter.i(TAG, "After Application.onCreate");
                setDefaultCrashHandler();
                GlobalConfig.mAppContext = (Context) param.thisObject;
                GlobalConfig.mRequestQueue = Volley.newRequestQueue(GlobalConfig.mAppContext);
                GlobalConfig.mRequestQueue.start();

                HandlerThread thread = new HandlerThread(
                        "biz_article_capture_thread", Process.THREAD_PRIORITY_BACKGROUND);
                thread.start();
                GlobalConfig.mArticleCaptureHandler = new Handler(thread.getLooper());
                dumpBizTable();
            }
        });
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
                            "SELECT bizinfo.username, rcontact.nickname, extinfo, appId, brandInfo " +
                            "FROM bizinfo JOIN rcontact USING (username) where username != 'weixin'" , null);
                    while (cursor.moveToNext()) {
                        BizInfo bizInfo = BizInfo.buildBizInfo(cursor);
                        if (bizInfo != null) {
                            //bizInfo.dump();
                            GlobalConfig.mBizInfos.add(bizInfo);
                        }
                    }

                    cursor.close();
                    db.close();

                    // 先从主进程调起第一个公众号的首页，然后在tools进程抓取数据
                    if (GlobalConfig.mIsMainProcess) {
                        startCapturBizArticle();
                    }
                }
            }
        }).start();
    }

    private void startCapturBizArticle() {
        GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (GlobalConfig.mBizInfos.size() == 0) {
                    return;
                }

                String url = GlobalConfig.mBizInfos.get(0).urls[0].url;
                LogWriter.i(TAG, "startWebViewUI url=" + url);
                WeChatUtil.startWebViewUI(GlobalConfig.mAppContext, url);
            }
        }, BIZ_HOME_LUNCH_INTERVAL);
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

    private void dumpCookie() {
        String cookie = WeChatUtil.getCookie();
        LogWriter.i(TAG, "cookie:" + cookie);
    }
}
