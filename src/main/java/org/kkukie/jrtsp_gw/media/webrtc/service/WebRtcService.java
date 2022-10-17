package org.kkukie.jrtsp_gw.media.webrtc.service;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.WebSocketService;

@Slf4j
public class WebRtcService {

    private String callId;

    private WebSocketService webSocketService = null;

    public void initWebSocketService(String callId) {
        try {
            if (webSocketService == null) {
                webSocketService = new WebSocketService();
                webSocketService.start(callId);
                this.callId = callId;
            }
        } catch (Exception e) {
            log.error("|WebRtcService({})| init.Exception", callId, e);
        }
    }

    public void handshake() {
        try {
            webSocketService.handshake();

            // WebRTC Handshaking 되기 위한 충분한 시간 확보
            Thread.sleep(1000);
        } catch (Exception e) {
            log.error("|WebRtcService({})| handshake.Exception", callId, e);
        }
    }

    public void disposeWebSocketService() {
        try {
            if (webSocketService != null) {
                webSocketService.stop();
                webSocketService = null;
            }
        } catch (Exception e) {
            log.error("|WebRtcService({})| dispose.Exception", callId, e);
        }
    }
    
}
