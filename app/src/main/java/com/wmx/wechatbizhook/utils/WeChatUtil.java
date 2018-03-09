package com.wmx.wechatbizhook.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.telephony.TelephonyManager;

import com.wmx.wechatbizhook.GlobalConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * Created by wangmingxing on 18-3-7.
 */

public class WeChatUtil {
    private static final String TAG = "WeChatUtil";

    public static String getUin() {
        SharedPreferences sp = GlobalConfig.mAppContext.getSharedPreferences("auth_info_key_prefs", Context.MODE_PRIVATE);
        return String.valueOf(sp.getInt("_auth_uin", 0));
    }

    public static String getImei() {
        TelephonyManager phone = (TelephonyManager) GlobalConfig.mAppContext.getSystemService(Context.TELEPHONY_SERVICE);
        return phone.getDeviceId();
    }

    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuilder buf = new StringBuilder();
            for (byte element : b) {
                i = element;
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDbPasswd() {
        String passwd = md5(getImei() + getUin());
        return passwd.substring(0, 7).toLowerCase();
    }

    public static File getContactDbFile() {
        ApplicationInfo appInfo = GlobalConfig.mAppContext.getApplicationInfo();
        File dir = new File(appInfo.dataDir, "MicroMsg");
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }

                File[] subDirFiles = file.listFiles();
                for (File subFile : subDirFiles) {
                    if (subFile.isDirectory()) {
                        continue;
                    }

                    if (subFile.getName().equals("EnMicroMsg.db")) {
                        return subFile;
                    }
                }
            }
        }
        return null;
    }

    public static void copyFile(File source, File dest) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            LogWriter.e(TAG, "copyFile error:", e);
        } finally {
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                LogWriter.e(TAG, "copyFile error:", e);
            }
        }
    }

    public static boolean isMainProcess(String processName) {
        return "com.tencent.mm".equals(processName);
    }

    public static boolean isWebViewProcess(String processName) {
        return "com.tencent.mm:tools".equals(processName);
    }

    public static void startWebViewUI(Context context, String url) {
        ClassLoader classLoader = context.getClassLoader();
        try {
            Class webviewUIClass = classLoader.loadClass("com.tencent.mm.plugin.webview.ui.tools.WebViewUI");
            Intent intent = new Intent(context, webviewUIClass);
            intent.putExtra("rawUrl", url);
            context.startActivity(intent);
        } catch (ClassNotFoundException e) {
            LogWriter.e(TAG, "startWebViewUI", e);
        }
    }
}
