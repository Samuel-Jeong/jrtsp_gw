package org.kkukie.jrtsp_gw.media.webrtc.websocket.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.media.webrtc.service.model.WebRtcIceInfo;
import org.kkukie.jrtsp_gw.media.webrtc.service.model.WebRtcServiceInfo;
import org.kkukie.jrtsp_gw.media.webrtc.service.model.WebSocketInfo;

import java.io.IOException;

@Slf4j
@Getter
public class WebSocketService {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final WebRtcServiceInfo webRtcServiceInfo;

    private final WebRtcIceInfo webRtcIceInfo;

    private final WebSocketInfo webSocketInfo;

    public WebSocketService() {
        DefaultConfig defaultConfig = ConfigManager.getDefaultConfig();
        this.webRtcServiceInfo = new WebRtcServiceInfo(defaultConfig.getServerUri(), defaultConfig.getApplicationName());
        this.webRtcIceInfo = new WebRtcIceInfo();
        this.webSocketInfo = new WebSocketInfo();
    }

    public void start(String conferenceId) throws WebSocketException, IOException {
        if (conferenceId == null || conferenceId.isEmpty()) { return; }

        webRtcServiceInfo.setConferenceId(conferenceId);
        webSocketInfo.start(webRtcServiceInfo);
        log.debug("|WebSocketService({})| LOCAL NETWORK = {}:{}", conferenceId, webSocketInfo.getLocalIp(), webSocketInfo.getLocalPort());
    }

    public void handshake() {
        // SEND REQUEST_OFFER
        webSocketInfo.handshake(webRtcIceInfo);
    }

    public void stop() {
        webSocketInfo.stop();
    }

}