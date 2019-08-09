package com.kedacom.vconf.sdk.utils.net;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Created by Sissi on 2019/8/6
 */
public final class NetworkHelper {

    private static NetInfo netInfo;
    private static ConnectivityManager connMan;
    private static WifiManager wifiMan;

    public synchronized static void init(@Nonnull Application context){
        if (null != netInfo){
            return;
        }

        connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == connMan){
            KLog.p(KLog.ERROR, "null == connMan");
            return;
        }
        wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == wifiMan){
            KLog.p(KLog.ERROR, "null == wifiMan");
            return;
        }

        netInfo = new NetInfo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connMan.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(),  new MyNetworkCallback());
            connMan.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),  new MyNetworkCallback());
            connMan.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),  new MyNetworkCallback());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connMan.getActiveNetwork(); //TODO 测试下断网情况下还能不能获取到，因为对比NetworkInfo此处未判断是否connected
                if (null != network){
                    netInfo.state = STATE_CONNECTED;
                    updateNetInfo(network);
                }
            }

        }else{

            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    KLog.p("onReceive: intent=%s", intent);
                    NetworkInfo networkInfo = connMan.getActiveNetworkInfo();
                    if (null != networkInfo){
                        updateNetInfo(networkInfo);
                    }else{
                        netInfo.clear();
                    }
                }
            }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

            NetworkInfo networkInfo = connMan.getActiveNetworkInfo();
            if (null != networkInfo){
                updateNetInfo(networkInfo);
            }
        }

    }


    /**
     * Indicates this network uses a Cellular transport.
     */
    public static final int TRANS_CELLULAR = 0;

    /**
     * Indicates this network uses a Wi-Fi transport.
     */
    public static final int TRANS_WIFI = 1;

//    /**
//     * Indicates this network uses a Bluetooth transport.
//     */
//    public static final int TRANS_BLUETOOTH = 2;

    /**
     * Indicates this network uses an Ethernet transport.
     */
    public static final int TRANS_ETHERNET = 3;

    /**
     * Indicates this network uses a VPN transport.
     */
    public static final int TRANS_VPN = 4;

//    /**
//     * Indicates this network uses a Wi-Fi Aware transport.
//     */
//    public static final int TRANS_WIFI_AWARE = 5;
//
//    /**
//     * Indicates this network uses a LoWPAN transport.
//     */
//    public static final int TRANS_LOWPAN = 6;

    public static final int TRANS_UNKNOWN = 7;


    public static boolean isConnected(){
        return null != netInfo && netInfo.state == STATE_CONNECTED;
    }

    public static int getTransType(){
        return null!=netInfo ? netInfo.transType : TRANS_UNKNOWN;
    }

    public static String getAddr(){
        return null!=netInfo ? netInfo.addr : null;
    }

    public static String getDns(){
        return null!=netInfo ? netInfo.dns : null;
    }

    public static String getGateway(){
        return null!=netInfo ? netInfo.gateway : null;
    }


    public static final int STATE_CONNECTED = 10;
    public static final int STATE_DISCONNECTED = 11;
    private static class NetInfo{
        int state = STATE_DISCONNECTED;
        int transType = TRANS_UNKNOWN;
        String addr="";
        String dns="";
        String gateway="";

        void clear(){
            state = STATE_DISCONNECTED;
            transType = TRANS_UNKNOWN;
            addr = "";
            dns = "";
            gateway = "";
        }

        @Override
        public String toString() {
            return "NetInfo{" +
                    "state=" + state +
                    ", transType=" + transType +
                    ", addr='" + addr + '\'' +
                    ", dns='" + dns + '\'' +
                    ", gateway='" + gateway + '\'' +
                    '}';
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void updateNetInfo(Network network){
        updateNetInfo(connMan.getNetworkCapabilities(network));
        updateNetInfo(connMan.getLinkProperties(network));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void updateNetInfo(LinkProperties linkProperties){
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()){
            InetAddress inetAddress = linkAddress.getAddress();
            if (inetAddress instanceof Inet4Address){
                netInfo.addr = inetAddress.getHostAddress();
                break;
            }
        }
        for (InetAddress dns : linkProperties.getDnsServers()){
            if (dns instanceof Inet4Address) {
                netInfo.dns = dns.getHostAddress();
                break;
            }
        }
        for (RouteInfo routeInfo : linkProperties.getRoutes()){
            InetAddress inetAddress = routeInfo.getGateway();
            if (inetAddress instanceof Inet4Address) {
                netInfo.gateway = inetAddress.getHostAddress();
                break;
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void updateNetInfo(NetworkCapabilities networkCapabilities){
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){ // 如果有以太网连接系统默认会选以太网
            netInfo.transType = TRANS_ETHERNET;
        }else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
            netInfo.transType = TRANS_WIFI;
        }else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
            netInfo.transType = TRANS_CELLULAR;
        }else{
            netInfo.transType = TRANS_UNKNOWN;
        }
    }

    private static void updateNetInfo(NetworkInfo networkInfo){
        if (!networkInfo.isConnected()){
            netInfo.state = STATE_DISCONNECTED;
            return;
        }
        netInfo.state = STATE_CONNECTED;
        if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET){
            netInfo.transType = TRANS_ETHERNET;
        }else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            netInfo.transType = TRANS_WIFI;
        }else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
            netInfo.transType = TRANS_CELLULAR;
        }else{
            netInfo.transType = TRANS_UNKNOWN;
        }

        if (TRANS_WIFI == netInfo.transType){
            netInfo.addr = getWifiIpAddr();
        }else{
            netInfo.addr = getIpAddress();
        }

        netInfo.dns = getDnsFromSystemProperties();
    }


    /**
     * 获取ip地址。
     * API level >=21 可以不用此法而通过{@link LinkProperties}获取。
     * */
    private static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 获取wifi连接状态下的ip。
     * 有些机型上{@link #getIpAddress()}拿不到正确结果，所以wifi连接状态下使用该方法获取ip。
     * */
    private static String getWifiIpAddr(){
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return "WifiIpAddr"; // TODO
    }

    /**
     * 获取DNS。
     * 仅适用于 API level < 26
     * */
    @SuppressWarnings("unchecked")
    private static String getDnsFromSystemProperties() {

        ArrayList<String> dnsList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return "";
        }

        // This originally looked for all lines containing .dns; but
        // http://code.google.com/p/android/issues/detail?id=2207#c73
        // indicates that net.dns* should always be the active nameservers, so
        // we use those.
        final String re1 = "^\\d+(\\.\\d+){3}$";
        final String re2 = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$";

        try {
            @SuppressLint("PrivateApi")
            Class SystemProperties = Class.forName("android.os.SystemProperties");
            //                Method method = SystemProperties.getMethod("get", new Class[]{String.class});
            Method method = SystemProperties.getMethod("get", String.class);
            final String[] netdns = new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4"};
            for (int i = 0; i < netdns.length; i++) {
//                    Object[] args = new Object[]{netdns[i]};
//                    String v = (String) method.invoke(null, args);
                String v = (String) method.invoke(null, netdns[i]);
                if (v != null && (v.matches(re1) || v.matches(re2)) && !dnsList.contains(v)) {
                    dnsList.add(v);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return dnsList.size()>0 ? dnsList.get(0) : "";
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class MyNetworkCallback extends ConnectivityManager.NetworkCallback{

        @Override
        public void onAvailable(Network network) {
            netInfo.clear();
            netInfo.state = STATE_CONNECTED;
        }

        @Override
        public void onLost(Network network) {
            netInfo.clear();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            updateNetInfo(connMan.getNetworkCapabilities(network));
            updateNetInfo(linkProperties);
        }

    }

}
