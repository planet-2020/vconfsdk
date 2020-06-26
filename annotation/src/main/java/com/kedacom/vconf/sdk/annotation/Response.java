package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记一条响应。
 * 响应是由请求{@link Request}触发的，作为对比通知{@link Notification}是平台主动推过来的。
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Response {
    String name();  // 响应名称
    Class clz();    // 响应内容
}
