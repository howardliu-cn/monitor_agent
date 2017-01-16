/**
 * @Probject Name: monitor_agent
 * @Path: com.wfj.monitor.handler.factoryMonitorLoggerFactory.java
 * @Create By Jack
 * @Create In 2017年1月16日 下午3:19:40
 * TODO
 */
package com.wfj.monitor.handler.factory;

import com.wfj.monitor.handler.dto.MonitorLogger;

/**
 * @Class Name MonitorLoggerFactory
 * @Author Jack
 * @Create In 2017年1月16日
 */
public class MonitorLoggerFactory {
	
	public static MonitorLogger getLogger(Class clazz){
		return new MonitorLogger(clazz);
	}

	public static MonitorLogger getLogger(String clazzName){
		return new MonitorLogger(clazzName);
	}
}
