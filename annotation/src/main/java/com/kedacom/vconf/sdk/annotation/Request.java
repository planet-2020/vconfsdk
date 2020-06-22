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
     * public static native void login(String jsonLoginPara， String jsonLoginPara2);
     * 则paras={String.class, String.class}
     * */
    Class[] paras() default {};

    /** 用户参数类型。
     * 若为空则认为跟paras的类型一致。
     userParas含义不同于paras，paras为native方法的形参列表，目前大部分情形下是StringBuffer类型的json字符串，而userParas是面向用户（框架使用者）的参数列表。
     例如native方法定义如下：
     public static native void login(StringBuffer jsonLoginPara1, StringBuffer jsonLoginPara2);
     而为了用户使用方便，面向用户的接口可能定义如下：
     public void login(LoginPara1 loginPara1, LoginPara2 loginPara2)
     则paras和userParas的赋值分别为paras={StringBuffer.class, StringBuffer.class}, userParas={LoginPara1.class, LoginPara2.class}
     框架在调用native方法前自动将LoginPara对象转为StringBuffer类型json字符串。

     NOTE: 对于{@link #isGet()}为true的情形，约定{@link #paras()}最后一个值为传出参数类型，userParas最后一个值接收该传出参数并作为结果返回给用户。
     如有如下native方法和用户方法定义：
     public static native void DcsGetServerCfg(String serverId, StringBuffer outpara); // NOTE:最后一个参数为传出参数，native方法使用传出参数反馈请求结果。
     public DCServerCfg getServerCfg(String serverId); // NOTE: 用户接口比native方法少一个传出参数，而通过返回值接受结果。
     则paras和userParas的赋值分别为
     paras={String.class, StringBuffer.class},
     userParas={String.class,
     DCServerCfg.class // NOTE: 用户接口并不需要传入该参数，而是通过返回值接受请求结果，此为用户接口的返回值类型。
     }
     框架在反馈用户结果前自动将StringBuffer类型json字符串outpara转为DCServerCfg对象。

     userPara到para转换规则按优先级从高到低如下：
     1、若userPara为基本类型包装类型，para为对应的基本类型，则将包装类型解包；
     2、若userPara为String，para为StringBuffer则将String转为StringBuffer；
     3、若para为String或StringBuffer，则将userPara转为String或StringBuffer类型的json字符串；
     4、若para为int且userPara为枚举则将该枚举转为json字符串再转为整型（NOTE：这种情况下用户需保证枚举到json的转换已定制化为转为整型）；
     5、其余情形不做转换；
     */
    Class[] userParas() default {};

    /**
     * 是否为GET请求。
     * GET请求不同于普通请求，最后一个userParas为请求结果，详见{@link #userParas()}说明
     * */
    boolean isGet() default false;

    /**
     * 请求对应的响应序列。{@link Response}
     * NOTE：请求也可能没有响应。
     * */
    String[] rspSeq() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq2() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq3() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq4() default {};
    // 添加更多路响应序列很容易（只需在注解处理器中相应增加一行），
    // 但是一个请求有太多可能的响应序列这并不合理，
    // 往往暗示着设计有缺陷，需审视。

    /**
     * 请求对应的超时时长。单位：秒
     * NOTE: 若无响应序列此超时时长无用。
     * */
    int timeout() default 5;
}
