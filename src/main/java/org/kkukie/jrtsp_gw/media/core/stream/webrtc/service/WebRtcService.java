package org.kkukie.jrtsp_gw.media.core.stream.webrtc.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class WebRtcService {

    private String conferenceId;

    private WebSocketService webSocketService = null;

    private final ReentrantLock handshakeLock = new ReentrantLock();
    private final ReentrantLock clientCountLock = new ReentrantLock();

    private int clientCount = 0;

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
        clientCountLock.lock();
        try {
            log.debug("|WebRtcService({})| [ADD] Current client count: [ {} ]",
                    conferenceId, ++clientCount
            );
        } catch (Exception e) {
            log.warn("|WebRtcService({})| Fail to increase the client count.", conferenceId, e);
        } finally {
            clientCountLock.unlock();
        }
    }

    public int removeClient() {
        if (clientCount <= 0) {
            return 0;
        }

        clientCountLock.lock();
        try {
            log.debug("|WebRtcService({})| [REMOVE] Current client count: [ {} ]",
                    conferenceId, --clientCount
            );
            return clientCount;
        } catch (Exception e) {
            log.warn("|WebRtcService({})| Fail to decrease the client count.", conferenceId, e);
            return clientCount;
        } finally {
            clientCountLock.unlock();
        }
    }
}
