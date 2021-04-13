package org.jeecg.modules.demo.test.entity;

import lombok.Data;

@Data
public class CherryDevice{
    private int id;
    private String devid;
    private String name;
    private int isBindAlarmConfig;
    private boolean smokeProtocolDevice;
}
