package com.kedacom.vconf.sdk.utils.net;

import com.google.common.base.Splitter;
import java.util.regex.Pattern;

/**
 * Created by Sissi on 2019/8/7
 */
public final class NetAddrHelper {

    private static final Splitter IPV4_SPLITTER = Splitter.on('.').limit(4);

    /**
     * 是否为有效的点分十进制ipv4地址。
     * NOTE: 只做语法格式检查。实际中有些地址是不能作为主机ip地址的，此种因素不考虑在内。
     * */
    public static boolean isValidIp(String ip){
        if (null == ip){
            return false;
        }
        for (int i = 0; i < ip.length(); i++) {
            char c = ip.charAt(i);
            if ( !(Character.isDigit(c) || '.' == c) ){
                return false;
            }
        }
        Iterable<String> fields = IPV4_SPLITTER.split(ip);
        int count = 0;
        for (String field : fields) {
            if (field.startsWith("0") && field.length() != 1){
                return false;
            }
            int val;
            try {
                val = Integer.parseInt(field);
            } catch (NumberFormatException e) {
                return false;
            }
            if (255 < val){
                return false;
            }
            ++count;
        }

        return count == 4;
    }

    /**
     * 是否为有效的ipv4掩码。
     * */
    public static boolean isValidMask(String mask){
        if (!isValidIp(mask)){
            return false;
        }
        try {
            return Pattern.matches("^[1]+[0]+$", Integer.toBinaryString(ipStr2Int(mask)));
        } catch (InvalidIpv4Exception e) {
            return false;
        }
    }

    private static boolean isIpValidMask(int validIp){
        return Pattern.matches("^[1]+[0]+$", Integer.toBinaryString(validIp));
    }

    /**
     * 是否为有效的主机ipv4地址。
     * @return true，主机ip是合法ip且主机位不是全0或全1。
     * */
    public static boolean isValidHost(String ip, String mask){
        try {
            int intIp = ipStr2Int(ip);
            int intMask = ipStr2Int(mask);
            if (!isIpValidMask(intMask)){
                return false;
            }
            return (~intMask & intIp) != 0 && (~intMask & intIp) != ~intMask; // 主机位不能全0或全1
        } catch (InvalidIpv4Exception e) {
            return false;
        }
    }

    public static boolean isValidHost(String ip){
        return !"0.0.0.0".equals(ip) && !"255.255.255.255".equals(ip);
    }

    /**
     * 是否为有效的网络配置。
     *
     * @param hostIp 主机ip
     * @param gatewayIp 网关ip
     * @param mask 掩码
     *
     * @return true, hostIp和gatewayIp均为有效的主机ip且在同一网段。
     * */
    public static boolean isValidNetConfig(String hostIp, String gatewayIp, String mask){
        try {
            int intHostIp = ipStr2Int(hostIp);
            int intGatewayIp = ipStr2Int(gatewayIp);
            int intMask = ipStr2Int(mask);
            if (!isIpValidMask(intMask)){
                return false;
            }
            boolean validHost = (~intMask & intHostIp) != 0 && (~intMask & intHostIp) != ~intMask;
            boolean validGateway = (~intMask & intGatewayIp) != 0 && (~intMask & intGatewayIp) != ~intMask;
            return validHost && validGateway
                    && (intHostIp & intMask) == (intGatewayIp & intMask); // 在同一网段
        } catch (InvalidIpv4Exception e) {
            return false;
        }
    }


//    /**
//     * 是否为本地ipv4地址。
//     * */
//    public static boolean isLocalIp(String ip, String mask){
//        try {
//            InetAddress inetAddresses = Inet4Address.getByName(ip);
//            inetAddresses.is
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//        return InetAddresses.(ip);
//    }
//
//    /**
//     * 是否为广播ipv4地址。
//     * */
//    public static boolean isBroadcastIp(String ip, String mask){
//        return InetAddresses.isInetAddress(ip);
//    }
//
//    /**
//     * 是否为组播ipv4地址。
//     * */
//    public static boolean isMultiCastIp(String ip, String mask){
//        return InetAddresses.isInetAddress(ip);
//    }

//    /**
//     * 获取子网地址。
//     * */
//    public static boolean isMultiCastIp(String ip, String mask){
//        return InetAddresses.isInetAddress(ip);
//    }
//
//    /**
//     * 获取主机位。
//     * */
//    public static boolean isMultiCastIp(String ip, String mask){
//        return InetAddresses.isInetAddress(ip);
//    }

    /**
     * 点分十进制ip字符串转整型
     * */
    public static int ipStr2Int(String ip) throws InvalidIpv4Exception {
        if (!isValidIp(ip)){
            throw new InvalidIpv4Exception(ip);
        }
        String[] fields = ip.split("\\.");
        return (Integer.parseInt(fields[0]) << 24)
                | (Integer.parseInt(fields[1]) << 16)
                | (Integer.parseInt(fields[2]) << 8)
                | Integer.parseInt(fields[3]);
    }

    /**
     * 整型ip转点分十进制ip字符串
     * */
    public static String ipInt2Str(int ip){
        final StringBuilder sb = new StringBuilder();
        sb.append(ip>>>24).append(".")
            .append(ip>>>16 & 0xff).append(".")
            .append((ip>>>8 & 0xff)).append(".")
            .append(ip & 0xff);
        return sb.toString();
    }


    /**
     * 点分十进制ip字符串转长整型
     * */
    public static long ipStr2Long(String ip) throws InvalidIpv4Exception {
        if (!isValidIp(ip)){
            throw new InvalidIpv4Exception(ip);
        }
        String[] fields = ip.split("\\.");
        return (Long.parseLong(fields[0]) << 24)
                | (Long.parseLong(fields[1]) << 16)
                | (Long.parseLong(fields[2]) << 8)
                | Long.parseLong(fields[3]);
    }

    /**
     * 长整型ip转点分十进制ip字符串
     * */
    public static String ipLong2Str(long ip){
        final StringBuilder sb = new StringBuilder();
        sb.append(ip>>>24).append(".")
                .append(ip>>>16 & 0xff).append(".")
                .append((ip>>>8 & 0xff)).append(".")
                .append(ip & 0xff);
        return sb.toString();
    }



    /**
     * 点分十进制ip字符串转整型（小端模式）
     * */
    public static int ipStr2IntLittleEndian(String ip) throws InvalidIpv4Exception {
        if (!isValidIp(ip)){
            throw new InvalidIpv4Exception(ip);
        }
        String[] fields = ip.split("\\.");
        return (Integer.parseInt(fields[3]) << 24)
                | (Integer.parseInt(fields[2]) << 16)
                | (Integer.parseInt(fields[1]) << 8)
                | Integer.parseInt(fields[0]);
    }


    /**
     * 点分十进制ip字符串转长整型（小端模式）
     * */
    public static long ipStr2LongLittleEndian(String ip) throws InvalidIpv4Exception {
        if (!isValidIp(ip)){
            throw new InvalidIpv4Exception(ip);
        }
        String[] fields = ip.split("\\.");
        return (Long.parseLong(fields[3]) << 24)
                | (Long.parseLong(fields[2]) << 16)
                | (Long.parseLong(fields[1]) << 8)
                | Long.parseLong(fields[0]);
    }

    /**
     * Ip格式掩码转位长形式掩码。
     * 如ip格式掩码255.255.255.0，转为位长形式后为24位。
     * @return 掩码长度，或者-1若掩码非法。
     * */
    public static int maskIp2Len(String maskIp){
        try {
            int intMask = ipStr2Int(maskIp);
            if (!isIpValidMask(intMask)){
                return -1;
            }
            String binStr = Integer.toBinaryString(intMask);
            int firstZeroIndx = binStr.indexOf("0");
            if (-1 == firstZeroIndx) {
                return 32;
            }
            return binStr.substring(0, firstZeroIndx).length();
        } catch (InvalidIpv4Exception e) {
            return -1;
        }
    }

    /**
     * 位长形式掩码转Ip格式掩码。
     * 如24位掩码转Ip形式掩码为255.255.255.0
     * @return ip形式掩码，或者null若掩码长度非法。
     * */
    public static String maskLen2Ip(int maskLen){
        if (maskLen<=0 || 32<=maskLen){
            return null;
        }
        return ipInt2Str(0xffffffff << 32-maskLen);
    }


    public static class InvalidIpv4Exception extends Exception{

        public InvalidIpv4Exception(String ip) {
            super("invalid ip " + ip);
        }

        public InvalidIpv4Exception(int ip) {
            super("invalid ip " + ip);
        }

    }

}
