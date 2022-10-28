package org.kkukie.jrtsp_gw.session.call.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.webrtc.service.WebRtcService;
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

    public void addCall() {
        if (webRtcService != null) {
            webRtcService.addClient();
        }
    }

    public void removeCall() {
        if (webRtcService != null && webRtcService.removeClient() == 0) {
            log.debug("|ConferenceInfo({})| WebRtcService has no more client. Finishing this conference...", conferenceId);
            webRtcService.disposeWebSocketService();
            ConferenceMaster.getInstance().deleteConference(conferenceId);
        }
    }

}
