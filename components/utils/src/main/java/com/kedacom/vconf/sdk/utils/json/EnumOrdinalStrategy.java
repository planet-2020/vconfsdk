package com.kedacom.vconf.sdk.utils.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *“枚举实例<-->枚举序号(ordinal)”序列化反序列化策略。
 * 若枚举使用了该注解，则在使用{@link Kson}序列化反序列化时，转换将在枚举实例和其ordinal之间进行。
 *
 * 示例：
 * {@code
 * @EnumOrdinalStrategy
 * public enum COLOR {
 *      RED, GREEN, BLUE,
 * }
 * }
 *
 * 这样，对于
 * class Cup{
 *      ....
 *      COLOR color = COLOR.RED;
 * }
 * Kson.toJson(new Cup());将输出{..., "color": 1}，而非默认的{..., "color": RED}；
 * 反之Kson.fromJson("{..., \"color\": 1}")将得到Cup对象其成员color为COLOR.RED。
 *
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumOrdinalStrategy {
}
