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
    String name();      // 请求对应的方法名
    String methodOwner(); // 请求对应的方法的所属类
    Class[] methodParas() default {}; // 请求对应的方法所需参数。

    /* 请求者传入的参数列表。
        NOTE：paras不同于methodParas，methodParas为jni方法的形参列表，目前大部分情形下是StringBuffer类型的json字符串，
        而paras是面向用户的参数列表，最终传给jni方法时会做类型转换以匹配methodParas。
        例如jni方法login(StringBuffer loginPara)，methodParas=StringBuffer.class，但是为了方便用户登录时实际传入的是LoginPara对象，
        框架自动将LoginPara对象转为StringBuffer类型json字符串再传给login方法。

        para到methodPara转换规则按优先级从高到低如下：
        1、如果para为基本类型包装类型，MethodPara为对应的基本类型则将包装类型解包；
        2、如果para为String，MethodPara为StringBuffer则将String转为StringBuffer；
        3、如果methodPara为String或StringBuffer，则将para转为String或StringBuffer类型的json字符串；
        4、其余情形不做转换直接将para赋给methodPara；
    */
    Class[] paras() default {};

    String[] rspSeq() default {}; // 请求对应的响应序列。注：请求也可能没有响应，此时不用填写让它默认为空就好。
    String[] rspSeq2() default {}; // 请求对应的另一个可能的响应序列
    String[] rspSeq3() default {}; // 请求对应的另一个可能的响应序列
    int timeout() default 10;   // 请求对应的超时。单位：秒
    boolean isMutualExclusive() default false; // 是否互斥。若互斥则仅容许存在一个进行中的该类请求。比如若startup互斥，当发出一个startup且响应尚未收到时，此时又来一个startup则后一个被丢弃。
}
