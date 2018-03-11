package com.wmx.wechatbizhook;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;

/**
 * Created by wangmingxing on 18-3-5.
 */

public class GlobalConfig {
    public static Context mAppContext;

    public static Handler mUIThreadHandler;

    public static boolean mIsMainProcess, mIsWebViewProcess;

    public static final boolean sRecordLog = false;

    public static ClassLoader mAppClassLoader;

}
