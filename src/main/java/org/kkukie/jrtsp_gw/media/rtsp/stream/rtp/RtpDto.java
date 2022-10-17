package org.kkukie.jrtsp_gw.media.rtsp.stream.rtp;

import lombok.Data;
import org.kkukie.jrtsp_gw.media.rtp.RtpPacket;

@Data
public class RtpDto {

    private final RtpPacket rtpPacket;
    private final String mediaType;

}
