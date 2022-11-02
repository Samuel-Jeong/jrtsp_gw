package org.kkukie.jrtsp_gw.random;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.module.FingerPrintGenerator;
import org.kkukie.jrtsp_gw.util.RandomManager;

@Slf4j
public class RandomTest {

    @Test
    public void test() {
        // 1) Given
        int length = 24;

        // 2) When
        String randomString = RandomManager.getRandomString(length);

        // 3) Then
        log.info("RandomString: [ {} ]", randomString);
    }

    @Test
    public void fingerPrint() throws Exception {
        // 1) Given
        String certPath = "/Users/jamesj/GIT_PROJECTS/jrtsp_gw/src/main/resources/dtls/cert.pem";

        // 2) When
        String fingerPrint = FingerPrintGenerator.getFingerPrint(certPath);

        // 3) Then
        log.info("FingerPrint: [ {} ]", fingerPrint);
    }

}
