package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记消息定义枚举类.
 *
 * Created by Sissi on 2018/9/5.
 */


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Message {
    /**
     * 消息所属模块。
     * 将作为前缀添加到该模块每一条消息名称前。
     * 建议使用模块缩写名且各模块间不要重复，如针对DataCollaborate模块可取值"DC"。
     * */
    String module();
}
