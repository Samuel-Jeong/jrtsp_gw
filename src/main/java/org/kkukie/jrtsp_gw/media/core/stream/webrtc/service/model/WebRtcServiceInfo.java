package org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class WebRtcServiceInfo {

    private final String uri;
    private final String applicationName;

    private String conferenceId = null;

    private long id;
    private int peerId;
    private String localSdp;
    private String remoteSdp;

}
