package org.kkukie.jrtsp_gw.media.stream.manager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.scheduler.WallClock;
import org.kkukie.jrtsp_gw.media.dtls.DtlsHandler;
import org.kkukie.jrtsp_gw.media.dtls.DtlsListener;
import org.kkukie.jrtsp_gw.media.rtp.RtpInfo;
import org.kkukie.jrtsp_gw.media.rtp.channels.PacketHandlerPipeline;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.media.rtp.statistics.RtpStatistics;
import org.kkukie.jrtsp_gw.media.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.rtsp.netty.NettyChannelManager;
import org.kkukie.jrtsp_gw.media.rtsp.rtcp.module.RtpClock;
import org.kkukie.jrtsp_gw.media.stream.handler.RtcpHandler;
import org.kkukie.jrtsp_gw.media.stream.handler.RtpHandler;
import org.kkukie.jrtsp_gw.media.stream.model.DataChannel;
import org.kkukie.jrtsp_gw.media.stun.candidate.IceComponent;
import org.kkukie.jrtsp_gw.media.stun.handler.IceHandler;
import org.kkukie.jrtsp_gw.media.stun.model.IceAuthenticatorImpl;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.ice.IceInfo;
import org.kkukie.jrtsp_gw.session.media.MediaSession;
import org.kkukie.jrtsp_gw.session.media.base.MediaType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Map;

import static org.kkukie.jrtsp_gw.media.stream.model.PacketPriority.*;

@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class PacketHandlerMaster {

    private final String conferenceId;
    private final MediaSession mediaSession;

    public final PacketHandlerPipeline handlers = new PacketHandlerPipeline();

    private IceHandler iceHandler;
    private DtlsHandler dtlsHandler;
    private RtpHandler rtpHandler;
    private RtcpHandler rtcpHandler;

    public void initIce(IceInfo iceInfo, List<InetSocketAddress> targetAddressList, DataChannel dataChannel) {
        iceHandler = new IceHandler(conferenceId, IceComponent.RTP_ID, dataChannel);

        IceAuthenticatorImpl iceAuthenticator = new IceAuthenticatorImpl();
        iceAuthenticator.setUfrag(iceInfo.getLocalIceUfrag());
        iceAuthenticator.setPassword(iceInfo.getLocalIcePasswd());
        iceHandler.setAuthenticator(iceAuthenticator);

        iceHandler.setPipelinePriority(STUN_PRIORITY);
        handlers.addHandler(iceHandler);

        iceHandler.startHarvester(
                dataChannel,
                iceInfo,
                targetAddressList
        );
    }

    public void initDtls(DatagramChannel mediaChannel, SocketAddress realRemoteAddress, DtlsListener dtlsListener) {
        if (mediaSession.isSecure()) {
            dtlsHandler = new DtlsHandler(conferenceId, realRemoteAddress);
            dtlsHandler.setChannel(mediaChannel);
            dtlsHandler.addListener(dtlsListener);
            dtlsHandler.setPipelinePriority(DTLS_PRIORITY);
            handlers.addHandler(dtlsHandler);
        }
    }

    public void initRtp(DatagramChannel mediaChannel, SocketAddress realRemoteAddress, Map<String, RTPFormats> mediaFormatMap) {
        RtpClock rtpClock = new RtpClock(new WallClock());
        RtpClock oobClock = new RtpClock(new WallClock());
        RtpStatistics rtpStatistics = new RtpStatistics(rtpClock);

        initRtpHandler(mediaFormatMap, rtpClock, oobClock, rtpStatistics);
        initRtcpHandler(mediaChannel, realRemoteAddress, rtpStatistics);
    }

    private void initRtpHandler(Map<String, RTPFormats> mediaFormatMap,
                                RtpClock rtpClock,
                                RtpClock oobClock, RtpStatistics rtpStatistics) {
        rtpHandler = new RtpHandler(
                conferenceId,
                rtpClock, oobClock,
                rtpStatistics,
                mediaFormatMap
        );
        rtpHandler.setRtpRecvCallback(this::handleRtpPacket);
        rtpHandler.setPipelinePriority(RTP_PRIORITY);
        handlers.addHandler(rtpHandler);
        if (mediaSession.isSecure()) {
            rtpHandler.enableSrtp(dtlsHandler);
        } else {
            rtpHandler.disableSrtp();
        }
    }

    private void initRtcpHandler(DatagramChannel mediaChannel, SocketAddress realRemoteAddress, RtpStatistics rtpStatistics) {
        if (mediaSession.isRtcpMux()) {
            rtcpHandler = new RtcpHandler(
                    conferenceId, mediaChannel.socket(),
                    rtpStatistics, MediaType.AUDIO.getName(), realRemoteAddress
            );
            rtcpHandler.start();
            rtcpHandler.setPipelinePriority(RTCP_PRIORITY);
            handlers.addHandler(rtcpHandler);
            if (mediaSession.isSecure()) {
                rtcpHandler.enableSRTCP(dtlsHandler);
            } else {
                rtcpHandler.disableSRTCP();
            }
        }
    }

    public void handleRtpPacket(RtpInfo rtpInfo) {
        // Send to Rtsp Client
        if (rtpInfo.getMediaType().equals(MediaType.AUDIO.getName())) {
            mediaSession.getRemoteSdpMediaInfo().setAudioPayloadType(rtpInfo.getRtpPacket().getPayloadType());
            /*log.info("|MediaSession({})| AUDIO [{}] >>> ({}) {}/{}", conferenceId,
                    remoteAudioPayloadType,
                    rtpInfo.getRtpPacket().getSyncSource(),
                    rtpInfo.getRtpPacket().getSeqNumber(), rtpInfo.getRtpPacket().getTimestamp()
            );*/
        } else if (rtpInfo.getMediaType().equals(MediaType.VIDEO.getName())) {
            mediaSession.getRemoteSdpMediaInfo().setVideoPayloadType(rtpInfo.getRtpPacket().getPayloadType());
            /*log.info("|MediaSession({})| VIDEO [{}] >>> ({}) {}/{}", conferenceId,
                    remoteVideoPayloadType,
                    rtpInfo.getRtpPacket().getSyncSource(),
                    rtpInfo.getRtpPacket().getSeqNumber(), rtpInfo.getRtpPacket().getTimestamp()
            );*/
        }

        relayToRtspClient(rtpInfo);
    }

    private void relayToRtspClient(RtpInfo rtpInfo) {
        List<Streamer> streamerList = NettyChannelManager.getInstance().getStreamerListByCallId(conferenceId);
        if (streamerList == null || streamerList.isEmpty()) {
            return;
        }

        for (Streamer streamer : streamerList) {
            applyRtpMetaToStreamer(rtpInfo, streamer);
            if (streamer.isStarted()) {
                streamer.sendRtpPacket(rtpInfo.getRtpPacket(), rtpInfo.getMediaType());
            }
        }
    }

    private void applyRtpMetaToStreamer(RtpInfo rtpInfo, Streamer streamer) {
        if (rtpInfo.getMediaType().equals(MediaType.AUDIO.getName())) {
            streamer.setAudioSsrc(rtpInfo.getRtpPacket().getSyncSource());
            streamer.setAudioCurSeqNum(rtpInfo.getRtpPacket().getSeqNumber());
            streamer.setAudioCurTimeStamp(rtpInfo.getRtpPacket().getTimestamp());
        } else if (rtpInfo.getMediaType().equals(MediaType.VIDEO.getName())) {
            streamer.setVideoSsrc(rtpInfo.getRtpPacket().getSyncSource());
            streamer.setVideoCurSeqNum(rtpInfo.getRtpPacket().getSeqNumber());
            streamer.setVideoCurTimeStamp(rtpInfo.getRtpPacket().getTimestamp());
        }
    }

    public void selectCandidate(boolean useCandidate) {
        if (mediaSession.isSecure() && dtlsHandler != null) {
            dtlsHandler.handshake(useCandidate);
        }
    }

    public void reset() {
        if (rtpHandler != null) {
            handlers.removeHandler(rtpHandler);
            rtpHandler = null;
        }

        if (rtcpHandler != null) {
            rtcpHandler.stop();
            handlers.removeHandler(rtcpHandler);
            rtcpHandler = null;
        }

        if (iceHandler != null) {
            iceHandler.stopHarvester();
            handlers.removeHandler(iceHandler);
            iceHandler = null;
        }

        try {
            if (dtlsHandler != null) {
                dtlsHandler.close();
                handlers.removeHandler(dtlsHandler);
                dtlsHandler = null;
            }
        } catch (IOException e) {
            // ignore
        }
    }

}
