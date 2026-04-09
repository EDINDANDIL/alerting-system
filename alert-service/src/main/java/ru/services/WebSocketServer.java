package ru.services;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.config.AlertConfig;
import ru.tinkoff.kora.common.Component;

@Component
public final class WebSocketServer implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final int DEFAULT_WS_PORT = 7818; // TODO плохо, поменяй

    private final ConnectionManager connectionManager;
    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(ConnectionManager connectionManager, AlertConfig config) throws Exception {
        this.connectionManager = connectionManager;
        this.port = config.wsPort() != null ? config.wsPort() : DEFAULT_WS_PORT;
        log.info("Initializing WebSocketServer on port {}", this.port);
        start();
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    public synchronized void start() throws InterruptedException {
        if (serverChannel != null && serverChannel.isActive()) {
            return;
        }

        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());


        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(65_536))
                                .addLast(new WebSocketServerProtocolHandler(
                                        "/ws",
                                        null,
                                        true,
                                        65536,
                                        true,
                                        true,
                                        false,
                                        10000L
                                ))
                                .addLast(new WebSocketHandler(connectionManager));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // TODO вот тут посмотри по документации

        serverChannel = bootstrap.bind(port).sync().channel();
        log.info("WebSocket server started on port {} (path=/ws)", port);
    }

    public synchronized void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
            }
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
            serverChannel = null;
            bossGroup = null;
            workerGroup = null;
        }
    }
}
