package com.crazypumpkin.library;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by CrazyPumPkin on 2017/6/22.
 */

public class Util {

    public static int dp2px(float dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * 获取对应View的当前位置
     * @param activity
     * @param view
     * @param ignoreStatusBar 是否不计状态栏的高度
     * @return
     */
    public static Rect getViewRect(Activity activity, View view, boolean ignoreStatusBar) {
        Rect result = new Rect();
        view.getGlobalVisibleRect(result);
        if (ignoreStatusBar) {
            result.offset(0, -getStatusBarHeight(activity));
        }
        return result;
    }

    public static int getStatusBarHeight(Activity activity){
        Rect decorViewRect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(decorViewRect);
        return decorViewRect.top;
    }

}
