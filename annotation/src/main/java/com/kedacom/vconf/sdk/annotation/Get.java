package com.kedacom.vconf.sdk.annotation;

/**
 * 用于标记获取消息．
 *
 * 获取是同步的．
 *
 * Created by Sissi on 2018/9/14.
 */

public @interface Get {
    String name() default ""; // 传递给下层的消息名称，若为空则使用被修饰的枚举的name。
    Class para() default Void.class; // 传入参数对应的类
    Class result(); // 返回结果对应的类
}
