package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记请求消息.
 *
 * 请求是异步的，一般会有对应的响应。
 *
 * Created by Sissi on 2018/9/3.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Request {
    String name() default "";      // 请求对应的方法名
    Class methodOwner() default Void.class; // 请求对应的方法的所属类
    Class[] paras() default Void.class; // 请求对应的方法所需参数列表
    String[] rspSeq() default {}; // 请求对应的响应序列。注：请求也可能没有响应，此时不用填写让它默认为空就好。
    String[] rspSeq2() default {}; // 请求对应的另一个可能的响应序列
    String[] rspSeq3() default {}; // 请求对应的另一个可能的响应序列
    int timeout() default 10;   // 请求对应的超时。单位：秒
    boolean isMutualExclusive() default false; // 是否互斥。若互斥则仅容许存在一个进行中的该类请求。比如若startup互斥，当发出一个startup且响应尚未收到时，此时又来一个startup则后一个被丢弃。
}
