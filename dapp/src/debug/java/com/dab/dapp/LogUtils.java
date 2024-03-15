package com.dab.dapp;

import android.util.Log;


/**
 * Created by Mei on 2017/4/8.
 */

public class LogUtils {
    private static String TAG = "日志";
    private static boolean showLog = true;

    public static void setShowLog(boolean showLog) {
        LogUtils.showLog = showLog;
    }

    public static boolean isShowLog() {
        return showLog;
    }

    public static void getTag(String stringInfo) {
        if (!showLog) {
            return;
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String className = stackTrace[1].getFileName();
        String methodName = stackTrace[1].getMethodName();
        int lineNumber = stackTrace[1].getLineNumber();
        String tag = "(" + className + ":" + lineNumber + ")";
        Log.e(tag, methodName + ":" + stringInfo);
    }

    public static void w(Object stringInfo) {
        if (!showLog) {
            return;
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String className = stackTrace[1].getFileName();
        String methodName = stackTrace[1].getMethodName();
        int lineNumber = stackTrace[1].getLineNumber();
        String tag = "(" + className + ":" + lineNumber + ")";
        if (stringInfo == null) {
            Log.w(tag, methodName + ":" + null);
        } else {
            Log.w(tag, methodName + ":" + stringInfo.toString());
        }
    }

    public static void e(Object stringInfo) {
        if (!showLog) {
            return;
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String className = stackTrace[1].getFileName();
        String methodName = stackTrace[1].getMethodName();
        int lineNumber = stackTrace[1].getLineNumber();
        String tag = "(" + className + ":" + lineNumber + ")";
        if (stringInfo == null) {
            Log.e(tag, methodName + ":" + null);
        } else {
            Log.e(tag, methodName + ":" + stringInfo.toString());
        }
    }

    public static void d(Object stringInfo) {
        if (!showLog) {
            return;
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String className = stackTrace[1].getFileName();
        String methodName = stackTrace[1].getMethodName();
        int lineNumber = stackTrace[1].getLineNumber();
        String tag = "(" + className + ":" + lineNumber + ")";
        if (stringInfo == null) {
            Log.d(tag, methodName + ":" + null);
        } else {
            Log.d(tag, methodName + ":" + stringInfo.toString());
        }
    }

}
