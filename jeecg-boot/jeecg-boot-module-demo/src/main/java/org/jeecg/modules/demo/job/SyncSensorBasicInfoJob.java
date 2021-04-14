package org.jeecg.modules.demo.job;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.DateUtils;
import org.jeecg.modules.demo.buz.SyncSensorBuz;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 示例带参定时任务
 * 
 */
@Slf4j
public class SyncSensorBasicInfoJob implements Job {
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
		log.info(String.format("同步 有人云 组态 基本信息......   时间:" + DateUtils.now(), this.parameter));
		syncSensorBuz.syncBasicInfo();
	}
}
