package org.kkukie.jrtsp_gw.session.call.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.WebRtcService;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.media.MediaSession;

@Getter
@Setter
@Slf4j
public class ConferenceInfo {

    private final String conferenceId;
    private final boolean isHost;

    private WebRtcService webRtcService = null;

    private MediaSession mediaSession = null;

    public ConferenceInfo(String conferenceId, boolean isHost) {
        this.conferenceId = conferenceId;
        this.isHost = isHost;
    }

    public void startWebRtcService() {
        if (webRtcService == null) {
            webRtcService = new WebRtcService(conferenceId);
            webRtcService.initWebSocketService();
            webRtcService.handshake();
        }
    }

    public void stopWebRtcService() {
        if (webRtcService != null) {
            webRtcService.removeAllCalls();
            webRtcService.disposeWebSocketService();
            webRtcService = null;
        }
    }

    public boolean waitWebRtcPrepared() {
        return webRtcService.waitHandshake();
    }

    public void addCall(String id) {
        if (webRtcService != null) {
            webRtcService.addCall(id);
        } else {
            log.warn("|ConferenceInfo({})| WebRtcService is not exist. Fail to add the call. (id={})", conferenceId, id);
        }
    }

    public void removeCall(String id) {
        if (webRtcService != null && webRtcService.removeCall(id) == 0) {
            log.debug("|ConferenceInfo({})| WebRtcService has no more client. Finishing this conference...", conferenceId);
            webRtcService.disposeWebSocketService();
            ConferenceMaster.getInstance().deleteConference(conferenceId);
        }
    }
}
