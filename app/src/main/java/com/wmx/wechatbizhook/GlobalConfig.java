package com.wmx.wechatbizhook;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;

import com.android.volley.RequestQueue;
import com.wmx.wechatbizhook.bean.BizInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by wangmingxing on 18-3-5.
 */

public class GlobalConfig {

    public static final int BIZ_ARTICLE_START_CAPPUTRE_INTERVAL = 5000;
    public static final String WECHAT_HOST = "https://mp.weixin.qq.com";

    public static Context mAppContext;

    public static Handler mUIThreadHandler;
    public static Handler mArticleCaptureHandler;

    public static boolean mIsMainProcess, mIsWebViewProcess;

    public static final boolean sRecordLog = false;

    public static ClassLoader mAppClassLoader;

    public static RequestQueue mRequestQueue;

    public static List<BizInfo> mBizInfos = new ArrayList();
}
