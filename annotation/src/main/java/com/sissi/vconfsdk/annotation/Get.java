package com.sissi.vconfsdk.annotation;

/**
 * 用于标记获取消息．
 *
 * 获取是同步的．
 *
 * Created by Sissi on 2018/9/14.
 */

public @interface Get {
    Class para() default Void.class; // 传入参数对应的类
    Class result(); // 返回结果对应的类
}
