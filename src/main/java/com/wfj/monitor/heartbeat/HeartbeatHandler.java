package com.wfj.monitor.heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wfj.monitor.heartbeat.HeartbeatConstants.HEADER_LENGTH;
import static com.wfj.monitor.heartbeat.HeartbeatConstants.HEARTBEAT_COUNTER;

/**
 * <br>created at 17-3-30
 *
 * @author liuxh
 * @since 1.0.0
 */
public class HeartbeatHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final byte PING_MSG = 1;
    public static final byte PONG_MSG = 2;
    public static final byte CUSTOM_MSG = 3;

    protected String name;
    protected IMonitorDataHandler handler;

    public HeartbeatHandler(String name, IMonitorDataHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (byteBuf.getByte(HEADER_LENGTH) == PING_MSG) {
            // handle PING single message
            pong(ctx);
        } else if (byteBuf.getByte(HEADER_LENGTH) == PONG_MSG) {
            // handle PONG single message
            if (logger.isDebugEnabled()) {
                logger.debug(name + " get PONG single message from " + ctx.channel().remoteAddress());
            }
        } else if (byteBuf.getByte(HEADER_LENGTH) == CUSTOM_MSG) {
            handleData(ctx, byteBuf);
        } else {
            abortData(ctx, byteBuf);
        }
    }

    protected void ping(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(HEADER_LENGTH + 1);
        buf.writeInt(5);
        buf.writeByte(PING_MSG);
        buf.retain();
        ctx.writeAndFlush(buf);
        long count = HEARTBEAT_COUNTER.incrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.debug(name + " send PING single message to " + ctx.channel().remoteAddress() + ", count: " + count);
        }
    }

    protected void pong(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(HEADER_LENGTH + 1);
        buf.writeInt(5);
        buf.writeByte(PONG_MSG);
        ctx.channel().writeAndFlush(buf);
        long count = HEARTBEAT_COUNTER.incrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.debug(name + " send PONG single message to " + ctx.channel().remoteAddress() + ", count: " + count);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        super.userEventTriggered(ctx, event);
        if (event instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) event;
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
            }
        }
    }

    protected void handleData(ChannelHandlerContext ctx, ByteBuf buf) {
        try {
            byte[] data = new byte[buf.readableBytes() - HEADER_LENGTH - 1];
            buf.skipBytes(HEADER_LENGTH + 1);
            buf.readBytes(data);
            handler.handleData(data);
        } catch (Exception e) {
            logger.warn("cannot handle message, must be wrong format message");
        }
    }

    protected void abortData(ChannelHandlerContext ctx, ByteBuf buf) {
        // do nothing
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        logger.warn("READER IDLE");
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.warn("WRITER IDLE");
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        logger.warn("ALL IDLE");
    }
}
