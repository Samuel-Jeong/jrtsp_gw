package org.kkukie.jrtsp_gw.media.core.stream.webrtc.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class WebRtcService {

    private final String conferenceId;

    private WebSocketService webSocketService = null;

    private final ReentrantLock handshakeLock = new ReentrantLock();
    private final CountDownLatch handshakeLatch = new CountDownLatch(1);
    private static final int WEBRTC_HANDSHAKE_TIMEOUT_VALUE = 2;
    private static final TimeUnit WEBRTC_HANDSHAKE_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final HashSet<String> callInfos;
    private final ReentrantLock callInfoSetLock = new ReentrantLock();

    public WebRtcService(String conferenceId) {
        this.conferenceId = conferenceId;
        this.callInfos = new HashSet<>();
    }

    public void initWebSocketService() {
        try {
            if (webSocketService == null) {
                webSocketService = new WebSocketService();
                webSocketService.start(conferenceId);
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
            handshakeLatch.countDown();
        }
    }

    public boolean waitHandshake() {
        try {
            if (handshakeLatch.getCount() > 0) {
                log.debug("|WebRtcService({})| Waiting handshaking... (timeout=[{}]{})", conferenceId, WEBRTC_HANDSHAKE_TIMEOUT_VALUE, WEBRTC_HANDSHAKE_TIMEOUT_UNIT);
            } else {
                return true;
            }

            if (!handshakeLatch.await(WEBRTC_HANDSHAKE_TIMEOUT_VALUE, WEBRTC_HANDSHAKE_TIMEOUT_UNIT)) {
                log.warn("|WebRtcService({})| Timeout! Fail to wait the handshakeLatch.", conferenceId);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            log.warn("|WebRtcService({})| Fail to wait the handshakeLatch.", conferenceId, e);
            return false;
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

    public void removeAllCalls() {
        callInfoSetLock.lock();
        try {
            callInfos.clear();
        } catch (Exception e) {
            log.warn("|WebRtcService({})| Fail to decrease the call count.", conferenceId, e);
        } finally {
            callInfoSetLock.unlock();
            if (callInfos.isEmpty()) {
                log.debug("|WebRtcService({})| [REMOVE ALL]", conferenceId);
            }
        }
    }

}
