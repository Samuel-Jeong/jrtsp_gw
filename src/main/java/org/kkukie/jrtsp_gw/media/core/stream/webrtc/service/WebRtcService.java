package org.kkukie.jrtsp_gw.media.core.stream.webrtc.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class WebRtcService {

    private String conferenceId;

    private WebSocketService webSocketService = null;

    private final ReentrantLock handshakeLock = new ReentrantLock();

    private final HashSet<String> callInfos;
    private final ReentrantLock callInfoSetLock = new ReentrantLock();

    public WebRtcService() {
        this.callInfos = new HashSet<>();
    }

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

    public void addCall(String id) {
        callInfoSetLock.lock();
        try {
            callInfos.add(id);
        } catch (Exception e) {
            log.warn("|WebRtcService({})| Fail to increase the call count.", conferenceId, e);
        } finally {
            callInfoSetLock.unlock();
            if (callInfos.contains(id)) {
                log.debug("|WebRtcService({})| [ADD:{}] Current call count: [ {} ]",
                        conferenceId, id, callInfos.size()
                );
            }
        }
    }

    public int removeCall(String id) {
        if (!callInfos.contains(id)) {
            return callInfos.size();
        }

        callInfoSetLock.lock();
        try {
            callInfos.remove(id);
            return callInfos.size();
        } catch (Exception e) {
            log.warn("|WebRtcService({})| Fail to decrease the call count.", conferenceId, e);
            return callInfos.size();
        } finally {
            callInfoSetLock.unlock();
            if (!callInfos.contains(id)) {
                log.debug("|WebRtcService({})| [REMOVE:{}] Current call count: [ {} ]",
                        conferenceId, id, callInfos.size()
                );
            }
        }
    }

}

