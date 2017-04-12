/**
 * @Probject Name: netty-wfj-base-dev
 * @Path: com.wfj.netty.monitorRegisteredWatcher.java
 * @Create By Jack
 * @Create In 2015年8月25日 上午10:05:57
 * TODO
 */
package com.wfj.monitor.handler.factory;

import com.wfj.monitor.handler.Health;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Class Name RegisteredWatcher
 * @Author Jack
 * @Create In 2015年8月25日
 */
public class RegisteredWatcher implements Watcher {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private Health hl;
	
	public RegisteredWatcher(Health target){
		hl = target;
	}

	/* (non-Javadoc)
	 * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent)
	 */
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		if(KeeperState.SyncConnected == event.getState()){
			log.info("ZK Watcher Connectioned........" );
			ZKConnectManager.getCdl().countDown();
			ZKConnectManager.setIsReconnection(false);
			ZKConnectManager.setIsWholeDateCheck(false);
		}else if(KeeperState.AuthFailed == event.getState()){
			log.info("ZK Watcher AuthFailed........" );
			hl.shutdownHealth("Disconnection");
		}else if(KeeperState.ConnectedReadOnly == event.getState()){
			log.info("ZK Watcher ConnectedReadOnly........" );
		}else if(KeeperState.Disconnected == event.getState()){
			log.info("ZK Watcher Disconnected........" );
			setReconnectStatus();
		}else if(KeeperState.Expired == event.getState()){
			log.info("ZK Watcher Expired........" );
			setReconnectStatus();
		}else if(KeeperState.NoSyncConnected == event.getState()){
			log.info("ZK Watcher NoSyncConnected........" );
			ZKConnectManager.getCdl().countDown();
			ZKConnectManager.setIsReconnection(false);
			ZKConnectManager.setIsWholeDateCheck(false);
		}else if(KeeperState.SaslAuthenticated == event.getState()){
			log.info("ZK Watcher SaslAuthenticated........" );
			hl.shutdownHealth("Disconnection");
		}else if(KeeperState.Unknown == event.getState()){
			log.info("ZK Watcher Unknown........" );
			hl.shutdownHealth("Disconnection");
		}
	}

	/**
	 * 设置重连状态
	 * @Methods Name setReconnectStatus
	 * @Create In 2015年9月7日 By Jack void
	 */
	private void setReconnectStatus() {
			//设置重连开始
			//1.先停止所有，这种 ZK 断开属于异常状态，故此必须断开
			hl.shutdownHealth("Disconnection");
			//2.再次重新链接
			hl.restartHealth("Reconnection");
	}

}
