package com.sissi.vconfsdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记请求消息.
 *
 * ＂请求－响应＂是异步的．　
 *
 * Created by Sissi on 2018/9/3.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Request {
    Class reqPara() default Void.class;  // 请求参数对应的类
    String[] rspSeq() default {}; // 请求对应的响应序列
    String[] rspSeq2() default {}; // 请求对应的第2路响应序列
    String[] rspSeq3() default {}; // 请求对应的第3路响应序列
    int timeout() default 10;   // 请求对应的超时
}
