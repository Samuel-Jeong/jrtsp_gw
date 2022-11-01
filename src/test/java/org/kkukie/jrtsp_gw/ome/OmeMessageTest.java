package org.kkukie.jrtsp_gw.ome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.config.SdpConfig;
import org.kkukie.jrtsp_gw.config.StunConfig;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.OmeAnswer;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.ice.RTCIceCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ActiveProfiles("dev")
@EnableConfigurationProperties({DefaultConfig.class, SdpConfig.class, DtlsConfig.class, StunConfig.class})
public class OmeMessageTest {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void requestOffer() throws Exception {
        // 1) Given


        // 2) When

        // 3) Then

    }

    @Test
    public void offer() throws Exception {
        // 1) Given


        // 2) When

        // 3) Then

    }

    @Test
    public void answer() throws Exception {
        // 1) Given
        RTCIceCandidate rtcIceCandidate1 = new RTCIceCandidate("123", 0, "ice sdp 1");
        RTCIceCandidate rtcIceCandidate2 = new RTCIceCandidate("456", 0, "ice sdp 2");
        List<RTCIceCandidate> candidates = new ArrayList<>();
        candidates.add(rtcIceCandidate1);
        candidates.add(rtcIceCandidate2);

        // 2) When
        OmeAnswer omeAnswer = new OmeAnswer(
                1234,
                0,
                "sdt test",
                candidates
        );

        // 3) Then
        log.info("omeAnswer: \n{}", gson.toJson(omeAnswer.makeJson()));
    }

    @Test
    public void candiate() throws Exception {
        // 1) Given


        // 2) When

        // 3) Then

    }

}
