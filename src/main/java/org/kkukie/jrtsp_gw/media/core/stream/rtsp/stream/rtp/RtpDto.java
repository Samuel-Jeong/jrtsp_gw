package org.kkukie.jrtsp_gw.media.core.stream.rtsp.stream.rtp;

import lombok.Data;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.RtpPacket;

@Data
public class RtpDto {

    private final RtpPacket rtpPacket;
    private final String mediaType;

}
