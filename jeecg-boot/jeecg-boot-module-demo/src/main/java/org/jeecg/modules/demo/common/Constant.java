package org.jeecg.modules.demo.common;

public class Constant {
    public final static String ESIndex_DefaultType  = "doc";
    public final static String ESIndex_cherry_sensor_device = "cherry_sensor_device";
    public final static String ESIndex_cherry_sensor_slave = "cherry_sensor_slave";
    public final static String ESIndex_cherry_sensor_dataId = "cherry_sensor_dataId";
    public final static String ESIndex_cherry_sensor_data = "cherry_sensor_data";

    public final static String RedisSensorTokenKey = "chain.sensor.token";
    public final static String RedisSensorLockKey = "chain.sensor.lock";

    public final static String LoginUrl = "https://openapi.mp.usr.cn/usrCloud/user/login"; //登录
    public final static String GetDevsForVnUrl = "https://openapi.mp.usr.cn/usrCloud/vn/dev/getDevsForVn"; // 获取某个用户的设备列表
    public final static String GetDevsUrl = "https://openapi.mp.usr.cn/usrCloud/dev/getDevice"; // 获取设备详情
    public final static String GetDataPointInfoByDeviceUrl = "https://openapi.mp.usr.cn/usrCloud/datadic/getDataPointInfoByDevice"; //根据设备获取变量信息
    public final static String GetDeviceDataPointHistoryUrl = "https://openapi.mp.usr.cn/usrCloud/dev/getDeviceDataPointHistory"; //获取设备变量历史记录
}
