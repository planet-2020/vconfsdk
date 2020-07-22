package com.kedacom.vconf.sdk.utils.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * “跛子”序列化反序列化策略
 * 若使用了该注解，则在使用{@link Kson}反序列化时，能正确应对如下情形：
 *
 * Class X{
 *     A MainParam;
 *     B AssParam;
 * }
 * 有些场景下下层返回的X实例的json字符串是如下完整形式：
 * {MainParam: val1, AssParam: val2}
 * 有些场景下json字符串是如下“跛子”形式——缺少AssParam：
 * {val1}
 *
 *
 * 使用示例：
 * {@code
 * @LameStrategy(mainField=A.class)
 * Class X{
 *     A MainParam;
 *     B AssParam;
 * }
 * 这样，无论是完整形式的“{MainParam: val1, AssParam: val2}”字符串还是“跛子”形式的"{val1}}"我们都能正确反序列化，得到X实例。
 * */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LameStrategy {
    /**
     * 主要字段。“跛子”形式下仅存的字段。
     * */
    Class mainField();
}
