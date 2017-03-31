package com.wfj.monitor.heartbeat;

/**
 * <br>created at 17-3-31
 *
 * @author liuxh
 * @since 1.0.0
 */
public interface IMonitorDataHandler {
    void start();

    void sendData(byte[] bytes);

    void handleData(byte[] bytes);
}
