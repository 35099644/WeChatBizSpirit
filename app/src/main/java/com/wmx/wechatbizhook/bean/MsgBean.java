package com.wmx.wechatbizhook.bean;

import com.google.gson.Gson;
import com.wmx.wechatbizhook.utils.LogWriter;

import java.util.ArrayList;

/**
 * Created by wangmingxing on 18-3-14.
 */

public class MsgBean {
    public static final String TAG = "BizMsgBean";

    public int can_msg_continue;
    public String errmsg;
    public int msg_count;
    public int next_offset;
    public int real_type;
    public int ret;
    public int use_video_tab;
    public int video_count;
    public String general_msg_list;

    private MsgList mMsgList;
    private ArrayList<MsgExtInfo> mArticleList;

    public static MsgBean buildFromJson(String s) {
        Gson gson = new Gson();
        return gson.fromJson(s, MsgBean.class);
    }

    public MsgList getMsgList() {
        if (mMsgList != null) {
            return mMsgList;
        }

        Gson gson = new Gson();
        mMsgList = gson.fromJson(general_msg_list, MsgList.class);
        formatMsg(mMsgList);
        return mMsgList;
    }

    public ArrayList<MsgExtInfo> getArticleList() {
        if (mArticleList != null) {
            return mArticleList;
        }

        MsgList msgList = getMsgList();
        if (msgList == null || msgList.list == null || msgList.list.length == 0) {
            return null;
        }

        ArrayList<MsgExtInfo> articleList = new ArrayList<>(100);
        for (Msg msg : msgList.list) {
            addArticle(articleList, msg.app_msg_ext_info);
        }

        mArticleList = articleList;
        return articleList;
    }

    public void dumpInfo() {
        if (general_msg_list == null) {
            return;
        }

        MsgList msgList = getMsgList();
        if (msgList == null) {
            return;
        }

        if (msgList.list == null || msgList.list.length == 0) {
            return;
        }

        for (Msg msg : msgList.list) {
            dumpMsgInfo(msg.app_msg_ext_info);
        }
    }

    private void addArticle(ArrayList<MsgExtInfo> list, MsgExtInfo msgExtInfo) {
        if (msgExtInfo == null) {
            return;
        }

        list.add(msgExtInfo);
        if (msgExtInfo.multi_app_msg_item_list == null
                || msgExtInfo.multi_app_msg_item_list.length == 0) {
            return;
        }

        for (MsgExtInfo extInfo : msgExtInfo.multi_app_msg_item_list) {
            addArticle(list, extInfo);
        }
        msgExtInfo.multi_app_msg_item_list = null;
    }

    private void dumpMsgInfo(MsgExtInfo extInfo) {
        if (extInfo == null) {
            return;
        }

        LogWriter.i(TAG, "datetime:" + extInfo.datetime);
        LogWriter.i(TAG, "title:" + extInfo.title);
        LogWriter.i(TAG, "content_url:" + extInfo.content_url);

        if (extInfo.multi_app_msg_item_list == null || extInfo.multi_app_msg_item_list.length == 0) {
            return;
        }

        for (MsgExtInfo info : extInfo.multi_app_msg_item_list) {
            dumpMsgInfo(info);
        }
    }

    private void formatMsg(MsgList msgList) {
        if (msgList == null) {
            return;
        }

        if (msgList.list == null || msgList.list.length == 0) {
            return;
        }

        for (Msg msg : msgList.list) {
            if (msg.app_msg_ext_info != null) {
                msg.app_msg_ext_info.datetime = msg.comm_msg_info.datetime;
            }
            formatMsgInfo(msg.app_msg_ext_info);
        }
    }

    private void formatMsgInfo(MsgExtInfo extInfo) {
        if (extInfo == null) {
            return;
        }

        extInfo.content_url = extInfo.content_url.replaceAll("&amp;", "&");
        if (extInfo.multi_app_msg_item_list == null || extInfo.multi_app_msg_item_list.length == 0) {
            return;
        }

        for (MsgExtInfo info : extInfo.multi_app_msg_item_list) {
            info.datetime = extInfo.datetime;
            formatMsgInfo(info);
        }
    }

    public static class MsgList {
        public Msg[] list;
    }

    public static class Msg {
        public CommMsgInfo comm_msg_info;
        public MsgExtInfo app_msg_ext_info;
    }

    /*
        "comm_msg_info":{
            "id":1000000097,
            "type":49,
            "datetime":1500372276,
            "fakeid":"3286586496",
            "status":2,
            "content":""
        },
     */
    public static class CommMsgInfo {
        public long id;
        public int type;
        public long datetime;
        public long fakeid;
        public int status;
        public String content;
    }


    /*
        "app_msg_ext_info":{
            "title":"你怎么知道你生活的一切不是假象？",
            "digest":"这一天终于来到了，托尼觉得自己很久以来一直都在等待这一天。",
            "content":"",
            "fileid":100000244,
            "content_url":"http://mp.weixin.qq.com/s?__biz=MzI4NjU4NjQ5Ng==&mid=2247485230&idx=1&sn=c6f094469055528459634ed0f894f343&chksm=ebdbe4c1dcac6dd7fe0cbf06bcf96c2dd7265ec5040c25f032922cc5d288db1386615992cd4d&scene=27#wechat_redirect",
            "source_url":"",
            "cover":"http://mmbiz.qpic.cn/mmbiz_png/MWR3szJ5iaadZic3B8KgNiazR4m6oPM1vQxcEve2zMkCSxMXbmEYFIHT6hePbSaYzeSBsicxq90jyEYpARHOYEvKNQ/0?wx_fmt=png",
            "subtype":9,
            "is_multi":1,
            "multi_app_msg_item_list":[
                {
                    "title":"最畅销的西班牙小说-《风之影》卡洛斯·鲁依斯·萨丰",
                    "digest":"书是镜子，人只能在书里看到自己的内心。《风之影》书籍全球销量超过1500万册，是历史上最畅销的西班牙语小说之",
                    "content":"",
                    "fileid":100000363,
                    "content_url":"http://mp.weixin.qq.com/s?__biz=MzI4NjU4NjQ5Ng==&mid=2247485230&idx=2&sn=5894c85768ca6a96e329f0e4871d2b14&chksm=ebdbe4c1dcac6dd796470e4e4e25e9bc3770711d42dae8534d342fe0ad9284dbfd5e5e7c6b6f&scene=27#wechat_redirect",
                    "source_url":"",
                    "cover":"http://mmbiz.qpic.cn/mmbiz_jpg/MWR3szJ5iaafjTiakyyjTiczSgibkE68mrFiburichyKUia4jOU9QGFZpooibfeDUML55CvYNicLricY750zCZ2TtJvWDrpA/0?wx_fmt=jpeg",
                    "author":"",
                    "copyright_stat":0,
                    "del_flag":1
                },
     */

    public static class MsgExtInfo {
        public String title;
        public String digest;
        public String content;
        public long fileid;
        public String content_url;
        public String source_url;
        public String cover;
        public int subtype;
        public int is_multi;
        public int del_flag;
        public String author;
        public int copyright_stat;
        public MsgExtInfo[] multi_app_msg_item_list;

        public long datetime;
        public int read_num;
        public int like_num;
    }
}
