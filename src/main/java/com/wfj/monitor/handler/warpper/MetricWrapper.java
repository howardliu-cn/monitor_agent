/**
 * @Probject Name: monitor_agent
 * @Path: com.wfj.monitor.handler.warpperMetricWrapper.java
 * @Create By Jack
 * @Create In 2017年1月16日 下午5:12:35
 * TODO
 */
package com.wfj.monitor.handler.warpper;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes.Name;

import org.slf4j.impl.StaticLoggerBinder;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * @Class Name MetricWrapper
 * @Author Jack
 * @Create In 2017年1月16日
 */
public class MetricWrapper {

	private static MetricWrapper slam = null;
	
	private final MetricRegistry registry = new MetricRegistry();
	
	private final static String METRIC_COUNTER_PATH = "com.wfj.monitor";
	
	//记录总入栈请求书
	private Counter sumInboundRequestCounts;
	//记录总出站请求数
	private Counter sumOutboundRequestCounts;
	//记录已成功处理完成的总请求数量
	private Counter sumDealRequestCounts;
	//记录处理错误的总请求数
	private Counter sumErrDealRequestCounts;
	//记录总处理错误的时间
	private Counter sumErrDealRequestTime;
	//记录总成功处理的请求时间
	private Counter sumDealRequestTime;
	//是否调试模式的开关，默认是开启的，不进行统计
	private AtomicBoolean isDebug;
	//当前处理时间
	private Counter peerDealRequestTime;
	
	//清收汇总数据，包括：次数、最大时间、最小时间、平均时间、处理速率等
	private Timer requestDetails;
	
	//每天的时间记录，用于判断清零
	private Date peerDate;

	public static void inital(){
		if(slam == null)
			instance();
		slam.clearnMetricRegistry();
		slam.buildMetricRegistry();
	}
	
	public static MetricWrapper instance(){
		if(slam == null)
			slam = new MetricWrapper();
		return slam;
	}
	
	/**
	 * 清理计数器所有内容
	 * @Methods Name clearnMetricRegistry
	 * @Create In 2017年1月16日 By Jack void
	 */
	private void clearnMetricRegistry(){
		Iterator<String> keys = slam.registry.getNames().iterator();
		while(keys.hasNext()){
			String item = keys.next();
			slam.registry.remove(item);
		}
	}
	
	/**
	 * 建立空的计量器，每天处理一次
	 * @Methods Name buildMetricRegistry
	 * @Create In 2017年1月16日 By Jack void
	 */
	private void buildMetricRegistry(){
		sumInboundRequestCounts = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "counts", "sum","inbound"));

		sumOutboundRequestCounts = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "counts", "sum","outbound"));
		
		sumDealRequestCounts = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "counts", "sum","deal"));
		
		sumErrDealRequestCounts = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "counts", "sum","error"));
		
		sumErrDealRequestTime = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "time", "sum","error"));
		
		sumDealRequestTime = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "time", "sum","deal"));
		
		peerDealRequestTime = slam.registry.counter(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "time", "sum","peer"));
		
		requestDetails = slam.registry.timer(MetricRegistry.name(MetricWrapper.METRIC_COUNTER_PATH, "request", "summer","request"));
	}
}
