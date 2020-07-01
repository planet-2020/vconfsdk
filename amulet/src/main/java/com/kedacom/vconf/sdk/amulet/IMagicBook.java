package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public interface IMagicBook {

    /**
     * magicbook名称
     * */
    @NonNull String name();

    /**
     * 请求ID对应的请求名称
     * （目前即为native方法名称）
     * 请求名称是面向native层的，请求ID是面向用户层的。
     * 请求ID跟请求名称的映射关系是多对1（绝大大部分情形下是一对一）
     * */
    @Nullable String reqName(@NonNull String reqId);

    /**
     * 是否为GET请求
     * @param reqId 请求ID
     * */
    boolean isGet(@NonNull String reqId);

    /**
     * 请求对应的native方法所属类
     * NOTE: 实际上此方法的必要性取决于和native层的具体约定，比如可以约定native方法的原型固定为一个且仅有一个，
     * 参数形式也约定好，这样Java层调用的始终是同一个native方法，在native层再根据标签如ReqName去做分发，
     * 这样就不再需要指定具体的Owner了，新增接口也不再需要新增native方法，只需新增标签。
     * 然而现有的JNI层的未经过充分的协商和和精心的设计且这部分是积重难返且由其他组开发维护，所以此处只能是迁就。
     * */
    @Nullable String nativeMethodOwner(@NonNull String reqId);

    /**
     * 请求对应的native方法参数列表
     * */
    @Nullable Class<?>[] nativeParaClasses(@NonNull String reqId);

    /**
     * 请求对应的用户方法参数列表
     * native方法和用户方法举例：
     * native方法定义如下：
     * public static native void login(StringBuffer jsonLoginPara1, StringBuffer jsonLoginPara2);
     * 而为了用户使用方便，面向用户的接口可能定义如下：
     * public void login(LoginPara1 loginPara1, LoginPara2 loginPara2)
     * 则native方法参数列表和用户方法参数列表分别为{StringBuffer.class, StringBuffer.class}, {LoginPara1.class, LoginPara2.class}
     * 框架负责将用户方法映射到native方法。
     * */
    @Nullable Class<?>[] userParaClasses(@NonNull String reqId);

    /**
     * 请求对应的响应消息序列列表
     * NOTE: 请求可能没有响应。
     * */
    @Nullable String[][] rspSeqs(@NonNull String reqId);

    /**
     * 请求对应的超时时长。
     * NOTE： 对于没有响应的请求此方法不适用
     * @return 超时时长，单位：秒。
     * */
    int timeout(@NonNull String reqId);

    /**
     * 响应名称对应的响应ID集合
     * 响应名称是面向native层的，响应ID是面向用户层的。
     * 响应ID跟响应名称的映射关系是多对1（绝大大部分情形下是一对一）
     * @param rspName 响应名称，若为null则获取该magicbook中所有响应的ID
     * */
    @Nullable Set<String> rspIds(@Nullable String rspName);

    /**
     * 响应内容对应的类
     * */
    @Nullable Class<?> rspClass(@NonNull String rspId);

    /**
     * 是否为贪婪标记。
     * */
    boolean isGreedyNote(@NonNull String rspId);

    /**
     * 通知名称对应的通知ID集合
     * 通知名称是面向native层的，通知ID是面向用户层的。
     * 通知ID跟通知名称的映射关系是多对1（绝大大部分情形下是一对一）
     * @param ntfName 通知名称，若为null则获取该magicbook中所有通知的ID
     * */
    @Nullable Set<String> ntfIds(@Nullable String ntfName);

    /**
     * 通知内容对应的类
     * */
    @Nullable Class<?> ntfClass(@NonNull String ntfId);

}
