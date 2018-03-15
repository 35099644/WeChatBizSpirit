package com.wmx.wechatbizhook.bean;

import com.google.gson.Gson;
import com.wmx.wechatbizhook.utils.LogWriter;

/**
 * Created by wangmingxing on 18-3-15.
 */


/*
    {
        "advertisement_num":0,
        "advertisement_info":[

        ],
        "appmsgstat":{
            "show":true,
            "is_login":true,
            "liked":false,
            "read_num":3020,
            "like_num":24,
            "ret":0,
            "real_read_num":0
        },
        "comment_enabled":1,
        "reward_head_imgs":[

        ],
        "only_fans_can_comment":false,
        "base_resp":{
            "wxtoken":777
        }
    }
 */
public class MsgExtBean {
    private static final String TAG = "BizMsgExtBean";

    public int advertisement_num;
    public AppMsgStat appmsgstat;
    public int comment_enabled;
    public boolean only_fans_can_comment;

    public static MsgExtBean buildFromJson(String s) {
        Gson gson = new Gson();
        return gson.fromJson(s, MsgExtBean.class);
    }

    public void dumpInfo() {
        if (appmsgstat == null) {
            return;
        }

        LogWriter.i(TAG, "read_num=" + appmsgstat.read_num
                + ",like_num=" + appmsgstat.like_num);
    }

    public static class AppMsgStat {
        public boolean show;
        public boolean is_login;
        public boolean liked;
        public int read_num;
        public int like_num;
        public int ret;
        public int real_read_num;
    }
}
