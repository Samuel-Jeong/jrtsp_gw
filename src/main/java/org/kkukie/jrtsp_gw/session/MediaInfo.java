package org.kkukie.jrtsp_gw.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpMedia;
import media.core.rtsp.sdp.SdpMline;
import media.core.rtsp.sdp.SdpRtp;
import media.core.rtsp.sdp.SdpSession;
import org.kkukie.jrtsp_gw.media.core.spi.MediaType;
import org.kkukie.jrtsp_gw.media.core.spi.format.FormatFactory;
import org.kkukie.jrtsp_gw.media.rtp.RtpInfo;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormat;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.media.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.rtsp.netty.NettyChannelManager;
import org.kkukie.jrtsp_gw.media.stream.manager.ChannelMaster;
import org.kkukie.jrtsp_gw.media.stream.model.DataChannel;
import org.kkukie.jrtsp_gw.media.stream.util.WebSocketPortManager;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.IceInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

@Getter
@Setter
@Slf4j
public class MediaInfo {

    private final String callId;
    private final SdpSession remoteSdp;
    private SocketAddress localMediaAddress;

    private DataChannel dataChannel;
    private DataChannel videoDataChannel;

    private int remoteAudioPayloadType = 0;
    private String remoteAudioRtpInfo = null;
    private int remoteVideoPayloadType = 0;
    private String remoteVideoRtpInfo = null;

    private SdpMedia remoteAudioDesc;

    private SdpMedia remoteVideoDesc;

    private SdpMedia remoteApplicationDesc;

    private final HashMap<String, RTPFormats> mediaFormatMap = new HashMap<>();

    private boolean isSecure = false;
    private boolean isRtcpMux = false;

    private final ChannelMaster channelMaster;

    private IceInfo iceInfo = null;
    private Queue<InetSocketAddress> targetAddressQueue = null;

    public MediaInfo(ChannelMaster channelMaster, String callId, SdpSession remoteSdp) {
        this.channelMaster = channelMaster;
        this.callId = callId;
        this.remoteSdp = remoteSdp;

        if (remoteSdp.getFingerprint() != null) {
            isSecure = true;
        }

        for (SdpMedia media : remoteSdp.getMedia()) {
            SdpMline mediaLine = media.getMline();
            if (mediaLine == null) {
                continue;
            }

            if (media.getFingerprint() != null) { isSecure = true; }
            if (media.getRtcpMux() != null) { isRtcpMux = true; }

            if (mediaLine.getType().equals("audio")) {
                remoteAudioDesc = media;
                remoteAudioPayloadType = Integer.parseInt(media.getMline().getPayloads().split(" ")[0]);
                SdpRtp priorityRtp = media.getRtp().get(0);
                if (priorityRtp != null) {
                    RTPFormats audioFormats = new RTPFormats();
                    audioFormats.add(new RTPFormat(
                                    (int) priorityRtp.getPayload(),
                                    FormatFactory.createAudioFormat(
                                            priorityRtp.getCodec(),
                                            priorityRtp.getRate().intValue(),
                                            8, 2
                                    )
                            )
                    );
                    mediaFormatMap.putIfAbsent(MediaType.AUDIO.getName(), audioFormats);
                    remoteAudioRtpInfo = priorityRtp.getCodec() + "/"
                            + priorityRtp.getRate().toString()
                            + (priorityRtp.getEncoding() != null? "/" + priorityRtp.getEncoding() : "");
                    log.debug("|MediaInfo({})| priorityRtp: {}, remoteAudioRtpInfo: {}", callId, priorityRtp, remoteAudioRtpInfo);
                } else {
                    log.warn("|MediaInfo({})| Fail to get the priority rtp info. (media-line={})", callId, media.getMline());
                }
                log.debug("|MediaInfo({})| remoteAudioDesc.getMid: {}", callId, remoteAudioDesc.getMid() != null ? remoteAudioDesc.getMid().getValue() : null);
            } else if (mediaLine.getType().equals("video")) {
                remoteVideoDesc = media;
                remoteVideoPayloadType = Integer.parseInt(media.getMline().getPayloads().split(" ")[0]);
                SdpRtp priorityRtp = media.getRtp().get(0);
                if (priorityRtp != null) {
                    RTPFormats videoFormats = new RTPFormats();
                    videoFormats.add(
                            new RTPFormat(
                                    (int) priorityRtp.getPayload(),
                                    FormatFactory.createVideoFormat(priorityRtp.getCodec()),
                                    priorityRtp.getRate().intValue()
                            )
                    );
                    mediaFormatMap.putIfAbsent(MediaType.VIDEO.getName(), videoFormats);
                    remoteVideoRtpInfo = priorityRtp.getCodec() + "/"
                            + priorityRtp.getRate().toString()
                            + (priorityRtp.getEncoding() != null? "/" + priorityRtp.getEncoding() : "");
                    log.debug("|MediaInfo({})| priorityRtp: {}, remoteVideoRtpInfo: {}", callId, priorityRtp, remoteVideoRtpInfo);
                } else {
                    log.warn("|MediaInfo({})| Fail to get the priority rtp info. (media-line={})", callId, media.getMline());
                }
                log.debug("|MediaInfo({})| remoteVideoDesc.getMid: {}", callId, remoteVideoDesc.getMid() != null ? remoteVideoDesc.getMid().getValue() : null);
            } else if (mediaLine.getType().equals("application")) {
                remoteApplicationDesc = media;
                log.debug("|MediaInfo({})|remoteApplicationDesc.getMid: {}", callId,
                        remoteApplicationDesc.getMid() != null ? remoteApplicationDesc.getMid().getValue() : null
                );
            }
        }
    }

    public boolean allocateMediaChannel(SocketAddress localAddress,
                                        IceInfo iceInfo, Queue<InetSocketAddress> targetAddressQueue) {
        try {
            if (dataChannel == null) {
                this.iceInfo = iceInfo;
                this.targetAddressQueue = targetAddressQueue;
                dataChannel = new DataChannel(
                        channelMaster, this,
                        callId, localAddress,
                        mediaFormatMap, isSecure, isRtcpMux,
                        iceInfo.getLocalIceUfrag(), iceInfo.getLocalIcePasswd()
                );
            } else {
                log.warn("|MediaInfo({})| Media channel Already Opened", callId);
                return false;
            }
        } catch (Exception e) {
            log.warn("|MediaInfo({})| Media channel is not created", callId, e);
            return false;
        }

        return true;
    }

    public void freeMediaChannel() {
        if (dataChannel != null) {
            dataChannel.close();
            WebSocketPortManager.getInstance().restorePort(((InetSocketAddress) localMediaAddress).getPort());
        }
    }

    public void handleRtpPacket(RtpInfo rtpInfo) {
        // Send to Rtsp Client
        if (rtpInfo.getMediaType().equals(MediaType.AUDIO.getName())) {
            remoteAudioPayloadType = rtpInfo.getRtpPacket().getPayloadType();
            /*log.info("|MediaInfo({})| AUDIO [{}] >>> ({}) {}/{}", callId,
                    remoteAudioPayloadType,
                    rtpInfo.getRtpPacket().getSyncSource(),
                    rtpInfo.getRtpPacket().getSeqNumber(), rtpInfo.getRtpPacket().getTimestamp()
            );*/
        } else if (rtpInfo.getMediaType().equals(MediaType.VIDEO.getName())) {
            remoteVideoPayloadType = rtpInfo.getRtpPacket().getPayloadType();
            /*log.info("|MediaInfo({})| VIDEO [{}] >>> ({}) {}/{}", callId,
                    remoteVideoPayloadType,
                    rtpInfo.getRtpPacket().getSyncSource(),
                    rtpInfo.getRtpPacket().getSeqNumber(), rtpInfo.getRtpPacket().getTimestamp()
            );*/
        }

        relayToRtspClient(rtpInfo);
    }

    private void relayToRtspClient(RtpInfo rtpInfo) {
        List<Streamer> streamerList = NettyChannelManager.getInstance().getStreamerListByCallId(callId);
        if (streamerList == null || streamerList.isEmpty()) {
            return;
        }

        for (Streamer streamer : streamerList) {
            applyRtpMetaToStreamer(rtpInfo, streamer);

            // CALLBACK
            if (streamer.getPlayResponse() != null) {
                streamer.sendPlayResponse();
            }

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

}
