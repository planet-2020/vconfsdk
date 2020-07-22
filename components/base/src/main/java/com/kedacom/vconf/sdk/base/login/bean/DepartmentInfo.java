package com.kedacom.vconf.sdk.base.login.bean;

/**
 * 部门信息
 * */
public class DepartmentInfo {
    public int id; // 部门id
    public String name; // 部门名称
    public String path; // 部门路径

    public DepartmentInfo(int id, String name, String path) {
        this.id = id;
        this.name = name;
        this.path = path;
    }
}
