package org.jeecg.modules.demo.job;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.jeecg.common.util.DateUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 示例带参定时任务
 * 
 */
@Slf4j
public class SyncSensor11111Job implements Job {

	private final static String LoginUrl = "https://openapi.mp.usr.cn/usrCloud/user/login";
	private final static String GetDevsForVn_Url = "https://openapi.mp.usr.cn/usrCloud/vn/dev/getDevsForVn";
	private final static String GetDataPointInfoByDeviceUrl = "https://openapi.mp.usr.cn/usrCloud/datadic/getDataPointInfoByDevice";

	private String userName="18269690309";
	private String password="0c534ed3fff7d2bfd32ee19d84644e3f";

	/**
	 * 若参数变量名修改 QuartzJobController中也需对应修改
	 */
	private String parameter;

	@Autowired
	private RestTemplate restTemplate;

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		log.info(String.format("welcome %s! Jeecg-Boot 带参数定时任务 SampleParamJob !   时间:" + DateUtils.now(), this.parameter));

	}

	private String getToken()  throws URISyntaxException{
        //有参 请求体中json参数 设置header头
		JSONObject param = new JSONObject();
		param.put("account","18269690309");
		param.put("password","0c534ed3fff7d2bfd32ee19d84644e3f");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Content-Type", "application/json");
		HttpEntity httpEntity = new HttpEntity<>(param, headers);
		JSONObject jsonObject = restTemplate.postForObject(LoginUrl, httpEntity, JSONObject.class);



		log.info(  jsonObject.toJSONString() );



		return  null;
	}



}

