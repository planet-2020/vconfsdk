package com.sissi.vconfsdk.annotation;

/**
 * 用来标记通知消息．
 *
 * 不同于响应消息是由本端请求消息触发的，通知消息是由远端主动推过来的．
 *
 * Created by Sissi on 2018/9/14.
 */

public @interface Notification {
    Class clz(); // 通知消息体对应的类
    int delay() default 100; // 延时（单位：毫秒）。仅用于模拟模式。
}
