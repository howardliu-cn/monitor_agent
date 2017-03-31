package com.wfj.monitor.heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wfj.monitor.heartbeat.HeartbeatConstants.HEADER_LENGTH;

/**
 * <br>created at 17-3-31
 *
 * @author liuxh
 * @since 1.0.0
 */
public class ServerHandler extends HeartbeatHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public ServerHandler() {
        super("server", new IMonitorDataHandler() {
            @Override
            public void start() {
                System.out.println("start");
            }

            @Override
            public void sendData(byte[] bytes) {
                System.out.println("sendData: " + new String(bytes));
            }

            @Override
            public void handleData(byte[] bytes) {
                System.out.println("handleData: " + new String(bytes));
            }
        });
    }

    @Override
    protected void handleData(ChannelHandlerContext ctx, ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes() - HEADER_LENGTH - 1];
        ByteBuf responseBuf = Unpooled.copiedBuffer(buf);
        buf.skipBytes(HEADER_LENGTH - 1);
        buf.readBytes(data);
        String content = new String(data);
        System.out.println(name + " get content: " + content);
        ctx.write(responseBuf);
    }

    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
        System.err.println("---client " + ctx.channel().remoteAddress().toString() + " reader timeout, ping it---");
        ping(ctx);
    }
}
