package org.kkukie.jrtsp_gw.media.webrtc.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.ice.IceInfo;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.model.ice.RTCIceCandidate;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Setter
public class WebRtcIceInfo {

    private final IceInfo iceInfo = new IceInfo();
    private int stunServerPort = 0;

    private boolean isRemoteIceTrickle = false;

    private final List<RTCIceCandidate> localCandidates = new ArrayList<>();
    private final List<RTCIceCandidate> remoteCandidates = new ArrayList<>();

}
