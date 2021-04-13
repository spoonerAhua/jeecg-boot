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
import org.springframework.data.web.JsonPath;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 示例带参定时任务
 * 
 */
@Slf4j
public class SyncSensorDataJob implements Job {
	/**
	 * 若参数变量名修改 QuartzJobController中也需对应修改
	 */
	private String parameter;

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	@Autowired
	private SyncSensorBuz syncSensorBuz;

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		log.info(String.format("同步 有人云 组态 数据......   时间:" + DateUtils.now(), this.parameter));
		syncSensorBuz.syncData();
	}
}
