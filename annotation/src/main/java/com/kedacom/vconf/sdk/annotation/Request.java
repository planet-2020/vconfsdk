package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记请求.
 *
 * 请求可以是同步的，也可以是异步的。
 * 同步请求即为普通的方法调用，方法调用结束请求结果产生请求结束，如设置/获取配置。同步请求没有响应。
 * 异步请求是消息交互模式，方法调用结束后请求发出等待响应，收齐请求对应的响应后，请求结束。异步请求也可以没有响应。
 *
 * Created by Sissi on 2018/9/3.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Request {

    /** 对应的native方法名称
     * 如LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara);
     * 则method为"login"
     * */
    String method();

    /**
     * 对应的native方法所属类
     * 如com.kedacom.kdv.mt.mtapi.LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara);
     * 则owner为"com.kedacom.kdv.mt.mtapi.LoginManager"
     * */
    String owner();

    /** 对应的native方法所需参数类型
     * 如LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara);
     * 则paras为String.class
     * */
    Class[] paras() default {};

    /** 用户参数类型。
     NOTE：userParas不同于paras，paras为native方法的形参列表，目前大部分情形下是StringBuffer类型的json字符串，而userParas是面向用户的参数列表。
     例如native方法定义如下：
     public static native void login(StringBuffer jsonLoginPara);
     而为了用户使用方便，面向用户的接口可能定义如下：
     public void login(LoginPara para)
     这里的LoginPara即为userParas，StringBuffer即为paras，框架在调用native方法前自动将LoginPara对象转为StringBuffer类型json字符串。

     userPara到para转换规则按优先级从高到低如下：
     1、若userPara为基本类型包装类型，para为对应的基本类型，则将包装类型解包；
     2、若userPara为String，para为StringBuffer则将String转为StringBuffer；
     3、若para为String或StringBuffer，则将userPara转为String或StringBuffer类型的json字符串；
     4、其余情形不做转换；
     */
    Class[] userParas() default {};

    /**
     * 请求类型。
     * */
    int SESSION = 0; // “请求——响应”，异步。
    int GET = 1; // 如获取配置。
    int SET = 2; // 如设置配置。
    int type() default SESSION;

    /**
     * 请求对应的响应序列。{@link Response}
     * NOTE：请求也可能没有响应。
     * */
    String[] rspSeq() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * NOTE: 可以使用java8新增的元注解@Repeatable替代这种方案，尤其如果响应序列无限多。
     * */
    String[] rspSeq2() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq3() default {};

    /**
     * 请求对应的超时时长。单位：秒
     * NOTE: 若无响应序列此超时时长无用。
     * */
    int timeout() default 5;
//    boolean isMutualExclusive() default false; // 是否互斥。若互斥则仅容许存在一个进行中的该类请求。比如若startup互斥，当发出一个startup且响应尚未收到时，此时又来一个startup则后一个被丢弃。
}
