package org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.module;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.base.MediaType;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.handler.RtspChannelHandler;
import org.kkukie.jrtsp_gw.media.core.util.WebSocketPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class NettyChannel
 * @brief NettyChannel class
 */
public class RtspNettyChannel { // > TCP

    private static final Logger logger = LoggerFactory.getLogger(RtspNettyChannel.class);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final String listenIp;
    private final int listenPort;
    /* Streamer Map */
    /* Key: To MDN, value: Streamer */
    private final HashMap<String, Streamer> streamerMap = new HashMap<>();
    private final ReentrantLock streamerMapLock = new ReentrantLock();
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private ServerBootstrap b;
    /*메시지 수신용 채널 */
    private Channel serverChannel;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspNettyChannel(String ip, int port) {
        this.listenIp = ip;
        this.listenPort = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void run(String ip, int port) {
        DefaultConfig defaultConfig = ConfigManager.getDefaultConfig();
        int recvBufSize = defaultConfig.getRecvBufSize();

        bossGroup = new NioEventLoopGroup();
        b = new ServerBootstrap();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_RCVBUF, recvBufSize)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new RtspDecoder(), new RtspEncoder());
                        pipeline.addLast(
                                new RtspChannelHandler(ip, port)
                        );
                    }
                });
    }

    /**
     * @fn public void stop()
     * @brief Netty Channel 을 종료하는 함수
     */
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ip   바인딩할 ip
     * @param port 바인당할 port
     * @return 성공 시 생성된 Channel, 실패 시 null 반환
     * @fn public Channel openChannel(String ip, int port)
     * @brief Netty Server Channel 을 생성하는 함수
     */
    public Channel openChannel(String ip, int port) {
        if (serverChannel != null) {
            logger.warn("Channel is already opened.");
            return null;
        }

        InetAddress address;
        ChannelFuture channelFuture;

        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            logger.warn("UnknownHostException is occurred. (ip={})", ip, e);
            return null;
        }

        try {
            channelFuture = b.bind(address, port).sync();
            serverChannel = channelFuture.channel();
            logger.debug("Channel is opened. (ip={}, port={})", address, port);

            return channelFuture.channel();
        } catch (Exception e) {
            logger.warn("Channel is interrupted. (address={}:{})", ip, port, e);
            return serverChannel;
        }
    }

    /**
     * @fn public void closeChannel()
     * @brief Netty Server Channel 을 닫는 함수
     */
    public void closeChannel() {
        if (serverChannel == null) {
            logger.warn("Channel is already closed.");
            return;
        }

        serverChannel.close();
        serverChannel = null;
        logger.debug("Channel is closed.");
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }


    ////////////////////////////////////////////////////////////////////////////////

    public Streamer addStreamer(MediaType mediaType, String conferenceId, String sessionId, String trackId, boolean isTcp) {
        String key = (trackId != null && !trackId.isEmpty()) ? conferenceId + ":" + trackId + ":" + sessionId : conferenceId + ":" + sessionId;
        if (getStreamer(key) != null) {
            logger.warn("Streamer is already exist. (key={})", sessionId);
            return null;
        }

        streamerMapLock.lock();
        try {
            Streamer streamer = new Streamer(
                    mediaType,
                    conferenceId,
                    sessionId,
                    trackId,
                    isTcp,
                    listenIp, WebSocketPortManager.getInstance().takePort()
            );
            streamerMap.putIfAbsent(streamer.getKey(), streamer);
            return streamer;
        } catch (Exception e) {
            logger.warn("Fail to add streamer (conferenceId={}, sessionId={}, trackId={})", conferenceId, sessionId, trackId, e);
            return null;
        } finally {
            streamerMapLock.unlock();
        }
    }

    public void deleteStreamer(String key) {
        Streamer streamer = getStreamer(key);
        if (streamer == null) {
            //logger.warn("Streamer is null. Fail to delete the Streamer. (key={})", key);
            return;
        }
        streamer.close();

        streamerMapLock.lock();
        try {
            streamerMap.remove(key);
        } catch (Exception e) {
            logger.warn("Fail to delete the Streamer. (key={})", key, e);
        } finally {
            streamerMapLock.unlock();
            if (streamerMap.get(key) == null) {
                logger.debug("Streamer is deleted. (key={})", key);
            }
        }
    }

    public void deleteAllStreamers() {
        streamerMapLock.lock();
        try {
            for (Map.Entry<String, Streamer> entry : getCloneStreamerMap().entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }

                Streamer streamer = entry.getValue();
                if (streamer == null) {
                    continue;
                }

                streamer.close();
                streamerMap.remove(key);
            }
        } catch (Exception e) {
            logger.warn("Fail to delete all the Streamers.", e);
        } finally {
            streamerMapLock.unlock();
        }
    }

    public Map<String, Streamer> getCloneStreamerMap() {
        if (streamerMap.isEmpty()) {
            return Collections.emptyMap();
        }

        HashMap<String, Streamer> cloneMap;

        streamerMapLock.lock();
        try {
            cloneMap = (HashMap<String, Streamer>) streamerMap.clone();
        } catch (Exception e) {
            logger.warn("Fail to clone the message sender map.");
            cloneMap = streamerMap;
        } finally {
            streamerMapLock.unlock();
        }

        return cloneMap;
    }

    /**
     * @param key Streamer key
     * @return 성공 시 Streamer 객체, 실패 시 null 반환
     * @fn public Streamer getStreamer (String key)
     * @brief 지정한 key 에 해당하는 Streamer 를 반환하는 함수
     */
    public Streamer getStreamer(String key) {
        return streamerMap.get(key);
    }

    public void startStreaming(String key) {
        Streamer streamer = getStreamer(key);
        if (streamer == null) {
            logger.warn("Fail to start the streaming. (key={})", key);
            return;
        }

        streamer.open();
        streamer.start();
    }

    public void stopStreaming(String key) {
        Streamer streamer = getStreamer(key);
        if (streamer == null) {
            //logger.warn("Fail to stop the streaming. (key={})", key);
            return;
        }

        streamer.stop();
    }

}
