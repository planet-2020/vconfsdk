/**
 * 模拟器控制开关，用来控制模拟器是否可用。
 * 模拟器仅用于本地调试，正式版本应始终为禁用状态，故该文件在第一次提交到版本库后应禁止后续提交
 * */
package com.kedacom.vconf.sdk.base;
public final class SimulatorOnOff {
    /**
     * 模拟器是否可用，true：可用，false：不可用。
     * */
    public static final boolean on = false;
}
