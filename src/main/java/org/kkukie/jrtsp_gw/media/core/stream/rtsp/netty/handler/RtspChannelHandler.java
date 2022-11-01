package org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpRtp;
import media.core.rtsp.sdp.SdpSession;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.config.SdpConfig;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.base.MediaType;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.NettyChannelManager;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.stream.rtp.base.RtpMeta;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.call.model.ConferenceInfo;
import org.kkukie.jrtsp_gw.session.media.MediaSession;
import org.kkukie.jrtsp_gw.util.RandomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 * HTTP 는 TCP 연결이므로 매번 연결 상태가 변경된다. (연결 생성 > 비즈니스 로직 처리 > 연결 해제)
 */

@Slf4j
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final String RTSP_PREFIX = "rtsp://";

    private final DefaultConfig defaultConfig = ConfigManager.getDefaultConfig();
    private final SdpConfig sdpConfig = ConfigManager.getSdpConfig();

    private final String name;

    private final String listenIp; // local ip
    private final int listenRtspPort; // local(listen) rtsp port

    private Streamer audioContextStreamer = null;
    private Streamer videoContextStreamer = null;

    private boolean isAudioReq = false;

    private String lastSessionId = null;

    private ConferenceInfo conferenceInfo = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String listenIp, int listenRtspPort) {
        this.name = "RTSP_" + listenIp + ":" + listenRtspPort + "|" + System.currentTimeMillis() + "|" + UUID.randomUUID().toString().substring(0, 10);

        this.listenIp = listenIp;
        this.listenRtspPort = listenRtspPort;

        log.debug("({}) RtspChannelHandler is created. (listenIp={}, listenRtspPort={})", name, listenIp, listenRtspPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private static void setUserAgent(DefaultHttpRequest req, Streamer streamer) {
        String userAgent = req.headers().get(RtspHeaderNames.USER_AGENT);
        if (userAgent != null && !userAgent.isEmpty()) {
            streamer.setClientUserAgent(userAgent);
        }
    }

    private static void setUri(String uri, Streamer streamer) {
        if (uri.contains("*")) {
            uri = uri.replaceAll("[*]", " ");
        }
        streamer.setUri(uri);
    }

    public static void sendResponse(String name, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        final String cSeq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cSeq != null) {
            res.headers().add(RtspHeaderNames.CSEQ, cSeq);
        }
        res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);

        res.headers().add(
                RtspHeaderNames.DATE,
                LocalDateTime.now()
        );

        res.headers().add(
                RtspHeaderNames.CACHE_CONTROL,
                "no-cache"
        );

        if (ctx != null) {
            log.debug("({}) [{}] > Success to send the response: {}\n", name, req.method(), res);
            ctx.write(res);
        } else {
            log.warn("({}) [{}] > Fail to send the response: {}\n", name, req.method(), res);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof DefaultHttpRequest) {
                // 0) HTTP REQUEST PARSING
                DefaultHttpRequest req = (DefaultHttpRequest) msg;
                DefaultFullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.NOT_FOUND);
                if (checkRequest(ctx, req, res)) {
                    return;
                }

                // 1) OPTIONS
                if (req.method() == RtspMethods.OPTIONS) {
                    handleOptions(ctx, req, res);
                }
                // 2) DESCRIBE
                else if (req.method() == RtspMethods.DESCRIBE) {
                    handleDescribe(ctx, req, res);
                }
                // 3) SETUP
                else if (req.method() == RtspMethods.SETUP) {
                    handleSetup(ctx, req, res);
                }
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    handlePlay(ctx, req, res);
                }
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    handleTeardown(ctx, req, res);
                }
                // 6) GET_PARAMETER
                else if (req.method() == RtspMethods.GET_PARAMETER) {
                    handleGetParameter(ctx, req, res);
                }
                // UNKNOWN
                else {
                    log.warn("({}) () < Unknown method: {}", name, req);
                    sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.METHOD_NOT_ALLOWED);
                }
            }
        } catch (Exception e) {
            log.warn("({}) Fail to handle RTSP Packet.", name, e);
        }
    }

    private void handleGetParameter(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        res.setStatus(RtspResponseStatuses.OK);
        sendResponse(name, ctx, req, res);
    }

    private void handleOptions(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        log.debug("({}) () < OPTIONS\n{}", name, req);

        res.setStatus(RtspResponseStatuses.OK);
        res.headers().add(
                RtspHeaderValues.PUBLIC,
                RtspMethods.OPTIONS + ", " +
                        RtspMethods.DESCRIBE + ", " +
                        RtspMethods.SETUP + ", " +
                        RtspMethods.PLAY + ", " +
                        RtspMethods.TEARDOWN
        );
        sendResponse(name, ctx, req, res);
    }

    private void handleDescribe(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        log.debug("({}) () < DESCRIBE\n{}", name, req);

        /**
         * DESCRIBE rtsp://[domain name]:[port]/0cdef1795485d46babb5b505902828f7@192.168.5.222 RTSP/1.0
         * CSeq: 3
         * User-Agent: LibVLC/3.0.16 (LIVE555 Streaming Media v2016.11.28)
         * Accept: application/sdp
         */

        String acceptType = req.headers().get(RtspHeaderNames.ACCEPT);
        if (acceptType == null || acceptType.isEmpty()) {
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return;
        } else if (!acceptType.contains("application/sdp")) {
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
            return;
        }

        res.setStatus(RtspResponseStatuses.OK);
        res.headers().add(
                RtspHeaderNames.CONTENT_TYPE,
                "application/sdp"
        );

        String conferenceId = getConferenceId(req);
        if (conferenceId == null) {
            log.warn("({}) Fail to get uri.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return;
        }

        // Find Track ID
        conferenceId = getParseCallIdFromTrackId(ctx, req, res, conferenceId);
        if (conferenceId == null) {
            log.warn("({}) Fail to get conferenceId.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return;
        }

        ConferenceInfo conferenceInfo = getCallInfo(ctx, req, res, conferenceId);
        if (conferenceInfo == null) {
            log.warn("({}) Fail to get conferenceInfo.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.SERVICE_UNAVAILABLE);
            return;
        }

        MediaSession mediaSession = conferenceInfo.getMediaSession();
        if (mediaSession == null) {
            log.warn("Fail to get media info. ({})", conferenceInfo.getConferenceId());
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
            return;
        }

        SdpRtp audioSdpRtp = mediaSession.getRemoteSdpMediaInfo().getAudioDesc().getRtp().get(0);
        SdpRtp videoSdpRtp = mediaSession.getRemoteSdpMediaInfo().getVideoDesc().getRtp().get(0);
        if (audioSdpRtp == null || videoSdpRtp == null) {
            log.warn("Session media infos are not available. ({})", conferenceInfo.getConferenceId());
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
            return;
        }

        SdpSession localSdp = sdpConfig.loadLocalSdpConfig(
                defaultConfig.getId(),
                0, // RTP 를 수신할 필요가 없음 (sendonly)
                mediaSession.getRemoteSdpMediaInfo().getAudioPayloadType(),
                mediaSession.getRemoteSdpMediaInfo().getAudioRtpInfo(),
                mediaSession.getRemoteSdpMediaInfo().getVideoPayloadType(),
                mediaSession.getRemoteSdpMediaInfo().getVideoRtpInfo()
        );
        ByteBuf buf = Unpooled.copiedBuffer(localSdp.write(), StandardCharsets.UTF_8);
        res.headers().add(
                RtspHeaderNames.CONTENT_LENGTH,
                buf.writerIndex()
        );
        res.content().writeBytes(buf);

        sendResponse(name, ctx, req, res);
    }

    private String removeTrackIdFromUri(String uri) {
        int trackIdPos = uri.indexOf(RtpMeta.TRACK_ID_TAG);
        if (trackIdPos > 0) {
            uri = uri.substring(0, trackIdPos - 1);
        }
        return uri;
    }

    private String getParseCallIdFromTrackId(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String conferenceId) {
        int trackIdPos = conferenceId.indexOf(RtpMeta.TRACK_ID_TAG);
        if (trackIdPos > 0) {
            String trackId = getTrackIdFromCallId(conferenceId);
            if (trackId == null || trackId.isEmpty()) {
                log.warn("({}) Fail to get uri. Predefined Track ID is wrong.", name);
                sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
                return null;
            }
            conferenceId = conferenceId.substring(0, trackIdPos - 1);
            log.debug("({}) conferenceId: {}, trackId: {}", name, conferenceId, trackId);
        }

        return conferenceId;
    }

    private String getTrackIdFromCallId(String conferenceId) {
        try {
            int trackIdPos = conferenceId.indexOf(RtpMeta.TRACK_ID_TAG);
            String trackId = conferenceId.substring(trackIdPos + RtpMeta.TRACK_ID_TAG.length() + 1);
            if (!trackId.isEmpty()) {
                if (trackId.equals(RtpMeta.AUDIO_TRACK_ID)) {
                    isAudioReq = true;
                } else if (trackId.equals(RtpMeta.VIDEO_TRACK_ID)) {
                    isAudioReq = false;
                } else {
                    return null;
                }
            }
            return trackId;
        } catch (Exception e) {
            return null;
        }
    }

    private void handleSetup(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        log.debug("({}) () < SETUP\n{}", name, req);

        /**
         * [UDP]
         *
         * SETUP rtsp://[domain name]:[port]/938507ed543ac177f61164e9ecb4c50b@192.168.5.222 RTSP/1.0
         * CSeq: 0
         * Transport: RTP/AVP;unicast;client_port=9406-9407
         */

        String conferenceId = getConferenceId(req);
        if (conferenceId == null) {
            log.warn("({}) Fail to get uri.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return;
        }

        // Find Track ID
        String originCallId = conferenceId;
        conferenceId = getParseCallIdFromTrackId(ctx, req, res, conferenceId);
        if (conferenceId == null) {
            return;
        }

        ConferenceInfo conferenceInfo = getCallInfo(ctx, req, res, conferenceId);
        if (conferenceInfo == null) {
            return;
        }

        // SESSION ID
        String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
        if (curSessionId == null || curSessionId.isEmpty()) {
            curSessionId = String.valueOf(RandomManager.getIntegerLong(1000000));
        }
        log.debug("({}) Current sessionId is [{}].", name, curSessionId);
        lastSessionId = curSessionId;

        String transportHeaderContent = req.headers().get(RtspHeaderNames.TRANSPORT);
        boolean isTcp = transportHeaderContent.contains(String.valueOf(RtspHeaderValues.INTERLEAVED));

        String trackId = getTrackIdFromCallId(originCallId);
        if (!saveStreamer(ctx, req, res, curSessionId, trackId, conferenceInfo, isTcp)) {
            return;
        }

        Streamer currentContextStreamer = null;
        if (trackId != null && !trackId.isEmpty()) {
            if (trackId.equals(RtpMeta.AUDIO_TRACK_ID)) {
                currentContextStreamer = audioContextStreamer;
                log.debug("({}) AudioContextStreamer is created. (sessionId={})", currentContextStreamer.getKey(), currentContextStreamer.getSessionId());
            } else if (trackId.equals(RtpMeta.VIDEO_TRACK_ID)) {
                currentContextStreamer = videoContextStreamer;
                log.debug("({}) VideoContextStreamer is created. (sessionId={})", currentContextStreamer.getKey(), currentContextStreamer.getSessionId());
            }
        }
        if (currentContextStreamer == null) {
            log.warn("Unknown track id is detected. ({})", trackId);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
            return;
        }

        // URI
        setUri(removeTrackIdFromUri(req.uri()), currentContextStreamer);

        // USERAGENT
        setUserAgent(req, currentContextStreamer);

        // TRANSPORT
        setRtpDestIp(ctx, transportHeaderContent, currentContextStreamer);

        if (isTcp) {
            setupTcp(ctx, req, res, transportHeaderContent, currentContextStreamer);
        } else {
            setupUdp(ctx, req, res, transportHeaderContent, currentContextStreamer);
        }
    }

    private boolean saveStreamer(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String curSessionId, String trackId, ConferenceInfo conferenceInfo, boolean isTcp) {
        Streamer streamer = addStreamer(conferenceInfo.getConferenceId(), curSessionId, trackId, isTcp);
        if (streamer == null || streamer.getTrackId() == null) {
            log.warn("({}) ({}) Streamer is not defined. (listenIp={}, listenPort={})",
                    name, curSessionId, listenIp, listenRtspPort
            );
            sendFailResponse(name, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
            return false;
        }

        releaseStreamerFromContext(streamer.getKey());
        saveStreamerToContext(ctx, streamer);
        return true;
    }

    private ConferenceInfo getCallInfo(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String conferenceId) {
        // CHECK CALL INFO
        ConferenceInfo conferenceInfo = ConferenceMaster.getInstance().findConference(conferenceId);
        if (conferenceInfo == null) {
            conferenceInfo = ConferenceMaster.getInstance().createConference(conferenceId, true);
            if (conferenceInfo == null) {
                log.warn("({}) Fail to get the conferenceInfo. ConferenceInfo is null.", name);
                sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
                return null;
            }
        }

        try {
            if (!conferenceInfo.getWebrtcHandshakeLatch().await(2, TimeUnit.SECONDS)) {
                // TIMEOUT !
                log.warn("({}) Timeout! Fail to wait the getWebrtcHandshakeLatch.", name);
                return null;
            }
        } catch (InterruptedException e) {
            log.warn("({}) Fail to wait the getWebrtcHandshakeLatch.", name, e);
            return null;
        }

        if (this.conferenceInfo == null) {
            this.conferenceInfo = conferenceInfo;
            this.conferenceInfo.addCall(name);
        }

        return conferenceInfo;
    }

    private Streamer addStreamer(String conferenceId, String curSessionId, String trackId, boolean isTcp) {
        // setup 요청 시마다 기존 streamer 삭제하고 새로운 streamer 생성
        Streamer streamer = NettyChannelManager.getInstance().getStreamer(
                getStreamerKey(conferenceId, trackId, curSessionId)
        );
        if (streamer != null) {
            NettyChannelManager.getInstance().deleteStreamer(streamer);
        }

        return NettyChannelManager.getInstance().addStreamer(
                isAudioReq ? MediaType.AUDIO : MediaType.VIDEO,
                conferenceId,
                curSessionId,
                trackId,
                isTcp
        );
    }

    private void setupUdp(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String transportHeaderContent, Streamer streamer) {
        if (getTransportInfo(ctx, req, res, transportHeaderContent, streamer)) {
            return;
        }

        // Listen RTCP?
        /*int rtcpDestPort = streamer.getRtcpDestPort();
        NettyChannelManager.getInstance().openRtcpChannel(streamer.getKey(), streamer.getListenIp(), rtcpDestPort);*/

        res.headers().add(
                RtspHeaderNames.TRANSPORT,
                transportHeaderContent
                // > Server listen rtcp port 설정안하면 client port 로 client 에서 rtcp packet 송신
                //+ ";server_port=" + listenRtspPort + "-" + rtspUnit.getRtcpListenPort()
                //+ ";ssrc=" + (isAudioReq? streamer.getAudioSsrc() : streamer.getVideoSsrc())
        );
        sendNormalOkResponse(res, ctx, req);

        log.debug("({}) ({}) Success to setup the udp stream. (rtpDestIp={}, rtpDestPort={}, rtcpDestPort={})",
                name, streamer.getKey(), streamer.getDestIp(), streamer.getRtpDestPort(), streamer.getRtcpDestPort()
        );
    }

    private boolean getTransportInfo(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String transportHeaderContent, Streamer streamer) {
        String rtpDestPortString = null;
        if (transportHeaderContent.contains(RtspHeaderValues.CLIENT_PORT)) {
            rtpDestPortString = getTransportAttribute(transportHeaderContent, RtspHeaderValues.CLIENT_PORT);
        } else if (transportHeaderContent.contains(RtspHeaderValues.PORT)) {
            rtpDestPortString = getTransportAttribute(transportHeaderContent, RtspHeaderValues.PORT);
        }
        if (rtpDestPortString == null) {
            log.warn("({}) ({}) Fail to parse rtp destination port. (transportHeaderContent={})",
                    name, streamer.getKey(), transportHeaderContent
            );
            sendFailResponse(name, ctx, req, res, streamer.getSessionId(), RtspResponseStatuses.NOT_ACCEPTABLE);
            return true;
        }

        if (rtpDestPortString.contains("-")) {
            String rtcpDesPortString = rtpDestPortString.substring(
                    rtpDestPortString.lastIndexOf("-") + 1
            );
            int rtcpDestPort = Integer.parseInt(rtcpDesPortString);
            if (rtcpDestPort <= 0) {
                log.warn("({}) ({}) Fail to parse rtcp destination port. (transportHeaderContent={})",
                        name, streamer.getKey(), transportHeaderContent
                );
                sendFailResponse(name, ctx, req, res, streamer.getSessionId(), RtspResponseStatuses.NOT_ACCEPTABLE);
                return true;
            } else {
                streamer.setRtcpDestPort(rtcpDestPort);
            }
            rtpDestPortString = rtpDestPortString.substring(0, rtpDestPortString.lastIndexOf("-"));

            int rtpDestPort = Integer.parseInt(rtpDestPortString);
            if (rtpDestPort <= 0) {
                log.warn("({}) ({}) Fail to parse rtp destination port. (transportHeaderContent={})",
                        name, streamer.getKey(), transportHeaderContent
                );
                sendFailResponse(name, ctx, req, res, streamer.getSessionId(), RtspResponseStatuses.NOT_ACCEPTABLE);
                return true;
            }
            streamer.setRtpDestPort(rtpDestPort);
        }
        return false;
    }

    private void setupTcp(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res, String transportHeaderContent, Streamer streamer) {
        res.headers().add(
                RtspHeaderNames.TRANSPORT,
                transportHeaderContent
        );
        sendNormalOkResponse(res, ctx, req);

        log.debug("({}) ({}) Success to setup the tcp stream.", name, streamer.getKey());
    }

    private void sendNormalOkResponse(DefaultFullHttpResponse res, ChannelHandlerContext ctx, DefaultHttpRequest req) {
        res.setStatus(RtspResponseStatuses.OK);
        res.headers().add(
                RtspHeaderNames.SESSION,
                lastSessionId // + ";timeout=60"
        );
        sendResponse(name, ctx, req, res);
    }

    private void saveStreamerToContext(ChannelHandlerContext ctx, Streamer streamer) {
        if (streamer.getTrackId().equals(RtpMeta.AUDIO_TRACK_ID)) {
            audioContextStreamer = streamer;
            audioContextStreamer.setRtspChannelContext(ctx);
        } else if (streamer.getTrackId().equals(RtpMeta.VIDEO_TRACK_ID)) {
            videoContextStreamer = streamer;
            videoContextStreamer.setRtspChannelContext(ctx);
        }
    }

    private void releaseStreamerFromContext(String key) {
        if (audioContextStreamer != null && audioContextStreamer.getKey().equals(key)) {
            audioContextStreamer.setRtspChannelContext(null);
            audioContextStreamer.stop();
            log.debug("({}) AudioContextStreamer is removed.", audioContextStreamer.getKey());
            audioContextStreamer = null;
        }

        if (videoContextStreamer != null && videoContextStreamer.getKey().equals(key)) {
            videoContextStreamer.setRtspChannelContext(null);
            videoContextStreamer.stop();
            log.debug("({}) VideoContextStreamer is removed.", videoContextStreamer.getKey());
            videoContextStreamer = null;
        }
    }

    private void setRtpDestIp(ChannelHandlerContext ctx, String transportHeaderContent, Streamer streamer) {
        /**
         * EX) Transport: RTP/AVP;multicast;destination=224.2.0.1;
         *              client_port=3456-3457;ttl=16
         */
        String rtpDestIp = getTransportAttribute(transportHeaderContent, RtspHeaderValues.DESTINATION);
        if (rtpDestIp != null) {
            streamer.setDestIp(rtpDestIp);
        } else {
            SocketAddress socketAddress = ctx.channel().remoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();
                if (inetAddress instanceof Inet4Address) {
                    log.debug("({}) ({}) IPv4: {}", name, streamer.getKey(), inetAddress);
                    streamer.setDestIp(inetAddress.getHostAddress()); // Remote IP Address
                } else if (inetAddress instanceof Inet6Address) {
                    log.warn("({}) ({}) IPv6: {}", name, streamer.getKey(), inetAddress);
                } else {
                    log.warn("({}) ({}) Not an IP address.", name, streamer.getKey());
                }
            } else {
                log.warn("({}) ({}) Not an internet protocol socket.", name, streamer.getKey());
            }
        }
        log.warn("({}) ({}) Destination ip is [{}].", name, streamer.getKey(), streamer.getDestIp());
    }

    private void handlePlay(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        log.debug("({}) () < PLAY\n{}", name, req);

        if (audioContextStreamer != null && videoContextStreamer != null
                && (audioContextStreamer.isTcp() != videoContextStreamer.isTcp())) {
            log.warn("({}) Audio & Video transport is not matched. (audio={}, video={})",
                    name, audioContextStreamer.isTcp() ? "TCP" : "UDP", videoContextStreamer.isTcp() ? "TCP" : "UDP"
            );
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
            return;
        }

        if (audioContextStreamer != null && videoContextStreamer != null) {
            log.debug("({}) AudioContextStreamer is selected. (sessionId={})", audioContextStreamer.getKey(), audioContextStreamer.getSessionId());
            log.debug("({}) VideoContextStreamer is selected. (sessionId={})", videoContextStreamer.getKey(), videoContextStreamer.getSessionId());

            if (audioContextStreamer.isTcp() && videoContextStreamer.isTcp()) {
                String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                if (curSessionId == null) {
                    log.warn("({}) () SessionId is null. Fail to process PLAY method. (listenIp={}, listenRtspPort={})",
                            name, listenIp, listenRtspPort
                    );
                    sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
                    return;
                }
                log.debug("({}) () Current sessionId is [{}].", name, curSessionId);

                NettyChannelManager.getInstance().startStreaming(audioContextStreamer.getKey());
                NettyChannelManager.getInstance().startStreaming(videoContextStreamer.getKey());

                res.setStatus(RtspResponseStatuses.OK);
                res.headers().add(
                        RtspHeaderNames.SERVER,
                        defaultConfig.getId()
                );
                if (!curSessionId.isEmpty()) {
                    res.headers().add(
                            RtspHeaderNames.SESSION,
                            curSessionId // + ";timeout=60"
                    );
                }

                Streamer streamer = NettyChannelManager.getInstance().getStreamerBySessionId(curSessionId);
                if (streamer != null) {
                    log.debug("Play response is saved in [{}]", streamer.getKey());
                    streamer.sendPlayResponse(res);
                } else {
                    audioContextStreamer.sendPlayResponse(res);
                }
            } else {
                String conferenceId = getConferenceId(req);
                if (conferenceId == null) {
                    log.warn("({}) Fail to get uri.", name);
                    sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
                    return;
                }

                // CHECK REQUEST
                String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                if (curSessionId == null) {
                    log.warn("({}) () SessionId is null. Fail to process PLAY method. (listenIp={}, listenRtspPort={})",
                            name, listenIp, listenRtspPort
                    );
                    sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
                    return;
                }
                log.debug("({}) Current sessionId is [{}].", name, curSessionId);

                // CHECK RTSP DESTINATION PORT
                int audioDestPort = audioContextStreamer.getRtpDestPort();
                if (audioDestPort <= 0) {
                    log.warn("({}) ({}) Fail to process the PLAY request. Audio destination port is wrong. (destPort={})",
                            name, audioContextStreamer.getKey(), audioDestPort
                    );
                    sendFailResponse(name, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                    return;
                }
                int videoDestPort = videoContextStreamer.getRtpDestPort();
                if (videoDestPort <= 0) {
                    log.warn("({}) ({}) Fail to process the PLAY request. Video destination port is wrong. (destPort={})",
                            name, videoContextStreamer.getKey(), videoDestPort
                    );
                    sendFailResponse(name, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                    return;
                }

                // CHECK RTSP DESTINATION IP
                NettyChannelManager.getInstance().startStreaming(audioContextStreamer.getKey());
                NettyChannelManager.getInstance().startStreaming(videoContextStreamer.getKey());

                // SUCCESS RESPONSE
                res.setStatus(RtspResponseStatuses.OK);
                res.headers().add(
                        RtspHeaderNames.SERVER,
                        defaultConfig.getId()
                );
                if (!curSessionId.isEmpty()) {
                    res.headers().add(
                            RtspHeaderNames.SESSION,
                            curSessionId // + ";timeout=60"
                    );
                }

                // Callback
                Streamer streamer = NettyChannelManager.getInstance().getStreamerBySessionId(curSessionId);
                if (streamer != null) {
                    log.debug("Play response is saved in [{}]", streamer.getKey());
                    streamer.sendPlayResponse(res);
                } else {
                    audioContextStreamer.sendPlayResponse(res);
                }
            }
        }

        if (audioContextStreamer == null || videoContextStreamer == null) {
            log.warn("({}) () Streamer is null. Fail to process PLAY method.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
        }
    }

    private String getConferenceId(DefaultHttpRequest req) {
        String uri = req.uri();

        uri = uri.substring(uri.indexOf(RTSP_PREFIX) + RTSP_PREFIX.length());
        if (uri.charAt(uri.length() - 1) == '/') {
            uri = uri.substring(0, uri.length() - 1);
        }

        String conferenceId = uri.substring(uri.indexOf("/") + 1);
        if (conferenceId.isEmpty()) {
            return null;
        }
        log.debug("({}) () Call-ID: {}", name, conferenceId);
        return conferenceId;
    }

    private void handleTeardown(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        log.debug("({}) () < TEARDOWN\n{}", name, req);

        if (conferenceInfo != null) {
            conferenceInfo.removeCall(name);
        }

        if (audioContextStreamer != null) {
            NettyChannelManager.getInstance().stopStreaming(audioContextStreamer.getKey());
            NettyChannelManager.getInstance().deleteStreamer(audioContextStreamer);
            log.debug("({}) ({}) Stop the streaming.", name, audioContextStreamer.getKey());
        }

        if (videoContextStreamer != null) {
            NettyChannelManager.getInstance().stopStreaming(videoContextStreamer.getKey());
            NettyChannelManager.getInstance().deleteStreamer(videoContextStreamer);
            log.debug("({}) ({}) Stop the streaming.", name, videoContextStreamer.getKey());
        }

        sendNormalOkResponse(res, ctx, req);
    }

    private String getTransportAttribute(String transportHeaderContent, AsciiString targetString) {
        int pos = transportHeaderContent.lastIndexOf(String.valueOf(targetString));
        if (pos < 0) {
            return null;
        }

        String posString = transportHeaderContent.substring(
                pos + targetString.length() + 1
        );

        int semicolonPos = posString.indexOf(";");
        if (semicolonPos >= 0) {
            posString = posString.substring(
                    0, posString.indexOf(";")
            );
        }

        return posString;
    }

    private boolean checkRequest(ChannelHandlerContext ctx, DefaultHttpRequest req, DefaultFullHttpResponse res) {
        if (req.headers() == null || req.headers().isEmpty()) {
            log.warn("({}) Fail to process the request. Header is not exist.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return true;
        }

        if (req.protocolVersion() != RtspVersions.RTSP_1_0) {
            log.warn("({}) Fail to process the request. Protocol version is not matched. ({})", name, req.protocolVersion());
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return true;
        }

        if (req.method() == null || req.method().name() == null || req.method().name().isEmpty()) {
            log.warn("({}) Fail to process the request. Request method is not exist.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return true;
        }

        // OPTIONS 가 아닌데 URI 가 없으면 탈락
        if ((req.method() != RtspMethods.OPTIONS)
                && (req.uri() == null || req.uri().isEmpty())) {
            log.warn("({}) Fail to process the request. Request uri is not exist.", name);
            sendFailResponse(name, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
            return true;
        }

        return false;
    }

    public void sendFailResponse(String name, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res, String curSessionId, HttpResponseStatus httpResponseStatus) {
        res.setStatus(httpResponseStatus);
        if (curSessionId != null && curSessionId.length() > 0) {
            res.headers().add(
                    RtspHeaderNames.SESSION,
                    curSessionId // + ";timeout=60"
            );
        }
        sendResponse(name, ctx, req, res);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (conferenceInfo != null) {
            conferenceInfo.removeCall(name);
        }

        if (audioContextStreamer != null) {
            releaseStreamerFromContext(audioContextStreamer.getKey());
        }

        if (videoContextStreamer != null) {
            releaseStreamerFromContext(videoContextStreamer.getKey());
        }

        log.warn("({}) RtspChannelHandler is inactive.", name);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("({}) RtspChannelHandler.Exception (cause={})", name, cause.toString());
    }

    private String getStreamerKey(String conferenceId, String trackId, String sessionId) {
        if (trackId != null && !trackId.isEmpty()) {
            return conferenceId + ":" + trackId + ":" + sessionId;
        }
        return conferenceId + ":" + sessionId;
    }

}
