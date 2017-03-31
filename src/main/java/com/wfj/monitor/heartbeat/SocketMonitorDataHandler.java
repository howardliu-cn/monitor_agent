package com.wfj.monitor.heartbeat;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <br>created at 17-3-31
 *
 * @author liuxh
 * @since 1.0.0
 */
public class SocketMonitorDataHandler extends AbstractMonitorDataHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Channel channel;
    private Bootstrap bootstrap;
    private NioEventLoopGroup workGroup = new NioEventLoopGroup(4);

    private String ip;
    private int port;
    private int reconnectDelaySeconds = super.retryDelaySeconds;
    private boolean linked = false;

    public SocketMonitorDataHandler(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void start() {
        bootstrap = new Bootstrap();
        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                             @Override
                             protected void initChannel(SocketChannel ch) throws Exception {
                                 ch.pipeline()
                                         .addLast(
                                                 new IdleStateHandler(0, 0, timeout),
                                                 new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0),
                                                 new CustomHeartbeatHandler(getName(), SocketMonitorDataHandler.this)
                                         );
                             }
                         }
                );
        connect();
    }

    protected void connect() {
        if (this.channel != null && this.channel.isActive()) {
            return;
        }
        bootstrap.connect(this.ip, port)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Connect to server [" + ip + ":" + port + "] successfully!");
                            }
                            channel = future.channel();
                            linked = true;
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "Failed to connect to server [" + ip + ":" + port + "], try connect after " + reconnectDelaySeconds + "s");
                            }
                            linked = false;
                            future.channel().eventLoop().schedule(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            connect();
                                        }
                                    },
                                    reconnectDelaySeconds,
                                    TimeUnit.SECONDS
                            );
                        }
                    }
                });
    }

    @Override
    public void handleException(Throwable cause, byte[] bytes) {
        // do nothing
    }

    @Override
    public boolean isActive() {
        return this.channel != null && this.channel.isActive() && linked;
    }

    @Override
    public void sendData(byte[] bytes) {
        try {
            ByteBuf buf = this.channel.alloc().buffer(1 + bytes.length);
            buf.writeByte(HeartbeatHandler.CUSTOM_MSG);
            buf.writeBytes(bytes);
            channel.writeAndFlush(buf);
        } catch (Throwable t) {
            handleException(t, bytes);
        }
    }

    @Override
    public void handleData(byte[] bytes) {
        try {
            String content = new String(bytes, "UTF-8");
            logger.debug("got one message: " + content);
        } catch (UnsupportedEncodingException ignored) {
        } catch (Exception e) {
            logger.warn("cannot handle data, abort it");
        }
    }

    @Override
    public String getName() {
        return super.getName() == null ? UUID.randomUUID().toString() : super.getName();
    }
}
