package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.model.ice;

import lombok.Data;

@Data
public class IceInfo {

    private String localIceUfrag;
    private String localIcePasswd;
    private String remoteIceUfrag;
    private String remoteIcePasswd;

    private String remoteUsername;

}
