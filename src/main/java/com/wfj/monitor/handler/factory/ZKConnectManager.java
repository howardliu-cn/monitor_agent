/**
 * @Probject Name: netty-wfj-monitor
 * @Path: com.wfj.netty.monitor.handlerZKConnectManager.java
 * @Create By Jack
 * @Create In 2015年11月9日 下午4:23:04
 * TODO
 */
package com.wfj.monitor.handler.factory;

import com.wfj.monitor.conf.EnvPropertyConfig;
import com.wfj.monitor.conf.SystemPropertyConfig;
import com.wfj.monitor.handler.Health;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Class Name ZKConnectManager
 * @Author Jack
 * @Create In 2015年11月9日
 */
public class ZKConnectManager {

	private static Logger log = LoggerFactory.getLogger(ZKConnectManager.class);

	private static final Integer ZK_RECONNECTION_MAX_COUNT = new Integer(5);

	private static final Integer ZK_MONITOR_ZKWHOLEDATE_TIMEOUT = new Integer(3600000);

	private static String ZK_MONITOR_HEARLTH_THREAD_NAME = "netty-wfj-server-monitor-hearth-thread";

	private static String ZK_MONITOR_ZKCONNECTION_THREAD_NAME = "netty-wfj-server-monitor-zkconnection-thread";

	private static String ZK_MONITOR_ZKWHOLEDATE_THREAD_NAME = "netty-wfj-server-monitor-zkwholedate-thread";

	private static Boolean isReconnection;

	private static Boolean isWholeDateCheck;

	private static AtomicInteger reconnectionCounts;

	private static Thread zkc;

	private static Thread zkw;

	private static ZooKeeper zk = null;

	private static CountDownLatch cdl = new CountDownLatch(1);

	/**
	 * @Return the CountDownLatch cdl
	 */
	public static CountDownLatch getCdl() {
		return cdl;
	}

	/**
	 * @Param CountDownLatch cdl to set
	 */
	public static void setCdl(CountDownLatch cdl) {
		ZKConnectManager.cdl = cdl;
	}

	/**
	 * @Return the Boolean isReconnection
	 */
	public static Boolean getIsReconnection() {
		return isReconnection;
	}

	/**
	 * @Param Boolean isReconnection to set
	 */
	public static void setIsReconnection(Boolean isReconnection) {
		ZKConnectManager.isReconnection = isReconnection;
	}

	/**
	 * @Return the Boolean isWholeDateCheck
	 */
	public static Boolean getIsWholeDateCheck() {
		return isWholeDateCheck;
	}

	/**
	 * @Param Boolean isWholeDateCheck to set
	 */
	public static void setIsWholeDateCheck(Boolean isWholeDateCheck) {
		ZKConnectManager.isWholeDateCheck = isWholeDateCheck;
	}

	/**
	 * zk 未连接监控，连续启动5次，时间间隔为 ZK 启动参数的超时时间 若已连接，则监控线程结束
	 * 
	 * @Methods Name zkConCheck
	 * @Create In 2015年9月11日 By Jack void
	 */
	private static void zkConCheck(final ZooKeeper zk, final Health hl) {

		isReconnection = new Boolean(true);

		if (zkc != null && zkc.isAlive())
			return;
		else if (zkc != null && zkc.isInterrupted()) {
			zkc.start();
		} else {
			zkc = new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						while (isReconnection) {
							// 1.判断是否超出最大重试次数
							Integer maxCountInteger = Integer.valueOf(SystemPropertyConfig
                                    .getContextProperty("system.seeting.monitor.reconnecte.count",
                                            String.valueOf(ZK_RECONNECTION_MAX_COUNT)));
							if (reconnectionCounts.get() >= maxCountInteger) {
								log.info("Zk Checker Service Was Great Max Reconnection Count, Stop It Now.....");
								// shutdownZK(zk, "Disable");
								wholeDateCheckerZKC(zk, hl);
							} else {
								// 2.重试次数+1
								reconnectionCounts.incrementAndGet();
								// 3.开始重连
								reconnection(zk, hl);
							}
							// 测试用5秒，正式env.setting.server.monitor.checker.sleeptime
							Thread.sleep(new Integer(EnvPropertyConfig
                                    .getContextProperty("env.setting.server.monitor.checker.sleeptime")).intValue());
						}
						log.debug("Zk Checker Service is Stop!");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001016"));
						log.error("Details: " + e.getMessage());
						Thread.currentThread().interrupt();
					}
				}
			}, ZKConnectManager.ZK_MONITOR_ZKCONNECTION_THREAD_NAME);

			zkc.start();
		}
	}

	/**
	 * 停止 zk 检查器，进入每天3次轮询检查，确保自身在断网超过一定时间后仍能链接
	 * 
	 * @Methods Name wholeDateCheckerZKC
	 * @Create In 2015年11月9日 By Jack
	 * @param zk
	 *            实例
	 * @param hl
	 *            实例
	 */
	private static void wholeDateCheckerZKC(final ZooKeeper zk, final Health hl) {
		isWholeDateCheck = new Boolean(true);
		reconnectionCounts = new AtomicInteger();

		if (zkw != null && zkw.isAlive())
			return;
		else if (zkw != null && zkw.isInterrupted()) {
			zkw.start();
		} else {
			zkw = new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						while (isWholeDateCheck) {
							// 1开始重连
							reconnection(zk, hl);
							// 2. 全天每个1小时检查一次。
							Thread.sleep(new Integer(EnvPropertyConfig
                                    .getContextProperty("env.setting.server.monitor.zkchk.sleeptime.wholedate"))
                                    .intValue(), ZK_MONITOR_ZKWHOLEDATE_TIMEOUT);
						}
						log.debug("Zk Checker Service is Stop!");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001016"));
						log.error("Details: " + e.getMessage());
						Thread.currentThread().interrupt();
					}
				}
			}, ZKConnectManager.ZK_MONITOR_ZKWHOLEDATE_THREAD_NAME);

			zkw.start();
		}
	}

	/**
	 * 建立 ZK 链接
	 * 
	 * @Methods Name creteZooKeeper
	 * @Create In 2015年11月9日 By Jack
	 * @param zk
	 * @param hl
	 * @return ZooKeeper
	 */
	public static ZooKeeper creteZooKeeper(Health hl) {
		isReconnection = new Boolean(false);
		isWholeDateCheck = new Boolean(false);
		reconnectionCounts = new AtomicInteger();

		try {

			if (zk != null) {
				return zk;
			} else {
				zk = new ZooKeeper(SystemPropertyConfig.getContextProperty("system.seeting.monitor.url"),
					new Integer(SystemPropertyConfig.getContextProperty("system.seeting.monitor.timeout")).intValue(), new RegisteredWatcher(hl), false);

				log.info("ZK Start Now........" + zk.getState().toString());
				cdl.await(new Integer(SystemPropertyConfig.getContextProperty("system.seeting.monitor.timeout")).intValue(), TimeUnit.MILLISECONDS);
				hl.setSessionId(zk.getSessionId());
				hl.setSessionPassword(zk.getSessionPasswd());

				if (zk.getState() != States.CONNECTED) {
					log.info("ZK Start Seting Close............. ");
					zk.close();
					zkConCheck(zk, hl);
				} else {
					log.info("Hearth Monitor ZK is Connected.......");
					isReconnection = new Boolean(false);
					isWholeDateCheck = new Boolean(false);
				}
			}

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001006"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		} catch (ConnectException e) {
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001010"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001007"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001010"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		}

		return zk;
	}

	/**
	 * 停止 ZK 服务
	 * 
	 * @Methods Name shutdownZK
	 * @Create In 2015年11月9日 By Jack
	 * @param zk
	 *            需要停止的 zk 实例
	 * @param status
	 *            状态描述
	 * @return boolean
	 */
	public static boolean shutdownZK(ZooKeeper zk, String status) {

		// 1.停止zk 链接监控线程
		if (zkc != null) {
			isReconnection = false;
		}
		// 2.停止实例信息获取模块
		if (zkw != null) {
			isWholeDateCheck = false;
		}

		// 3.重置监控状态
		isReconnection = new Boolean(false);
		isWholeDateCheck = new Boolean(false);
		reconnectionCounts = new AtomicInteger();

		// 4.销毁服务监控服务
		try {
			if (zk != null && zk.getState() != States.CLOSED) {
				zk.close();
				zk = null;
			} else {
				zk = null;
			}
			log.info("Health Monitor All of Stoped!");
			return true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001017"));
			log.error("Details: " + e.getMessage());
			return false;
		}

	}

	/**
	 * 重新链接 ZK
	 * 
	 * @Methods Name reconnection
	 * @Create In 2015年11月9日 By Jack
	 * @param zk
	 *            zk 原实例
	 * @param hl
	 *            健康在检查实例
	 * @return boolean
	 */
	public static boolean reconnection(ZooKeeper zk, Health hl) {

		try {
			int timeout = new Integer(SystemPropertyConfig.getContextProperty("system.seeting.monitor.timeout")).intValue();
			cdl = new CountDownLatch(1);

			if (zk != null && zk.getState().isConnected()) {
				return true;
			} else if (zk != null && zk.getState().isAlive()) {
				zk = new ZooKeeper(SystemPropertyConfig.getContextProperty("system.seeting.monitor.url"), timeout, new RegisteredWatcher(hl), hl.getSessionId(), hl.getSessionPassword());
			} else if (zk != null && zk.getState() == States.CLOSED) {
				zk = new ZooKeeper(SystemPropertyConfig.getContextProperty("system.seeting.monitor.url"), timeout, new RegisteredWatcher(hl));
			} else {
				zk = new ZooKeeper(SystemPropertyConfig.getContextProperty("system.seeting.monitor.url"), timeout, new RegisteredWatcher(hl));
			}

			cdl.await(timeout, TimeUnit.MILLISECONDS);
			hl.setZk(zk);
			hl.setSessionId(zk.getSessionId());
			hl.setSessionPassword(zk.getSessionPasswd());

			log.info("ZK Restate(" + zk.getSessionId() + ") is(" + reconnectionCounts + "): " + zk.getState().toString());

			if (zk.getState() != States.CONNECTED) {
				log.info("ZK Restate Failer Start Checkers............. ");
				zk.close();
				if (isWholeDateCheck) {
					isReconnection = new Boolean(false);
					if (zkw != null && zkw.isAlive()) {
						return false;
					} else if (zkw != null && zkw.isInterrupted()) {
						zkw.start();
						return false;
					} else {
						wholeDateCheckerZKC(zk, hl);
						return false;
					}
				} else {
					isReconnection = new Boolean(true);
					if (zkc != null && zkc.isAlive()) {
						return false;
					} else if (zkc != null && zkc.isInterrupted()) {
						zkc.start();
						return false;
					} else {
						zkConCheck(zk, hl);
						return false;
					}
				}

			} else {
				log.info("Hearth Monitor ZK is Reconnected.......");
				isReconnection = new Boolean(false);
				isWholeDateCheck = new Boolean(false);
				if (hl.getM() != null) {
					if (!hl.getM().isAlive() && hl.getM().isInterrupted()) {
						hl.setIsMonitorStopBoolean(false);
						hl.getM().start();
					} else {
						hl.startHealth("Reconnected");
					}
				} else {
					hl.startHealth("Reconnected");
				}
				return true;
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001009"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001006"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001007"));
			log.error("Details: " + e.getMessage());
			zkConCheck(zk, hl);
		}

		return false;
	}
}
