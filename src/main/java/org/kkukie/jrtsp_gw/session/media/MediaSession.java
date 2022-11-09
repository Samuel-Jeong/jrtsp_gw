package org.kkukie.jrtsp_gw.session.media;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpMedia;
import media.core.rtsp.sdp.SdpMline;
import media.core.rtsp.sdp.SdpRtp;
import media.core.rtsp.sdp.SdpSession;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.format.RTPFormat;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.session.media.base.MediaType;
import org.kkukie.jrtsp_gw.session.media.base.SdpMediaInfo;
import org.kkukie.jrtsp_gw.session.media.format.FormatFactory;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Objects;

import static org.kkukie.jrtsp_gw.session.media.base.DefaultMediaInfo.*;

@Getter
@Setter
@Slf4j
public class MediaSession {

    private final String conferenceId;
    private SocketAddress localMediaAddress;

    private final SdpSession remoteSdp;
    private final SdpMediaInfo remoteSdpMediaInfo;

    private final HashMap<String, RTPFormats> mediaFormatMap;

    private boolean isSecure = false;
    private boolean isRtcpMux = false;

    public MediaSession(String conferenceId, SdpSession remoteSdp) {
        this.conferenceId = conferenceId;
        this.remoteSdp = remoteSdp;
        this.remoteSdpMediaInfo = new SdpMediaInfo();
        this.mediaFormatMap = new HashMap<>();

        parseRemoteSdp(conferenceId, remoteSdp);
    }

    private void parseRemoteSdp(String conferenceId, SdpSession remoteSdp) {
        if (remoteSdp.getFingerprint() != null) {
            isSecure = true;
        }

        for (SdpMedia media : remoteSdp.getMedia()) {
            SdpMline mediaLine = media.getMline();
            if (mediaLine == null) { continue; }

            if (media.getFingerprint() != null) { isSecure = true; }
            if (media.getRtcpMux() != null) { isRtcpMux = true; }

            if (mediaLine.getType().equals("audio")) {
                parseAudio(conferenceId, media);
            } else if (mediaLine.getType().equals("video")) {
                parseVideo(conferenceId, media);
            } else if (mediaLine.getType().equals("application")) {
                parseApplication(conferenceId, media);
            }
        }

        log.debug("|MediaSession({})| Parsing the remote sdp is done.", conferenceId);
    }

    private void parseAudio(String conferenceId, SdpMedia media) {
        remoteSdpMediaInfo.setAudioDesc(media);
        remoteSdpMediaInfo.setAudioPayloadType(
                Integer.parseInt(
                        Objects.requireNonNull(
                                media.getMline()
                                )
                                .getPayloads().split(" ")[0]
                )
        );

        SdpRtp priorityRtp = media.getRtp().get(0); // 우선순위 코덱 선택
        if (priorityRtp != null) {
            RTPFormats audioFormats = new RTPFormats();
            audioFormats.add(new RTPFormat(
                            (int) priorityRtp.getPayload(),
                            FormatFactory.createAudioFormat(
                                    priorityRtp.getCodec(),
                                    priorityRtp.getRate() != null? priorityRtp.getRate().intValue() : DEFAULT_AUDIO_SAMPLE_RATE,
                                    DEFAULT_AUDIO_SAMPLE_SIZE, DEFAULT_AUDIO_CHANNEL_SIZE
                            )
                    )
            );
            mediaFormatMap.putIfAbsent(MediaType.AUDIO.getName(), audioFormats);
            remoteSdpMediaInfo.setAudioRtpInfo(getRtpInfo(priorityRtp));
            log.debug("|MediaSession({})| priorityRtp: {}, remoteAudioRtpInfo: {}", conferenceId, priorityRtp, remoteSdpMediaInfo.getAudioRtpInfo());
        } else {
            log.warn("|MediaSession({})| Fail to get the priority rtp info. (media-line={})", conferenceId, media.getMline());
        }

        log.debug("|MediaSession({})| remoteAudioDesc.getMid: {}", conferenceId, remoteSdpMediaInfo.getAudioDesc().getMid() != null ? remoteSdpMediaInfo.getAudioDesc().getMid().getValue() : null);
    }

    private void parseVideo(String conferenceId, SdpMedia media) {
        remoteSdpMediaInfo.setVideoDesc(media);
        remoteSdpMediaInfo.setVideoPayloadType(
                Integer.parseInt(
                        Objects.requireNonNull(
                                        media.getMline()
                                )
                                .getPayloads().split(" ")[0]
                )
        );

        SdpRtp priorityRtp = media.getRtp().get(0); // 우선순위 코덱 선택
        if (priorityRtp != null) {
            RTPFormats videoFormats = new RTPFormats();
            videoFormats.add(
                    new RTPFormat(
                            (int) priorityRtp.getPayload(),
                            FormatFactory.createVideoFormat(priorityRtp.getCodec()),
                            priorityRtp.getRate() != null? priorityRtp.getRate().intValue() : DEFAULT_VIDEO_SAMPLE_RATE
                    )
            );
            mediaFormatMap.putIfAbsent(MediaType.VIDEO.getName(), videoFormats);
            remoteSdpMediaInfo.setVideoRtpInfo(getRtpInfo(priorityRtp));
            log.debug("|MediaSession({})| priorityRtp: {}, remoteVideoRtpInfo: {}", conferenceId, priorityRtp, remoteSdpMediaInfo.getVideoRtpInfo());
        } else {
            log.warn("|MediaSession({})| Fail to get the priority rtp info. (media-line={})", conferenceId, media.getMline());
        }

        log.debug("|MediaSession({})| remoteVideoDesc.getMid: {}", conferenceId, remoteSdpMediaInfo.getVideoDesc().getMid() != null ? remoteSdpMediaInfo.getVideoDesc().getMid().getValue() : null);
    }

    private void parseApplication(String conferenceId, SdpMedia media) {
        remoteSdpMediaInfo.setApplicationDesc(media);

        log.debug("|MediaSession({})|remoteApplicationDesc.getMid: {}", conferenceId,
                remoteSdpMediaInfo.getApplicationDesc().getMid() != null ? remoteSdpMediaInfo.getApplicationDesc().getMid().getValue() : null
        );
    }

    private String getRtpInfo(SdpRtp priorityRtp) {
        return priorityRtp.getCodec()
                + "/" + priorityRtp.getRate()
                + (priorityRtp.getEncoding() != null ? "/" + priorityRtp.getEncoding() : "");
    }

}
