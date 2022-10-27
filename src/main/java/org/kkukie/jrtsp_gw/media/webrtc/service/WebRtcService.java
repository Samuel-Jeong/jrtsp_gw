package org.kkukie.jrtsp_gw.media.webrtc.service;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.WebSocketService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class WebRtcService {

    private String conferenceId;

    private WebSocketService webSocketService = null;

    private final ReentrantLock handshakeLock = new ReentrantLock();

    private final AtomicInteger clientCount = new AtomicInteger(0);

    public void initWebSocketService(String conferenceId) {
        try {
            if (webSocketService == null) {
                webSocketService = new WebSocketService();
                webSocketService.start(conferenceId);
                this.conferenceId = conferenceId;
            }
        } catch (Exception e) {
            log.error("|WebRtcService({})| init.Exception", conferenceId, e);
        }
    }

    public void disposeWebSocketService() {
        try {
            if (webSocketService != null) {
                webSocketService.stop();
                webSocketService = null;
            }
        } catch (Exception e) {
            log.error("|WebRtcService({})| dispose.Exception", conferenceId, e);
        }
    }

    public void handshake() {
        handshakeLock.lock();
        try {
            webSocketService.handshake();

            // WebRTC Handshaking 되기 위한 충분한 시간 확보
            Thread.sleep(1000);
        } catch (Exception e) {
            log.error("|WebRtcService({})| handshake.Exception", conferenceId, e);
        } finally {
            handshakeLock.unlock();
        }
    }

    public void addClient() {
        int clientCount = this.clientCount.incrementAndGet();
        log.debug("|WebRtcService({})| [ADD] Current client count: [ {} ]", conferenceId, clientCount);
    }

    public int removeClient() {
        int clientCount = this.clientCount.decrementAndGet();
        log.debug("|WebRtcService({})| [REMOVE] Current client count: [ {} ]", conferenceId, clientCount);
        return clientCount;
    }
    
}
