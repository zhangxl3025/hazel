package com.zxl.hazel.web.netty;

import com.zxl.hazel.web.WebServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyWebServer implements WebServer {

    private static final Logger log = LoggerFactory.getLogger(NettyWebServer.class);
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel channel;

    private int port;
    private boolean running;

    @Override
    public void start(int port) {
        this.port = port;

        // 在独立线程中启动 Netty，避免阻塞主线程
        // 等待服务器关闭
        Thread nettyThread = new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new HttpServerCodec());
                                ch.pipeline().addLast(new HttpObjectAggregator(65536));
                                ch.pipeline().addLast(new NettyDispatcherHandler());
                            }
                        });

                channel = bootstrap.bind(port).sync().channel();
                running = true;
                log.info("Netty WebServer started on port {}", port);

                // 等待服务器关闭
                channel.closeFuture().sync();

            } catch (InterruptedException e) {
                log.info("Netty server interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Failed to start Netty server", e);
            } finally {
                stop();
            }
        });

        nettyThread.setName("netty-server");
        nettyThread.setDaemon(false);
        nettyThread.start();

        // 等待服务器启动完成
        waitForStartup();
    }

    private void waitForStartup() {
        int retries = 50; // 最多等待5秒
        while (!running && retries-- > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!running) {
            log.warn("Netty server may not have started properly");
        }
    }

    @Override
    public void registerRoutes() {
        // NettyDispatcherHandler 构造函数中已经注册路由，这里不需要重复注册
        // 保留方法只是为了实现接口
    }

    @Override
    public void stop() {
        running = false;

        if (channel != null) {
            try {
                channel.close().sync();
            } catch (InterruptedException e) {
                log.warn("Interrupted while closing channel", e);
                Thread.currentThread().interrupt();
            }
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        log.info("Netty WebServer stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPort() {
        return port;
    }
}