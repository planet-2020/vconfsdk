package com.kedacom.vconf.sdk.utils.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * “枚举实例<-->自定义值（Numeric、boolean、String）”序列化反序列化策略
 * 若枚举使用了该注解，则在使用{@link Kson}序列化反序列化时，转换将在枚举实例和getValue的返回的值之间进行。
 *
 * 示例：
 * {@code
 * @EnumCustomValueStrategy
 * public enum COLOR {
 *     RED(2), GREEN(4), BLUE(6);
 *
 *     private final int value;
 *     COLOR(int value){
 *         this.value = value;
 *     }
 *
 *     public int getValue() { // NOTE: 使用该适配器枚举中必须定义原型为"${TYPE} getValue()"的方法。${TYPE}为返回值类型，目前支持基本类型和String。
 *         return value;
 *     }
 * }
 * }
 *
 * 这样，对于
 * class Cup{
 *      ....
 *      COLOR color = COLOR.RED;
 * }
 * Kson.toJson(new Cup());将输出{..., "color": 2}，而非默认的{..., "color": RED}；
 * 反之gson.fromJson("{..., \"color\": 2}")将得到Cup对象其成员color为COLOR.RED。
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumCustomValueStrategy {
}
