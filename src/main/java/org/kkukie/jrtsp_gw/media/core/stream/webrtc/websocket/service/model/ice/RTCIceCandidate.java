package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.ice;

public class RTCIceCandidate {

    public final String sdpMid;
    public final int sdpMLineIndex;
    public final String sdp;
    public final String serverUrl;

    public RTCIceCandidate(String sdpMid, int sdpMLineIndex, String sdp) {
        this(sdpMid, sdpMLineIndex, sdp, (String)null);
    }

    public RTCIceCandidate(String sdpMid, int sdpMLineIndex, String sdp, String serverUrl) {
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdp = sdp;
        this.serverUrl = serverUrl;
    }

    public String toString() {
        return String.format("%s@%d [sdpMid=%s, sdpMLineIndex=%s, sdp=%s, serverUrl=%s]", RTCIceCandidate.class.getSimpleName(), this.hashCode(), this.sdpMid, this.sdpMLineIndex, this.sdp, this.serverUrl);
    }

}

