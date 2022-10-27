package org.kkukie.jrtsp_gw.session.media;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpMedia;
import media.core.rtsp.sdp.SdpMline;
import media.core.rtsp.sdp.SdpRtp;
import media.core.rtsp.sdp.SdpSession;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormat;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.session.format.FormatFactory;

import java.net.SocketAddress;
import java.util.HashMap;

@Getter
@Setter
@Slf4j
public class MediaSession {

    private final String conferenceId;
    private SocketAddress localMediaAddress;

    private final SdpSession remoteSdp;
    private final SdpMediaInfo remoteSdpMediaInfo;

    private final HashMap<String, RTPFormats> mediaFormatMap = new HashMap<>();

    private boolean isSecure = false;
    private boolean isRtcpMux = false;

    public MediaSession(String conferenceId, SdpSession remoteSdp) {
        this.conferenceId = conferenceId;

        this.remoteSdp = remoteSdp;
        this.remoteSdpMediaInfo = new SdpMediaInfo();

        parseRemoteSdp(conferenceId, remoteSdp);
    }

    private void parseRemoteSdp(String conferenceId, SdpSession remoteSdp) {
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
                remoteSdpMediaInfo.setAudioDesc(media);
                remoteSdpMediaInfo.setAudioPayloadType(Integer.parseInt(media.getMline().getPayloads().split(" ")[0]));
                SdpRtp priorityRtp = media.getRtp().get(0);
                if (priorityRtp != null) {
                    RTPFormats audioFormats = new RTPFormats();
                    audioFormats.add(new RTPFormat(
                                    (int) priorityRtp.getPayload(),
                                    FormatFactory.createAudioFormat(
                                            priorityRtp.getCodec(),
                                            priorityRtp.getRate() != null? priorityRtp.getRate().intValue() : 48000,
                                            8, 2
                                    )
                            )
                    );
                    mediaFormatMap.putIfAbsent(MediaType.AUDIO.getName(), audioFormats);
                    remoteSdpMediaInfo.setAudioRtpInfo(
                            priorityRtp.getCodec() + "/"
                                    + priorityRtp.getRate().toString()
                                    + (priorityRtp.getEncoding() != null? "/" + priorityRtp.getEncoding() : "")
                    );
                    log.debug("|MediaSession({})| priorityRtp: {}, remoteAudioRtpInfo: {}", conferenceId, priorityRtp, remoteSdpMediaInfo.getAudioRtpInfo());
                } else {
                    log.warn("|MediaSession({})| Fail to get the priority rtp info. (media-line={})", conferenceId, media.getMline());
                }
                log.debug("|MediaSession({})| remoteAudioDesc.getMid: {}", conferenceId, remoteSdpMediaInfo.getAudioDesc().getMid() != null ? remoteSdpMediaInfo.getAudioDesc().getMid().getValue() : null);
            } else if (mediaLine.getType().equals("video")) {
                remoteSdpMediaInfo.setVideoDesc(media);
                remoteSdpMediaInfo.setVideoPayloadType(Integer.parseInt(media.getMline().getPayloads().split(" ")[0]));
                SdpRtp priorityRtp = media.getRtp().get(0);
                if (priorityRtp != null) {
                    RTPFormats videoFormats = new RTPFormats();
                    videoFormats.add(
                            new RTPFormat(
                                    (int) priorityRtp.getPayload(),
                                    FormatFactory.createVideoFormat(priorityRtp.getCodec()),
                                    priorityRtp.getRate() != null? priorityRtp.getRate().intValue() : 90000
                            )
                    );
                    mediaFormatMap.putIfAbsent(MediaType.VIDEO.getName(), videoFormats);
                    remoteSdpMediaInfo.setVideoRtpInfo(
                            priorityRtp.getCodec() + "/"
                                    + priorityRtp.getRate().toString()
                                    + (priorityRtp.getEncoding() != null? "/" + priorityRtp.getEncoding() : "")
                    );
                    log.debug("|MediaSession({})| priorityRtp: {}, remoteVideoRtpInfo: {}", conferenceId, priorityRtp, remoteSdpMediaInfo.getVideoRtpInfo());
                } else {
                    log.warn("|MediaSession({})| Fail to get the priority rtp info. (media-line={})", conferenceId, media.getMline());
                }
                log.debug("|MediaSession({})| remoteVideoDesc.getMid: {}", conferenceId, remoteSdpMediaInfo.getVideoDesc().getMid() != null ? remoteSdpMediaInfo.getVideoDesc().getMid().getValue() : null);
            } else if (mediaLine.getType().equals("application")) {
                remoteSdpMediaInfo.setApplicationDesc(media);
                log.debug("|MediaSession({})|remoteApplicationDesc.getMid: {}", conferenceId,
                        remoteSdpMediaInfo.getApplicationDesc().getMid() != null ? remoteSdpMediaInfo.getApplicationDesc().getMid().getValue() : null
                );
            }
        }
    }

}
