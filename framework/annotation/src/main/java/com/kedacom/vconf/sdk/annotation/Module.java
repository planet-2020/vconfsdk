package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记消息所属模块.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Module {
    /**
     * 名称
     * 建议使用模块名缩写，例如：
     * DataCollaborate——"DC"
     * Upgrade——"UG"
     * 缩写命名可参考：https://www.allacronyms.com/
     * */
    String name();
}
