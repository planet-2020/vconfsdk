package com.kedacom.vconf.sdk.base.upgrade.bean;

/**
 * 升级包信息
 * */
public class UpgradePkgInfo {
    private String fileName; // 文件名
    private int fileSize;    // 文件大小
    private int versionId; // 版本唯一标识
    private String versionNum;   // 版本号
    private String versionNote;  // 版本说明

    public UpgradePkgInfo(String fileName, int fileSize, int versionId, String versionNum, String versionNote) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.versionId = versionId;
        this.versionNum = versionNum;
        this.versionNote = versionNote;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getVersionId() {
        return versionId;
    }

    public String getVersionNum() {
        return versionNum;
    }

    public String getVersionNote() {
        return versionNote;
    }
}
