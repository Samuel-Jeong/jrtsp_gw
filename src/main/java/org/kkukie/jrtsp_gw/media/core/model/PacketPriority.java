package org.kkukie.jrtsp_gw.media.core.model;

public class PacketPriority {

    private PacketPriority() {}

    public static final int RTP_PRIORITY = 4; // a packet each 20ms
    public static final int STUN_PRIORITY = 3; // a packet each 400ms
    public static final int RTCP_PRIORITY = 2; // a packet each 5s
    public static final int DTLS_PRIORITY = 1; // only for handshake

}
