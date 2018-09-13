package com.sissi.vconfsdk.frame;

/**
 * Created by Sissi on 1/20/2017.
 */
@SuppressWarnings({"JniMissingFunction", "unused"})
final class NativeMethods {
//    static final HashMap<String, Method> map = new HashMap<String, Method>();

    private NativeMethods(){}

//    /**
//     * 调用native方法（发送请求）
//     * @param methodName 方法名
//     * @param jsonPara json格式的请求参数
//     * */
//    @SuppressWarnings("SameReturnValue")
//    static synchronized boolean invoke(String methodName, String jsonPara){
//        Method method = map.get(methodName);
//        if (null != method){
//            try {
//                if (null != jsonPara) {
//                    method.invoke(null, jsonPara);
//                }else{
//                    method.invoke(null);
//                }
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//            return true;
//        }
//
//        //noinspection TryWithIdenticalCatches
//        try {
//            if (null != jsonPara) {
//                method = NativeMethods.class.getDeclaredMethod(methodName, String.class);
//                method.invoke(null, jsonPara);
//            }else{
//                method = NativeMethods.class.getDeclaredMethod(methodName);
//                method.invoke(null);
//            }
//            map.put(methodName, method);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }
//
//    /**
//     * 调用native方法（获取配置）
//     * @param methodName 方法名
//     * @param outPara 传出参数，用于存放获取到的配置
//     * */
//    static synchronized boolean invoke(String methodName, StringBuffer outPara){
//        Method method;
//        if (null == outPara){
//            return false;
//        }
//        //noinspection TryWithIdenticalCatches
//        try {
//            method = NativeMethods.class.getDeclaredMethod(methodName, StringBuffer.class);
////            KLog.sp("invoke method "+method);
//            method.invoke(null, outPara);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }
//
//
//    //========================native函数（请勿重载）==================================
//    // 方法名必须和对应的请求ID名称保持一致！
//
//    /** 设置native层回调*/
//    static native int Setcallback(Object callback);
//
//    //======================== 初始化
//    /** 设置业务工作路径（一些文件创建所需的根目录）*/
//    private static native int SetSysWorkPathPrefix(String para);
//    /** 启动业务层 */
//    private static native int MtStart(String para);
//    /** 停止业务层 */
//    private static native int MtStop();
//    /** 启动JNI层 */
//    private static native int JniStart(String para);
//    /** 启动业务服务*/
//    private static native int SYSStartService(String para);
//    /** 开关业务日志*/
//    private static native int SetLogCfgCmd(String para);
//    /** 设置己端网络信息*/
//    private static native int SendUsedNetInfoNtf(String para);
//    /** 设置丢包重传参数*/
//    private static native int SetLostPktResendCfgCmd(String para);
//    /** 设置会议加密类型*/
//    private static native int SetEncryptTypeCfgCmd(String para);
//    /** 设置前项纠错*/
//    private static native int SetFECCfgCmd(String para);
//
//    //==========================联系人
//    /** 获取地址簿版本*/
//    private static native int GetSharedDirectoryVersionReq(String para);
//    /** 获取地址簿内容*/
//    private static native int GetSharedDirectoryReq();
//    /** 临时关注某人（以获得其状态变化通知）*/
//    private static native int IMTempSubscribeReq(String para);
//    /** 取消临时关注某人*/
//    private static native int IMUnTempSubscribeReq(String para);
//    /** 查询联系人详情*/
//    public static native int GetAccountInfoReq(String para);
//    /** 批量查询联系人详情*/
//    public static native int QueryUserInfoReq(String para);
//
//    //==========================组织架构
//    /** 获取组织架构版本*/
//    private static native int GetDepartmentVersionReq();
//    /** 获取组织架构包含的部门*/
//    private static native int GetDepartmentCompanyReq();
//    /** 获取部门包含的成员*/
//    private static native int GetDepartmentUserReq(String para);
//    /** 获取组织架构及全体成员*/
//    private static native int GetDepartmentAllReq();
//    /** 按姓名模糊查找组织架构成员*/
//    private static native int GetUserListByStrReq(String para);
//
//
//    //==========================会议
////    private static native int getCallCapPlusCmd(StringBuffer outPara);
//    private static native int SetCallCapPlusCmd(String para);
//    private static native int JoinConfCmd(String para);
//    private static native int OsdCtrlPcAssStreamCmd(String para);
//    private static native int GetSkyShareLoginState(StringBuffer outPara);


    static native int invoke(String methodName, String reqPara);
    static native int invoke(String methodName, StringBuffer output);
    static native int setCallback(Object callback);

}
