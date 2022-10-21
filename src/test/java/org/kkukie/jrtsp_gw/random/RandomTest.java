package org.kkukie.jrtsp_gw.random;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.module.FingerPrintGen;
import org.kkukie.jrtsp_gw.util.StringManager;

@Slf4j
public class RandomTest {

    @Test
    public void test() {
        // 1) Given
        int length = 24;

        // 2) When
        String randomString = StringManager.getRandomString(length);

        // 3) Then
        log.info("RandomString: [ {} ]", randomString);
    }

    @Test
    public void fingerPrint() throws Exception {
        // 1) Given
        String certPath = "/Users/jamesj/GIT_PROJECTS/UANGEL/urtsp_gw/src/main/resources/config/cert.pem";

        // 2) When
        String fingerPrint = FingerPrintGen.getFingerPrint(certPath);

        // 3) Then
        log.info("FingerPrint: [ {} ]", fingerPrint);
    }

}