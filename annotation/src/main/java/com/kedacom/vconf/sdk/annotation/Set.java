package com.kedacom.vconf.sdk.annotation;

/**
 * 用于标记设置消息．
 *
 * 设置是同步的．
 *
 * Created by Sissi on 2018/9/14.
 */

public @interface Set {
    String name() default ""; // 传递给下层的消息名称，若为空则使用被修饰的枚举的name。
    Class value(); // 传入参数对应的类
}
