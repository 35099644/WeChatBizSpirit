package com.wmx.wechatbizhook.hook;

import android.net.Uri;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.wmx.wechatbizhook.GlobalConfig;
import com.wmx.wechatbizhook.bean.MsgBean;
import com.wmx.wechatbizhook.bean.MsgExtBean;
import com.wmx.wechatbizhook.utils.LogWriter;
import com.wmx.wechatbizhook.utils.RefUtil;
import com.wmx.wechatbizhook.utils.WeChatUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by wangmingxing on 18-3-14.
 */

public class WebViewClientHook extends BaseHook {
    private static final String TAG = "BizWebViewClientHook";
    private static final String Q_UA2 = "Q-UA2: QV=3&PL=ADR&PR=WX&PP=com.tencent.mm&PPVN=6.6.3&TBSVC=43603&CO=BK&COVC=043909&PB=GE&VE=GA&DE=PHONE&CHID=0&LCID=9422&MO= PixelXL &RL=1440*2392&OS=7.1.2&API=25";
    private static final String Q_GUID ="b08a5edb5e2a655000ca6e7e13b788cb";
    private static final String UA = "Mozilla/5.0 (Linux; Android 7.1.2; Pixel XL Build/NZH54D; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.132 MQQBrowser/6.2 TBS/043909 Mobile Safari/537.36 MicroMessenger/6.6.3.1260(0x26060336) NetType/WIFI Language/zh_CN";
    private static final String Q_AUTH ="31045b957cf33acf31e40be2f3e71c5217597676a9729f1b";

    // 只请求最近30天的文章
    private static final int BIZ_REQ_INTERVAL = 30 * 24 * 60 * 60;

    // 当前正在抓取的公众号索引
    private int mCurrentBizIndex = 0;

    // 已经请求到的文章列中正在抓取文章的索引
    private int mCurrentArticleIndex = 0;

    // 当前公众号下一次要请求文章列表的偏移
    private int mCurrentBizReqOffset = 0;

    // 当前公众号获取文章列表的URL
    private String mBizArtileListUrl;

    // 当前公众号文章列表
    private ArrayList<MsgBean.MsgExtInfo> mArticleList;

    private Random mDelayRandom = new Random(0x12398756);

    public WebViewClientHook(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public void hook() {
        try {
            final Class wcWebViewClass =
                    mLoadPackageParam.classLoader.loadClass("com.tencent.smtt.sdk.WebView");
            final Class webViewClientClass =
                    mLoadPackageParam.classLoader.loadClass("com.tencent.xweb.x5.j$2");
            final Class webResReqCls = mLoadPackageParam.classLoader.loadClass(
                    "com.tencent.smtt.export.external.interfaces.WebResourceRequest");

            findAndHookMethod(webViewClientClass,
                    "shouldInterceptRequest",
                    wcWebViewClass, webResReqCls, Bundle.class,
                    new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object webResReq = param.args[1];
                    final Map<String, String> headers = (Map) RefUtil.callDeclaredMethod(
                            webResReq, "getRequestHeaders", new Class[]{});
                    final String referer = headers.get("Referer");

                    Uri uri = (Uri) RefUtil.callDeclaredMethod(webResReq, "getUrl", new Class[]{});
                    String url = uri.toString();
                    final String pass_ticket = uri.getQueryParameter("pass_ticket");

//                    LogWriter.i(TAG, "shouldInterceptRequest1, header=" + headers
//                            + ",url=" + url + ",extra=" + param.args[2].toString());

                    if ("urlcheck".equals(uri.getQueryParameter("action"))) {
                        String uin = uri.getQueryParameter("uin");
                        String key = uri.getQueryParameter("key");
                        String appmsg_token = uri.getQueryParameter("appmsg_token");
                        LogWriter.i(TAG, "Extract url parameter"
                                + ",uin=" + uin
                                + ",key=" + key
                                + ",pass_ticket=" + pass_ticket
                                + ",appmsg_token=" + appmsg_token);

                        mBizArtileListUrl = referer.replace("action=home", "action=getmsg");
                        StringBuilder sb = new StringBuilder();
                        sb.append(mBizArtileListUrl)
                                .append("&f=json&is_ok=1")
                                .append("&uin=").append(uin)
                                .append("&key=").append(key)
                                .append("&appmsg_token=").append(appmsg_token)
                                .append("&x5=1");
                        mBizArtileListUrl = sb.toString();
                        mCurrentBizReqOffset = 0;
                        mCurrentArticleIndex = 0;
                        requestBizArticleList(mCurrentBizReqOffset, 10, referer);
                    } else if ("/mp/getappmsgext".equals(uri.getPath())) {
                        requestNextArticleExtInfo(url, pass_ticket, referer);
                    }
                }
            });
        } catch (Exception e) {
            LogWriter.e(TAG, e);
        }
    }

    private int getRandomDelay() {
        int delay = mDelayRandom.nextInt();
        delay = delay % 10000;
        if (delay < 2000) {
            delay = 2000;
        }
        return delay;
    }

    /**
     * 获取文章的阅读数，点赞数
     */
    private void requestNextArticleExtInfo(final String extInfourl, final String passTicket, final String referer) {
        StringRequest request = new StringRequest(Request.Method.POST, extInfourl,
            new Response.Listener<String>() {
                @Override
                public void onResponse(final String s) {
                    LogWriter.i(TAG, "appmsgext:" + s);
                    GlobalConfig.mArticleCaptureHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MsgExtBean msgExt = MsgExtBean.buildFromJson(s);
                            msgExt.dumpInfo();

                            // 请求失败，重新请求
                            if (msgExt.appmsgstat == null) {
                                LogWriter.e(TAG, "request article extinfo error!");
                                GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        requestNextArticleExtInfo(extInfourl, passTicket, referer);
                                    }
                                }, 5000);
                                return;
                            }

                            if (msgExt.appmsgstat != null && mArticleList != null) {
                                MsgBean.MsgExtInfo currArticle = mArticleList.get(mCurrentArticleIndex);
                                currArticle.read_num = msgExt.appmsgstat.read_num;
                                currArticle.like_num = msgExt.appmsgstat.like_num;

                                if (System.currentTimeMillis() / 1000 - currArticle.datetime > BIZ_REQ_INTERVAL) {
                                    mCurrentBizIndex++;
                                    mCurrentBizReqOffset = 0;
                                    requestNextBiz();
                                    return;
                                }
                            }

                            // 获取完信息请求下一篇文章的信息
                            mCurrentArticleIndex++;
                            GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    requestNextArticleInfo(mArticleList, referer);
                                }
                            }, getRandomDelay());
                        }
                    });
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    LogWriter.e(TAG, "getappmsgext error " + volleyError);
                    GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            requestNextArticleExtInfo(extInfourl, passTicket, referer);
                        }
                    }, 5000);
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return makeHttpHeader(referer);
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                StringBuilder sb = new StringBuilder();
                sb.append("is_only_read=1")
                        .append("&pass_ticket=").append(passTicket)
                        .append("&is_temp_url=0");
                return sb.toString().getBytes();
            }
        };
        GlobalConfig.mRequestQueue.add(request);
    }

    /**
     * 请求文章的信息
     */
    private void requestNextArticleInfo(final ArrayList<MsgBean.MsgExtInfo> articleList, final String referer) {
        if (articleList == null || articleList.size() == 0) {
            return;
        }

        // 已请求到的列表数据信息获取完毕，请求下一个列表
        LogWriter.i(TAG, "requestNextArticleInfo, index=" + mCurrentArticleIndex + ",totol=" + articleList.size());
        if (mCurrentArticleIndex >= articleList.size()) {
            GlobalConfig.mArticleCaptureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(articleList);
                    LogWriter.i(TAG, "Article list info:" + jsonStr);
                    requestBizArticleList(mCurrentBizReqOffset, 10, referer);
                }
            }, 0);
            return;
        }

        try {
            final String url = articleList.get(mCurrentArticleIndex).content_url;
            WeChatUtil.startWebViewUI(
                    GlobalConfig.mAppContext, url);

            // 30s后还未结束，重新请求
            final int articleIndex = mCurrentArticleIndex;
            GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mCurrentArticleIndex >= articleList.size()) {
                        return;
                    }

                    if (mCurrentArticleIndex != articleIndex) {
                        return;
                    }

                    MsgBean.MsgExtInfo info = articleList.get(mCurrentArticleIndex);
                    String currUrl = info.content_url;
                    if (url.equals(currUrl)) {
                        if (info.like_num != 0 || info.read_num != 0) {
                            return;
                        }
                        LogWriter.i(TAG, "lunch biz article timeout, restart");
                        requestNextArticleInfo(articleList, referer);
                    }
                }
            }, 30000);
        } catch (Exception e) {
            LogWriter.e(TAG, e);
        }
    }

    /**
     * 请求公众号文章列表
     * @param offset 偏移
     * @param count 请求数目
     * @param referer
     */
    private void requestBizArticleList(int offset, final int count, final String referer) {
        LogWriter.i(TAG, "requestBizArticleList offset=" + offset + ",count=" + count);
        String url = mBizArtileListUrl + "&offset=" + offset + "&count=" + count;
        StringRequest sr = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(final String s) {
                        LogWriter.i(TAG, "Article list response:" + s);
                        LogWriter.i(TAG, "CurrentBizIndex=" + mCurrentBizIndex);
                        GlobalConfig.mArticleCaptureHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                MsgBean msg = MsgBean.buildFromJson(s);
                                msg.dumpInfo();
                                mCurrentArticleIndex = 0;
                                boolean done = false;

                                if ("ok".equals(msg.errmsg)) {
                                    if (msg.can_msg_continue == 0) {
                                        done = true;
                                        // 偏移加一个大数字，下次请求列表的时候返回列表数目为0,触发下一个公众号请求
                                        mCurrentBizReqOffset += 100;
                                    } else {
                                        mCurrentBizReqOffset = msg.next_offset;
                                    }
                                }

                                mArticleList = msg.getArticleList();
                                // 列表请求失败，重新调起公众号历史页
                                if (mArticleList == null || mArticleList.size() == 0) {
                                    LogWriter.i(TAG, "no article");
                                    if (done) {
                                        mCurrentBizIndex++;
                                        mCurrentBizReqOffset = 0;
                                    }

                                    // 调起下一个公众号首页
                                    requestNextBiz();
                                    return;
                                }

                                LogWriter.i(TAG, "Totol articles:" + mArticleList.size());
                                // 获取到列表以后，逐个用webview打开来获取阅读数和点赞数
                                requestNextArticleInfo(mArticleList, referer);
                            }
                        });
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        LogWriter.e(TAG, "error:" + volleyError);
                        requestBizArticleList(mCurrentBizReqOffset, count, referer);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return makeHttpHeader(referer);
            }
        };
        GlobalConfig.mRequestQueue.add(sr);
    }

    /**
     * 开始下一个公众号请求
     */
    private void requestNextBiz() {
        if (mCurrentBizIndex >= GlobalConfig.mBizInfos.size()) {
            LogWriter.i(TAG, "request all biz articles done!");
            mCurrentBizIndex = 0;
            return;
        }

        LogWriter.i(TAG, "request next Biz, index=" + mCurrentBizIndex);
        String url = GlobalConfig.mBizInfos.get(mCurrentBizIndex).urls[0].url;
        WeChatUtil.startWebViewUI(GlobalConfig.mAppContext, url);

        // 30s后还未打开重新打开
        final int bizIndex = mCurrentBizIndex;
        GlobalConfig.mUIThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bizIndex != mCurrentBizIndex) {
                    return;
                }

                if (mCurrentBizReqOffset != 0) {
                    return;
                }
                LogWriter.i(TAG, "lunch biz home timeout, restart");
                requestNextBiz();
            }
        }, 30000);
    }

    private Map<String, String> makeHttpHeader(String referer) {
        Map<String, String> header = new HashMap<>();
        header.put("Cookie", WeChatUtil.getCookie());
        header.put("Q-UA2", Q_UA2);
        header.put("Q-GUID", Q_GUID);
        header.put("Q-Auth", Q_AUTH);
        header.put("Referer", referer);
        header.put("User-Agent", UA);
        header.put("X-Requested-With", "XMLHttpRequest");
        return header;
    }
}
