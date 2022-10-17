package org.kkukie.jrtsp_gw.media.core.rtcp;

import java.net.InetSocketAddress;

public class RtcpInfo {

    private final RtcpPacket rtcpPacket;
    private final byte[] data;
    private final boolean audioType;
    private final InetSocketAddress fromAddr;
    private final InetSocketAddress toAddr;

    public RtcpInfo(RtcpPacket rtcpPacket, byte[] data, InetSocketAddress fromAddr, InetSocketAddress toAddr, boolean mediaType) {
        this.rtcpPacket = rtcpPacket;
        this.data = data;
        this.audioType = mediaType;
        this.fromAddr = fromAddr;
        this.toAddr = toAddr;
    }

    public RtcpPacket getRtcpPacket() {
        return rtcpPacket;
    }

    public boolean isAudioType() {
        return audioType;
    }

    public InetSocketAddress getFromAddr() {
        return fromAddr;
    }

    public InetSocketAddress getToAddr() {
        return toAddr;
    }

    public byte[] getBytes(){
        return data;
    }

}
