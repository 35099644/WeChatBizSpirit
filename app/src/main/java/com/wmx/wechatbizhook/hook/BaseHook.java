package com.wmx.wechatbizhook.hook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by 王明兴 on 2018/3/10.
 */

public class BaseHook {
    protected XC_LoadPackage.LoadPackageParam mLoadPackageParam;

    public BaseHook(XC_LoadPackage.LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
    }

    public void hook() {

    }
}
