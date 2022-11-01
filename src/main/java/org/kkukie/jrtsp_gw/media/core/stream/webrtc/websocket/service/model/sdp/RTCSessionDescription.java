package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.sdp;

import java.util.Objects;

public class RTCSessionDescription {

    public final RTCSdpType sdpType;
    public final String sdp;

    public RTCSessionDescription(RTCSdpType sdpType, String sdp) {
        this.sdpType = sdpType;
        this.sdp = sdp;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.sdp, this.sdpType});
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            RTCSessionDescription other = (RTCSessionDescription)obj;
            return Objects.equals(this.sdp, other.sdp) && this.sdpType == other.sdpType;
        }
    }

    public String toString() {
        return String.format("%s@%d [sdpType=%s, sdp=%s]", RTCSessionDescription.class.getSimpleName(), this.hashCode(), this.sdpType, this.sdp);
    }

}

