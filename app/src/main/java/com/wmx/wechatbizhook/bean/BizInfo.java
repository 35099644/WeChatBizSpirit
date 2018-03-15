package com.wmx.wechatbizhook.bean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wmx.wechatbizhook.utils.LogWriter;

import net.sqlcipher.Cursor;

/**
 * Created by wangmingxing on 18-3-8.
 */

public class BizInfo {
    private static final String TAG = "BizInfo";

    public String username;
    public String appId;
    public String extInfo;
    public String brandInfo;
    public String nickname;
    public BrandInfo.Info[] urls;

    public void dump() {
        LogWriter.i(TAG, "username:" + username);
        LogWriter.i(TAG, "nickname:" + nickname);
        LogWriter.i(TAG, "appId:" + appId);
        LogWriter.i(TAG, "extInfo:" + extInfo);
        LogWriter.i(TAG, "brandInfo:" + brandInfo);
    }

    public static BizInfo buildBizInfo(Cursor cursor) {
        BizInfo bizInfo = new BizInfo();
        bizInfo.username = cursor.getString(cursor.getColumnIndex("username"));
        bizInfo.appId = cursor.getString(cursor.getColumnIndex("appId"));
        bizInfo.extInfo = cursor.getString(cursor.getColumnIndex("extInfo"));
        bizInfo.brandInfo = cursor.getString(cursor.getColumnIndex("brandInfo"));
        bizInfo.nickname = cursor.getString(cursor.getColumnIndex("nickname"));

        try {
            Gson gson = new Gson();
            BrandInfo info = gson.fromJson(bizInfo.brandInfo, BrandInfo.class);
            bizInfo.urls = info.urls;
        } catch (Exception e) {
            LogWriter.e(TAG, e);
            bizInfo = null;
        }
        return bizInfo;
    }
}
