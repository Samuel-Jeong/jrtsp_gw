package org.kkukie.jrtsp_gw.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpParser;
import media.core.rtsp.sdp.SdpSession;
import org.apache.commons.net.ntp.TimeStamp;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "sdp")
@Getter
@Setter
@Slf4j
public class SdpConfig extends AbstractConfig {

    private String version;
    private String origin;
    private String session;
    private String time;
    private String connection;
    private String audio;
    private String audioRtpMap;
    private String video;
    private String videoRtpMap;
    private String sdpLocalIp;

    private String[] audioAttributeList;
    private String[] videoAttributeList;

    private static final SdpParser sdpParser = SdpParser.INSTANCE;

    public SdpSession loadLocalSdpConfig(String id, int localPort,
                                         int audioPayloadType, String audioRtpInfo,
                                         int videoPayloadType, String videoRtpInfo) {
        try {
            StringBuilder sdpStr = new StringBuilder();

            // 1) Session
            // 1-1) Version
            String curVersion = "v=" + version + "\r\n";
            sdpStr.append(curVersion);

            // 1-2) Origin
            /*
                - Using NTP Timestamp
                [RFC 4566]
                  <sess-id> is a numeric string such that the tuple of <username>,
                  <sess-id>, <nettype>, <addrtype>, and <unicast-address> forms a
                  globally unique identifier for the session.  The method of
                  <sess-id> allocation is up to the creating tool, but it has been
                  suggested that a Network Time Protocol (NTP) format timestamp be
                  used to ensure uniqueness.
             */
            String originSessionId = String.valueOf(TimeStamp.getCurrentTime().getTime());
            String curOrigin = String.format(this.origin, originSessionId, sdpLocalIp);
            curOrigin = "o=" + curOrigin + "\r\n";
            sdpStr.append(curOrigin);

            // 1-3) Session
            String curSession = "s=" + session + "\r\n";
            sdpStr.append(curSession);

            // 3) Media
            // 3-1) Connection
            String connection = String.format(this.connection, sdpLocalIp);
            connection = "c=" + connection + "\r\n";
            sdpStr.append(connection);

            // 2) Time
            // 2-1) Time
            String curTime = "t=" + time + "\r\n";
            sdpStr.append(curTime);

            // 3) Media
            // 3-2) Media
            if (audio != null && !audio.isEmpty()) {
                sdpStr.append("m=");
                if (audioPayloadType != 0) {
                    sdpStr.append(String.format(this.audio, localPort, audioPayloadType));
                    log.debug("this.audio: {} / audioPayloadType: {} / audioRtpInfo: {}", this.audio, audioPayloadType, audioRtpInfo);
                } else {
                    return null;
                }
                sdpStr.append("\r\n");
            }

            if (audioRtpMap != null && !audioRtpMap.isEmpty()) {
                sdpStr.append("a=");
                sdpStr.append(String.format(this.audioRtpMap, audioPayloadType, audioRtpInfo));
                sdpStr.append("\r\n");
            }

            for (String attribute : audioAttributeList) {
                sdpStr.append("a=");
                sdpStr.append(attribute);
                sdpStr.append("\r\n");
            }

            if (video != null && !video.isEmpty()) {
                sdpStr.append("m=");
                if (videoPayloadType != 0) {
                    sdpStr.append(String.format(this.video, localPort, videoPayloadType));
                    log.debug("this.video: {} / videoPayloadType: {} / videoRtpInfo: {}", this.video, videoPayloadType, videoRtpInfo);
                } else {
                    return null;
                }
                sdpStr.append("\r\n");
            }

            if (videoRtpMap != null && !videoRtpMap.isEmpty()) {
                sdpStr.append("a=");
                sdpStr.append(String.format(this.videoRtpMap, videoPayloadType, videoRtpInfo));
                sdpStr.append("\r\n");
            }

            for (String attribute : videoAttributeList) {
                sdpStr.append("a=");
                sdpStr.append(attribute);
                sdpStr.append("\r\n");
            }

            SdpSession localSdp = null;
            try {
                localSdp = sdpParser.parse(sdpStr.toString());
                log.debug("({}) Local SDP=\n{}", id, localSdp.write());
            } catch (Exception e) {
                log.error("({}) Fail to parse the local sdp. ({})", id, sdpStr, e);
                System.exit(1);
            }
            return localSdp;
        } catch (Exception e) {
            log.warn("Fail to load the local sdp.", e);
            return null;
        }
    }

}
