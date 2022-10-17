package org.kkukie.jrtsp_gw.media.rtsp.rtcp.type.extended.feedback.transportlayer.base;

public class RtcpTransportLayerFeedbackType {

    /**
     * Assigned in AVPF [RFC4585]:
     * <p>
     * 1:    Generic NACK
     * 31:   reserved for future expansion of the identifier number space
     * <p>
     * Assigned in this memo:
     * <p>
     * 2:    reserved (see note below)
     * 3:    Temporary Maximum Media Stream Bit Rate Request (TMMBR)
     * 4:    Temporary Maximum Media Stream Bit Rate Notification (TMMBN)
     * <p>
     * Available for assignment:
     * <p>
     * 0:    unassigned
     * 5-30: unassigned
     */

    public static final int NACK = 1;
    public static final int TMMBR = 3; // Temporary Maximum Media Stream Bit Rate Request
    public static final int TMMBN = 4; // Temporary Maximum Media Stream Bit Rate Notification

}
