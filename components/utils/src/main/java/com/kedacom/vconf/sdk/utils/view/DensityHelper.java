package com.kedacom.vconf.sdk.utils.view;

import android.content.Context;

/**
 * Created by Sissi on 2019/8/14
 */
public final class DensityHelper {

    /**
     * dip转pixel。
     * @param density 屏幕密度。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float dpToPx(float dp, float density, boolean bRound) {
        if (bRound){
            return (int) (dp*density + 0.5f);
        }else{
            return dp*density;
        }
    }
    /**
     * dip转pixel。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float dpToPx(float dp, Context context, boolean bRound) {
        return dpToPx(dp, context.getResources().getDisplayMetrics().density, bRound);
    }

    /**
     * pixel转dip。
     * @param density 屏幕密度。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float pxToDp(float px, float density, boolean bRound) {
        if (bRound){
            return (int) (px/density + 0.5f);
        }else{
            return px / density;
        }
    }
    /**
     * pixel转dip。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float pxToDp(float px, Context context, boolean bRound) {
        return pxToDp(px, context.getResources().getDisplayMetrics().density, bRound);
    }

    /**
     * sp转pixel。
     * @param scaledDensity 放缩的屏幕密度（受设置中字体大小设置影响）。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float spToPx(float sp, float scaledDensity, boolean bRound) {
        if (bRound){
            return (int) (sp * scaledDensity + 0.5f);
        }else{
            return sp * scaledDensity;
        }
    }
    /**
     * sp转pixel。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float spToPx(float sp, Context context, boolean bRound) {
        return spToPx(sp, context.getResources().getDisplayMetrics().scaledDensity,bRound);
    }

    /**
     * pixel转sp
     * @param scaledDensity 放缩的屏幕密度（受设置中字体大小设置影响）。
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float pxToSp(float px, float scaledDensity, boolean bRound) {
        if (bRound){
            return (int) (px/scaledDensity + 0.5f);
        }else{
            return px / scaledDensity;
        }
    }

    /**
     * pixel转sp
     * @param bRound 转换结果是否需要四舍五入取整。
     * */
    public static float pxToSp(float px, Context context, boolean bRound) {
        return pxToSp(px, context.getResources().getDisplayMetrics().scaledDensity, bRound);
    }


}
