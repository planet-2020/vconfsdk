package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记请求消息.
 *
 * Created by Sissi on 2018/9/3.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Request {
    Class para() default Void.class;  // 请求参数对应的类
    String[] rspSeq() default {}; // 请求对应的响应序列。注：请求也可能没有响应，此时不用填写让它默认为空就好。
    String[] rspSeq2() default {}; // 请求对应的另一个可能的响应序列
    String[] rspSeq3() default {}; // 请求对应的另一个可能的响应序列
    int timeout() default 10;   // 请求对应的超时。单位：秒
}
