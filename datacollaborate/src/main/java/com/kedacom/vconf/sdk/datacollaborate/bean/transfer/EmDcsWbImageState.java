/*
 * Copyright (c) 2017 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsWbImageState {
    emImageStateDownloading,        // 文件正在接收，请耐心等待...
    emImageStateDownLoadFail,		// 文件接收失败，非常抱歉！！！
    emImageStateOwnerAlreadyLeave,	// 文件同步失败，发起方可能已断开连接
    emImageStateDownLoadOk,			// 文件接收成功，即将显示
    emImageStateInit,		        // 初始状态
    emImageStateConvertFail,		// 文件转换失败，可能文件错误或已损坏
    emImageStateSelfAlreadyLeave;    // 文件接收未完成，已离开会议

    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(EmDcsWbImageState.class, new Enum2IntJsonAdapter());
    }
}
