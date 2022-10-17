package org.kkukie.jrtsp_gw.media.rtsp.netty;

import io.netty.channel.Channel;
import org.kkukie.jrtsp_gw.media.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.rtsp.base.MediaType;
import org.kkukie.jrtsp_gw.media.rtsp.netty.module.RtcpNettyChannel;
import org.kkukie.jrtsp_gw.media.rtsp.netty.module.RtspNettyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @class public class NettyChannelManager
 * @brief Netty channel manager 클래스
 * RTP Netty Channel 을 관리한다.
 */
public class NettyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelManager.class);

    private static NettyChannelManager manager = null;
    private final HashMap<String, RtcpNettyChannel> rtcpChannelMap = new HashMap<>();
    private final ReentrantLock rtcpChannelMapLock = new ReentrantLock();
    private RtspNettyChannel rtspNettyChannel = null;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private NettyChannelManager ()
     * @brief NettyChannelManager 생성자 함수
     */
    private NettyChannelManager() {
        // Nothing
    }

    /**
     * @return 최초 호출 시 새로운 NettyChannelManager 전역 변수, 이후 모든 호출에서 항상 이전에 생성된 변수 반환
     * @fn public static NettyChannelManager getInstance ()
     * @brief NettyChannelManager 싱글턴 변수를 반환하는 함수
     */
    public static NettyChannelManager getInstance() {
        if (manager == null) {
            manager = new NettyChannelManager();

        }
        return manager;
    }

    public void stop() {
        deleteRtspChannel();
        deleteAllRtcpChannels();
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void openRtspChannel(String ip, int port) {
        rtspNettyChannel = new RtspNettyChannel(ip, port);
        rtspNettyChannel.run(ip, port);

        // 메시지 수신용 채널 open
        Channel channel = rtspNettyChannel.openChannel(
                ip,
                port
        );

        if (channel == null) {
            rtspNettyChannel.closeChannel();
            rtspNettyChannel.stop();
            logger.warn("| Fail to add the channel. (ip={}, port={})", ip, port);
            return;
        }

        logger.debug("| ({}) Success to add channel.", rtspNettyChannel);
    }

    public void deleteRtspChannel() {
        if (rtspNettyChannel != null) {
            rtspNettyChannel.deleteAllStreamers();
            rtspNettyChannel.closeChannel();
            rtspNettyChannel.stop();
            logger.debug("| Success to close the channel.");
        }
    }

    public RtspNettyChannel getRtspChannel() {
        return rtspNettyChannel;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpNettyChannel openRtcpChannel(String streamerKey, String ip, int port) {
        try {
            rtcpChannelMapLock.lock();

            if (rtcpChannelMap.get(streamerKey) != null) {
                logger.trace("| ({}) Fail to add the rtcp channel. Key is duplicated.", streamerKey);
                return null;
            }

            /*int port = WebSocketPortManager.getInstance().takePort();
            if (port == -1) {
                logger.warn("| Fail to add the channel. Port is full. (key={})", key);
                return false;
            }*/

            RtcpNettyChannel rtcpNettyChannel = new RtcpNettyChannel(streamerKey, ip, port);
            rtcpNettyChannel.run(ip, port);

            // 메시지 수신용 채널 open
            Channel channel = rtcpNettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                logger.warn("| ({}) Fail to add the rtcp channel.", streamerKey);
                return null;
            }

            rtcpChannelMap.putIfAbsent(streamerKey, rtcpNettyChannel);
            logger.debug("| ({}) Success to add rtcp channel.", streamerKey);
            return rtcpNettyChannel;
        } catch (Exception e) {
            logger.warn("| ({}) Fail to add rtcp channel (ip={}, port={}).", streamerKey, ip, port, e);
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteRtcpChannel(String streamerKey) {
        try {
            rtcpChannelMapLock.lock();

            if (!rtcpChannelMap.isEmpty()) {
                RtcpNettyChannel rtcpNettyChannel = rtcpChannelMap.get(streamerKey);
                if (rtcpNettyChannel == null) {
                    return;
                }

                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                rtcpChannelMap.remove(streamerKey);

                logger.debug("| ({}) Success to close the rtcp channel.", streamerKey);
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to close the rtcp channel.", streamerKey, e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteAllRtcpChannels() {
        try {
            rtcpChannelMapLock.lock();
            rtcpChannelMap.entrySet().removeIf(Objects::nonNull);

            logger.debug("| Success to close all rtcp channel(s).");
        } catch (Exception e) {
            logger.warn("| Fail to close all rtcp channel(s).", e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public RtcpNettyChannel getRtcpChannel(String streamerKey) {
        try {
            rtcpChannelMapLock.lock();

            return rtcpChannelMap.get(streamerKey);
        } catch (Exception e) {
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public Streamer addStreamer(MediaType mediaType, String callId, String sessionId, String trackId, boolean isTcp) {
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to add the message sender. Not found the netty channel. (callId={}, trackId={}", callId, trackId, sessionId);
            return null;
        }

        return rtspNettyChannel.addStreamer(mediaType, callId, sessionId, trackId, isTcp);
    }

    public Streamer getStreamer(String key) {
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to get the message sender. Not found the netty channel.", key);
            return null;
        }

        return rtspNettyChannel.getStreamer(key);
    }

    public void deleteStreamer(Streamer streamer) {
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to delete the message sender. Not found the netty channel.", streamer.getKey());
            return;
        }

        rtspNettyChannel.deleteStreamer(streamer.getKey());
    }

    public void startStreaming(String key) {
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to start to stream media. Not found the netty channel", key);
            return;
        }

        rtspNettyChannel.startStreaming(key);
    }

    public void stopStreaming(String key) {
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to stop to stream media. Not found the netty channel", key);
            return;
        }

        rtspNettyChannel.stopStreaming(key);
    }

    public List<Streamer> getStreamerListByUri(String videoUri) {
        return rtspNettyChannel.getCloneStreamerMap().values().stream().filter(
                streamer -> {
                    if (streamer == null) {
                        return false;
                    }
                    return streamer.getUri().equals(videoUri);
                }
        ).collect(Collectors.toList());
    }

    public List<Streamer> getStreamerListByCallId(String callId) {
        Collection<Streamer> values = rtspNettyChannel.getCloneStreamerMap().values();
        if (values != null && !values.isEmpty()) {
            return values.stream().filter(
                    streamer -> {
                        if (streamer == null) {
                            return false;
                        }
                        return streamer.getCallId().equals(callId);
                    }
            ).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public Streamer getStreamerBySessionId(String sessionId) {
        return rtspNettyChannel.getCloneStreamerMap().values().stream().filter(
                streamer -> {
                    if (streamer == null) {
                        return false;
                    }
                    return streamer.getSessionId().equals(sessionId);
                }
        ).findFirst().orElse(null);
    }

    public List<Streamer> getAllStreamers() {
        return new ArrayList<>(rtspNettyChannel.getCloneStreamerMap().values());
    }

}
