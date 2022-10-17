package org.kkukie.jrtsp_gw.session.call;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.session.media.MediaInfo;

@Getter
@Setter
@Slf4j
public class CallInfo {

    private final String callId;
    private final String conferenceId;
    private final boolean isHost;

    private MediaInfo mediaInfo = null;

    public CallInfo(String conferenceId, String callId, boolean isHost) {
        this.conferenceId = conferenceId;
        this.callId = callId;
        this.isHost = isHost;
    }

}
