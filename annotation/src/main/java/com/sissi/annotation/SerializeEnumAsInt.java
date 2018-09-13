package com.sissi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Sissi on 2018/9/6.
 */


/**
 * 序列化枚举为整型.如enum Color{Red, Green},Color.Red做json转换时默认会被序列化为Red,使用该注解标注后将序列化为其ordinal,即0.
 *
 * 若所修饰的类为枚举类型则直接针对该类生效. 否则针对其直接内部枚举类生效. 如@SerializeEnumAsInt Class Beans{ enum Color{red, green}, enum Size{Big, Small} }, 针对Color和Size生效
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SerializeEnumAsInt {
//    boolean value() default true;
}
