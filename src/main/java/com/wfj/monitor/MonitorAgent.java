/**
 * @Probject Name: monitor_agent
 * @Path: com.wangfujing.monitorMonitorAgent.java
 * @Create By Jack
 * @Create In 2017年1月5日 上午11:09:20
 * TODO
 */
package com.wfj.monitor;

import com.wfj.monitor.common.SLACounter;
import com.wfj.monitor.transform.MonitoringTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * @Class Name MonitorAgent
 * @Author Jack
 * @Create In 2017年1月5日
 */
public class MonitorAgent {
	/**
	 * 监控启动方法，完成初始化动作
	 * 第一优先级
	 * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
	 * @Methods Name premain
	 * @Create In 2017年1月5日 By Jack
	 * @param args
	 * @param inst
	 *            void
	 */
	public static void premain(String args, Instrumentation inst) {
//        TODO read config from args
//        SLACounter.init();
        inst.addTransformer(new MonitoringTransformer());
	}
	
	/**
	 * 默认监控启动方法，完成初始化动作
	 * 第二优先级
	 * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
	 * @Methods Name premain
	 * @Create In 2017年1月5日 By Jack
	 * @param args void
	 */
	public static void premain(String args){
		
	}
	
	/**
	 * 动态代理注入启动方法，完成初始化动作
	 * 需配合线程类型主程序使用
	 * 使用非参数形式，如：-javaagent:xxxx=xxx配置JVM
	 * @Methods Name agentmain
	 * @Create In 2017年1月5日 By Jack
	 * @param agentArguments
	 * @param instrumentation
	 * @throws UnmodifiableClassException void
	 */
	public static void agentmain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException {

    }

}
