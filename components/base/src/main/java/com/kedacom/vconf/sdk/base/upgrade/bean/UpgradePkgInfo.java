package com.kedacom.vconf.sdk.base.upgrade.bean;

/**
 * 升级包信息
 * */
public class UpgradePkgInfo {
    public String fileName; // 文件名
    public int fileSize;    // 文件大小
    public int versionId; // 版本唯一标识
    public String versionNum;   // 版本号
    public String versionNote;  // 版本说明

    public UpgradePkgInfo(String fileName, int fileSize, int versionId, String versionNum, String versionNote) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.versionId = versionId;
        this.versionNum = versionNum;
        this.versionNote = versionNote;
    }
}
