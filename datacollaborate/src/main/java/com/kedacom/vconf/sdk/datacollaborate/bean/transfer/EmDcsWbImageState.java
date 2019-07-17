/*
 * Copyright (c) 2017 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsWbImageState {
    emImageStateDownloading,        // 文件正在接收，请耐心等待...
    emImageStateDownLoadFail,		// 文件接收失败，非常抱歉！！！
    emImageStateOwnerAlreadyLeave,	// 文件同步失败，发起方可能已断开连接
    emImageStateDownLoadOk,			// 文件接收成功，即将显示
    emImageStateInit,		        // 初始状态
    emImageStateConvertFail,		// 文件转换失败，可能文件错误或已损坏
    emImageStateSelfAlreadyLeave    // 文件接收未完成，已离开会议
}
