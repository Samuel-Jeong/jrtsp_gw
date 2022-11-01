package org.kkukie.jrtsp_gw.session.call.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.WebRtcService;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.media.MediaSession;

import java.util.concurrent.CountDownLatch;

@Getter
@Setter
@Slf4j
public class ConferenceInfo {

    private final String conferenceId;
    private final boolean isHost;

    private WebRtcService webRtcService = null;
    private final CountDownLatch webrtcHandshakeLatch = new CountDownLatch(1);

    private MediaSession mediaSession = null;

    public ConferenceInfo(String conferenceId, boolean isHost) {
        this.conferenceId = conferenceId;
        this.isHost = isHost;
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
