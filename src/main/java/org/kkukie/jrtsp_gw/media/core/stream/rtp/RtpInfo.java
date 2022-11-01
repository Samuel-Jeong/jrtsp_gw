package org.kkukie.jrtsp_gw.media.core.stream.rtp;

import java.net.InetSocketAddress;

public class RtpInfo {
    private final RtpPacket rtpPacket;
    private final String mediaType;
    private final InetSocketAddress fromAddr;
    private final InetSocketAddress toAddr;

    public RtpInfo(RtpPacket rtpPacket, InetSocketAddress fromAddr, InetSocketAddress toAddr, String mediaType) {
        this.rtpPacket = rtpPacket;
        this.mediaType = mediaType;
        this.fromAddr = fromAddr;
        this.toAddr = toAddr;
    }

    public RtpPacket getRtpPacket() {
        return rtpPacket;
    }

    public String getMediaType() {
        return mediaType;
    }

    public InetSocketAddress getFromAddr() {
        return fromAddr;
    }

    public InetSocketAddress getToAddr() {
        return toAddr;
    }

}
