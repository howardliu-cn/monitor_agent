/**
 * @Probject Name: servlet-monitor-dev-sql
 * @Path: com.wfj.netty.servlet.handler.factoryKafkaConnectManager.java
 * @Create By Jack
 * @Create In 2016年4月6日 下午3:06:21
 * TODO
 */
package com.wfj.monitor.handler.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * kafka 工具类
 * @Class Name KafkaConnectManager
 * @Author Jack
 * @Create In 2016年4月6日
 */
public class KafkaConnectManager {
	private static Logger log = LoggerFactory.getLogger(KafkaConnectManager.class);


	/**
	 * 发送信息到 Kafka
	 * @Methods Name sendMsgToTopic
	 * @Create In 2016年4月6日 By Jack
	 * @param topic 指定的Topic
	 * @param key   消息的 Key
	 * @param msg   消息内容
	 * @return
	 */
	public static boolean sendMsgToTopic(String topic, final String key, final String msg) {
		return true;
	}
}
