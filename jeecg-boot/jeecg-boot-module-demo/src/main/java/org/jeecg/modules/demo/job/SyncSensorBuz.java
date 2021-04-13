package org.jeecg.modules.demo.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.boot.starter.lock.client.RedissonLockClient;
import org.jeecg.common.es.JeecgElasticsearchTemplate;
import org.jeecg.common.util.DateUtils;
import org.jeecg.modules.demo.test.entity.CherryDevice;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 示例带参定时任务
 * 
 */
@Component
@Slf4j
public class SyncSensorBuz{
	private final static String ESIndex_DefaultType  = "doc";
	private final static String ESIndex_cherry_sensor_device = "cherry_sensor_device";
	private final static String ESIndex_cherry_sensor_slave = "cherry_sensor_slave";
	private final static String ESIndex_cherry_sensor_dataId = "cherry_sensor_dataId";
	private final static String ESIndex_cherry_sensor_data = "cherry_sensor_data";

	private final static String RedisSensorTokenKey = "chain.sensor.token";
	private final static String RedisSensorLockKey = "chain.sensor.lock";

	private final static String LoginUrl = "https://openapi.mp.usr.cn/usrCloud/user/login"; //登录
	private final static String GetDevsForVnUrl = "https://openapi.mp.usr.cn/usrCloud/vn/dev/getDevsForVn"; // 获取某个用户的设备列表
	private final static String GetDevsUrl = "https://openapi.mp.usr.cn/usrCloud/dev/getDevice"; // 获取设备详情
	private final static String GetDataPointInfoByDeviceUrl = "https://openapi.mp.usr.cn/usrCloud/datadic/getDataPointInfoByDevice"; //根据设备获取变量信息
	private final static String GetDeviceDataPointHistoryUrl = "https://openapi.mp.usr.cn/usrCloud/dev/getDeviceDataPointHistory"; //获取设备变量历史记录

	private final static SimpleDateFormat DateFormat= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	@Value("${usrCloud.userName}")
	private String userName;
	@Value("${usrCloud.password}")
	private String password;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RedissonLockClient redissonLock;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private JeecgElasticsearchTemplate esTemplate;

	public void syncBasicInfo()  {
		// 取得设备列表
		List<CherryDevice> devices = getDevsForVn();

		// 取得设备详情 和 slave 信息，并同步到es中
		Set<String> deviceIdList  = devices.stream().map(ele -> ele.getDevid()).collect(Collectors.toSet());
		deviceIdList.forEach( devId -> syncDevice(devId));

		// 取得设备详情 和 slave 信息，并同步到es中
		syncDataPoint(deviceIdList );
	}

	public void syncData()  {
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"bool\": {}\n" +
				"  },\n" +
				"  \"from\": 0,\n" +
				"  \"size\": 100,\n" +
				"  \"sort\": [\n" +
				"    { \"deviceId.keyword\"  : \"asc\" },\n" +
				"    { \"slaveIndex.keyword\": \"asc\" },\n" +
				"    { \"dataid\"            : \"asc\" }\n" +
				"  ],\n" +
				"  \"aggs\": {}\n" +
				"}";
		JSONObject queryObject = JSONObject.parseObject(query);
		JSONObject esResult = esTemplate.search(ESIndex_cherry_sensor_dataId, ESIndex_DefaultType,queryObject);
		JSONArray dataIds = (JSONArray) JSONPath.eval(esResult,"hits.hits._source");
		for( int i=0; i<dataIds.size();i++ ){
			JSONObject data = dataIds.getJSONObject(i);
			syncWithDataId(data);
		}
	}

	public Long getMaxTimeFromES(String deviceId, String slaveIndex, String dataPointId )  {
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"bool\": {\n" +
				"      \"must\": [\n" +
				"        {\n" +
				"          \"term\": {\n" +
				"            \"deviceId.keyword\": \"" + deviceId + "\"\n" +
				"          }\n" +
				"        },\n" +
				"        {\n" +
				"          \"term\": {\n" +
				"            \"slaveIndex.keyword\": \""+ slaveIndex +"\"\n" +
				"          }\n" +
				"        },\n" +
				"        {\n" +
				"          \"term\": {\n" +
				"            \"dataPointId.keyword\": \"" + dataPointId + "\"\n" +
				"          }\n" +
				"        }\n" +
				"      ],\n" +
				"      \"must_not\": [],\n" +
				"      \"should\": []\n" +
				"    }\n" +
				"  },\n" +
				"  \"from\": 0,\n" +
				"  \"size\": 0,\n" +
				"  \"sort\": [],\n" +
				"  \"aggs\": {\n" +
				"    \"max\": {\n" +
				"      \"max\": {\n" +
				"        \"field\": \"time\"\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		JSONObject queryObject = JSONObject.parseObject(query);
		JSONObject esResult = esTemplate.search(ESIndex_cherry_sensor_data, ESIndex_DefaultType,queryObject);
		Object maxTime = JSONPath.eval(esResult,"aggregations.max.value");

		Long result = 0l;
		if( null != maxTime){
			result = Double.valueOf(String.valueOf(maxTime)).longValue();
		}
		return result;

	}

	/**
	 *
	 *  同步数据
	 *
	 * @param dataPoint
	 */
	public void syncWithDataId(JSONObject dataPoint )  {
		String deviceId = dataPoint.getString("deviceId");
		String slaveIndex = dataPoint.getString("slaveIndex");
		String dataPointId = dataPoint.getString("dataid");
		String deviceName = dataPoint.getString("deviceName");
		String slaveName = dataPoint.getString("slaveName");
		String dataPointName = dataPoint.getString("name");

		int pageNo =1;
		int whileCount = Integer.MAX_VALUE;
		Long maxTime = getMaxTimeFromES(deviceId,slaveIndex,dataPointId);
		while ( whileCount >0) {
			whileCount =0;
			JSONObject param = new JSONObject();
			param.put("pageNo", pageNo);
			param.put("start", maxTime);
			param.put("end", System.currentTimeMillis());
			param.put("pageSize", 100);
			param.put("timeSort", "asc");
			JSONArray devDatapoints = new JSONArray();
			JSONObject dp = new JSONObject();
			dp.put("deviceNo", deviceId);
			dp.put("slaveIndex", slaveIndex);
			if (null != dataPointId) {
				dp.put("dataPointId", dataPointId);
			}
			devDatapoints.add(dp);
			param.put("devDatapoints", devDatapoints);

			HttpEntity httpEntity = new HttpEntity<>(param, getDefaultHeader(this.getToken()));
			JSONObject jsonObject = restTemplate.postForObject(GetDeviceDataPointHistoryUrl, httpEntity, JSONObject.class);

			JSONArray dataIds = jsonObject.getJSONObject("data").getJSONArray("list");
			if (null != dataIds && !dataIds.isEmpty()) {
				for (int m = 0; m < dataIds.size(); m++) {
					JSONObject dataInPoint = dataIds.getJSONObject(m);
					JSONArray detailDataList = dataInPoint.getJSONArray("list");
					for (int j = 0; j < detailDataList.size(); j++) {
						JSONObject dd = detailDataList.getJSONObject(j);
						dd.put("deviceId", deviceId);
						dd.put("slaveIndex", slaveIndex);
						dd.put("dataPointId", dataPointId);
						dd.put("deviceName", deviceName);
						dd.put("slaveName", slaveName);
						dd.put("dataPointname", dataPointName);
						Date date = new Date(dd.getLong("time"));
						dd.put("timeStr", DateFormat.format(date));
						String ddId = deviceId + "-" + slaveIndex + "-" + dataPointId + "-" + dd.getLong("time");
						log.info("ddId:" + ddId + ", " + dd);
						esTemplate.saveOrUpdate(ESIndex_cherry_sensor_data, ESIndex_DefaultType, ddId, dd);
						whileCount++;
					}
				}
			}
			pageNo++;
		}
	}

	private String getToken(){
		String token = stringRedisTemplate.opsForValue().get(RedisSensorTokenKey);
		if( null == token){
			if (redissonLock.tryLock(RedisSensorLockKey, -1)) {
				try {
					//有参 请求体中json参数 设置header头
					JSONObject param = new JSONObject();
					param.put("account", userName);
					param.put("password", password);
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					headers.set("Content-Type", "application/json");
					HttpEntity httpEntity = new HttpEntity<>(param, headers);
					JSONObject jsonObject = restTemplate.postForObject(LoginUrl, httpEntity, JSONObject.class);
					log.info(jsonObject.toJSONString());
					token = jsonObject.getJSONObject("data").getString("token");
					stringRedisTemplate.opsForValue().set(RedisSensorTokenKey, token, 2, TimeUnit.HOURS);
				}finally {
					redissonLock.unlock(RedisSensorLockKey);
				}
			} else {
				log.info("execute获取锁失败 ");
			}
		}
		log.info("有人云 token:" +  token );
		return token;
	}

	private HttpHeaders getDefaultHeader(String token){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Content-Type", "application/json");
		if( null != token) {
			headers.set("token", token);
		}
		return headers;
	}
	/**
	 *
	 * 获取 设备列表
	 *
	 * @param token
	 * @return
	 */
	private List<CherryDevice> getDevsForVn(){
		JSONObject param = new JSONObject();
		HttpEntity httpEntity = new HttpEntity<>(param, getDefaultHeader(this.getToken()));
		JSONObject jsonObject = restTemplate.postForObject(GetDevsForVnUrl, httpEntity, JSONObject.class);
		List<CherryDevice> devices = jsonObject.getJSONObject("data").getJSONArray("devices").toJavaList(CherryDevice.class);
		return devices;
	}

	/**
	 *
	 * 获取设备详情（slave）
	 *
	 * @param token
	 * @param devId
	 * @return
	 */
	private void syncDevice(String devId ){
		JSONObject param = new JSONObject();
		param.put("deviceId",devId);

		HttpEntity httpEntity = new HttpEntity<>(param, getDefaultHeader(this.getToken()));
		JSONObject jsonObject = restTemplate.postForObject(GetDevsUrl, httpEntity, JSONObject.class);

		JSONObject data = jsonObject.getJSONObject("data");
		// 设备详情
		JSONObject device = data.getJSONObject("device");
		esTemplate.saveOrUpdate(ESIndex_cherry_sensor_device,ESIndex_DefaultType, device.getString("deviceId") ,device);

		// 设备slave
		JSONArray slaves = data.getJSONArray("deviceSlaves");
		for(int i=0; i<slaves.size();i++){
			JSONObject slave = slaves.getJSONObject(i);
			String slaveId = slave.getString("deviceId") + "-" + slave.getString("slaveIndex");
			esTemplate.saveOrUpdate(ESIndex_cherry_sensor_slave,ESIndex_DefaultType, slaveId, slave);
		}
	}

	/**
	 *
	 * 获取设备数据点
	 *
	 * @param token
	 * @param deviceIdLists
	 * @return
	 */
	private void syncDataPoint(Set<String>  deviceIdLists){
		JSONObject param = new JSONObject();
		param.put("deviceIds",deviceIdLists);
		param.put("isGetCanWrite",1);
		param.put("isGetOperable",1);
		param.put("isGetNotCanWrite",1);
		param.put("isTimestampDatas",1);
		param.put("isGetLocationDatas",1);

		HttpEntity httpEntity = new HttpEntity<>(param, getDefaultHeader(this.getToken()));
		JSONObject jsonObject = restTemplate.postForObject(GetDataPointInfoByDeviceUrl, httpEntity, JSONObject.class);
		log.info(jsonObject.toJSONString());

		JSONArray deviceArray = jsonObject.getJSONArray("data");
		//遍历设备
		for(int n=0;n<deviceArray.size();n++){
			JSONObject device = deviceArray.getJSONObject(n);
			String deviceId = device.getString("deviceId");
			JSONArray slaveArray = device.getJSONArray("slaves");
			//遍历slave
			for(int l=0; l<slaveArray.size();l++){
				JSONObject slave = slaveArray.getJSONObject(l);
				String slaveIndex = slave.getString("slaveIndex");
				JSONArray iotDataDescriptionList = slave.getJSONArray("iotDataDescription");
				//遍历 iotDataDescription
				for(int m=0; m<iotDataDescriptionList.size();m++){
					JSONObject iotDataDescription = iotDataDescriptionList.getJSONObject(m);
					//iotModbusDataCmd
					JSONObject iotModbusDataCmd = iotDataDescription.getJSONObject("iotModbusDataCmd");
					iotDataDescription.putAll( iotModbusDataCmd );

					JSONObject deviceInES = esTemplate.getDataById(ESIndex_cherry_sensor_device,ESIndex_DefaultType,deviceId);
					iotDataDescription.put("deviceId", deviceId);
					iotDataDescription.put("deviceName", deviceInES.getString("name"));

					String slaveId = deviceId + "-" + slaveIndex;
					JSONObject slaveInES = esTemplate.getDataById(ESIndex_cherry_sensor_slave,ESIndex_DefaultType,slaveId);
					iotDataDescription.put("slaveId", slaveId);
					iotDataDescription.put("slaveIndex", slaveIndex);
					iotDataDescription.put("slaveName", slaveInES.getString("slaveName"));

					String dataId = iotModbusDataCmd.getString("dataid");
					String id = deviceId + "-" + slaveIndex + "-" + dataId;
					esTemplate.saveOrUpdate(ESIndex_cherry_sensor_dataId,ESIndex_DefaultType, id, iotDataDescription);
				}
			}
		}
	}
}
