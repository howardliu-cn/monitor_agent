package com.wfj.monitor.heartbeat;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>created at 17-3-31
 *
 * @author liuxh
 * @since 1.0.0
 */
public class CustomHeartbeatHandler extends HeartbeatHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private SocketMonitorDataHandler handler;

    public CustomHeartbeatHandler(String name, SocketMonitorDataHandler handler) {
        super(name, handler);
        this.handler = handler;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        // reconnect
        this.handler.connect();
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        logger.warn("READER IDLE");
        handlerIdle(ctx);
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.warn("WRITER IDLE");
        handlerIdle(ctx);
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        logger.warn("ALL IDLE");
        handlerIdle(ctx);
    }

    private void handlerIdle(ChannelHandlerContext ctx) {
        ping(ctx);
    }
}
