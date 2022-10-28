package org.kkukie.jrtsp_gw.random;

import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.SdpMedia;
import media.core.rtsp.sdp.SdpParser;
import media.core.rtsp.sdp.SdpSession;
import org.junit.jupiter.api.Test;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.module.RTCPeerConnectionMaster;

@Slf4j
public class RTCPeerConnectionMasterTest {

    String certPath = "/Users/jamesj/GIT_PROJECTS/UANGEL/urtsp_gw/src/main/resources/config/cert.pem";

    SdpParser sdpParser = SdpParser.INSTANCE;

    String remoteSdp = "v=0\r\n" +
            "o=OvenMediaEngine 101 2 IN IP4 127.0.0.1\r\n" +
            "s=-\r\n" +
            "t=0 0\r\n" +
            "a=ice-ufrag:lBkCdN\r\n" +
            "a=ice-pwd:dUJkHe6WnD9BGqv5wy1c2YAT3jXtSVPx\r\n" +
            "a=fingerprint:sha-256 28:C2:15:E1:99:22:8B:03:4C:FD:37:6D:BB:8F:51:A6:F6:ED:82:D7:D5:11:6C:7E:D6:52:FB:9E:53:5E:B3:29\r\n" +
            "a=ice-options:trickle\r\n" +
            "a=msid-semantic: WMS x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\n" +
            "a=group:BUNDLE Namq79 zqiyfb\r\n" +
            "a=group:LS Namq79 zqiyfb\r\n" +
            "m=video 9 UDP/TLS/RTP/SAVPF 98\r\n" +
            "c=IN IP4 0.0.0.0\r\n" +
            "a=rtpmap:98 H264/90000\r\n" +
            "a=fmtp:98 packetization-mode=1;profile-level-id=42e01f;level-asymmetry-allowed=1\r\n" +
            "a=rtcp-fb:98 goog-remb\r\n" +
            "a=extmap:1 urn:ietf:params:rtp-hdrext:framemarking\r\n" +
            "a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
            "a=setup:actpass\r\n" +
            "a=mid:Namq79\r\n" +
            "a=msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\n" +
            "a=sendonly\r\n" +
            "a=ssrc:2346570055 cname:mhfc98pgYLJuHO4t\r\n" +
            "a=ssrc:2346570055 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\n" +
            "a=ssrc:2346570055 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\n" +
            "a=ssrc:2346570055 label:n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\n" +
            "a=rtcp-mux\r\n" +
            "a=rtcp-rsize\r\n" +
            "m=audio 9 UDP/TLS/RTP/SAVPF 110\r\n" +
            "c=IN IP4 0.0.0.0\r\n" +
            "a=rtpmap:110 OPUS/48000/2\r\n" +
            "a=fmtp:110 sprop-stereo=1;stereo=1;minptime=10;useinbandfec=1\r\n" +
            "a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
            "a=setup:actpass\r\n" +
            "a=mid:zqiyfb\r\n" +
            "a=msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\n" +
            "a=sendonly\r\n" +
            "a=ssrc:1357213020 cname:mhfc98pgYLJuHO4t\r\n" +
            "a=ssrc:1357213020 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\n" +
            "a=ssrc:1357213020 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\n" +
            "a=ssrc:1357213020 label:ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\n" +
            "a=rtcp-mux\r\n" +
            "a=rtcp-rsize";

    @Test
    public void test() {
        // 1) Given
        RTCPeerConnectionMaster rtcPeerConnectionMaster = new RTCPeerConnectionMaster();
        rtcPeerConnectionMaster.setCertPath(certPath);

        // 2) When
        SdpSession remoteSdpSession = sdpParser.parse(remoteSdp);

        rtcPeerConnectionMaster.setRemoteDesc(remoteSdpSession);

        // 3) Then
        SdpSession answer = rtcPeerConnectionMaster.createAnswerSdpSession();
        log.info("ANSWER : \n{}", answer.write());

        for (SdpMedia media : answer.getMedia()) {
            log.info("media: \n{}", media.getMline().getType());
        }

    }

}
