package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记响应消息．
 * 响应消息一定由请求消息触发．
 *
 * Created by Sissi on 2018/9/3.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Response {
    String name() default "";  // 下层传递上来的响应消息名称，若为空则使用被修饰的枚举的name。
    Class clz() default Void.class; // 响应消息体对应的类
    int delay() default 100; // 延时（单位：毫秒）。仅模拟模式生效。
}
