package org.kkukie.jrtsp_gw.session.media.base;

import lombok.Getter;
import lombok.Setter;
import media.core.rtsp.sdp.SdpMedia;

@Getter
@Setter
public class SdpMediaInfo {

    private SdpMedia audioDesc;
    private SdpMedia videoDesc;
    private SdpMedia applicationDesc;

    private int audioPayloadType;
    private String audioRtpInfo;
    private int videoPayloadType;
    private String videoRtpInfo;

}
