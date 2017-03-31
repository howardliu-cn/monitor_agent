package com.wfj.monitor.heartbeat;

/**
 * <br>created at 17-3-31
 *
 * @author liuxh
 * @since 1.0.0
 */
public abstract class AbstractMonitorDataHandler implements IMonitorDataHandler {
    protected String name;
    protected int retryDelaySeconds = 10;
    protected int timeout = 5;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public abstract void handleException(Throwable cause, byte[] bytes);

    public abstract boolean isActive();
}
