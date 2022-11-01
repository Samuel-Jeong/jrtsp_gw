package org.kkukie.jrtsp_gw.media.core.stream.rtcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author kangmoo Heo
 */
public class RtcpPsFb extends RtcpHeader {
    public static final int PSFB_PLI = 1;
    public static final int PSFB_SLI = 2;
    public static final int PSFB_RPSI = 3;
    public static final int PSFB_FIR = 4;
    public static final int PSFB_AFB = 15;

    protected long senderSsrc;
    protected long mediaSsrc;
    protected List<RtcpFci> rtcpFcis;


    // RFC 4585: Feedback format.
    // Common packet format:
    //
    //    0                   1                   2                   3
    //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    //   |V=2|P|   FMT   |       PT      |          length               |
    //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // 0 |                  SSRC of packet sender                        |
    //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // 4 |                  SSRC of media source                         |
    //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    //   :            Feedback Control Information (FCI)                 :
    //   :                                                               :
    public RtcpPsFb() {
        super();
    }

    public RtcpPsFb(boolean padding, int fmt, long senderSsrc, long mediaSsrc) {
        super(padding, RTCP_PSFB);
        this.count = fmt;
        this.senderSsrc = senderSsrc;
        this.mediaSsrc = mediaSsrc;
        this.rtcpFcis = new ArrayList<>(RtcpPacket.MAX_SOURCES);
    }

    public long getSenderSsrc() {
        return this.senderSsrc;
    }

    public void addRtcpFci(RtcpFci... rtcpFci){
        rtcpFcis.addAll(Arrays.asList(rtcpFci));
    }

    public int decode(byte[] rawData, int offSet){
        int tmp = offSet;

        offSet = super.decode(rawData, offSet);

        this.senderSsrc |= rawData[offSet++] & 0xFF;
        this.senderSsrc <<= 8;
        this.senderSsrc |= rawData[offSet++] & 0xFF;
        this.senderSsrc <<= 8;
        this.senderSsrc |= rawData[offSet++] & 0xFF;
        this.senderSsrc <<= 8;
        this.senderSsrc |= rawData[offSet++] & 0xFF;

        this.mediaSsrc |= rawData[offSet++] & 0xFF;
        this.mediaSsrc <<= 8;
        this.mediaSsrc |= rawData[offSet++] & 0xFF;
        this.mediaSsrc <<= 8;
        this.mediaSsrc |= rawData[offSet++] & 0xFF;
        this.mediaSsrc <<= 8;
        this.mediaSsrc |= rawData[offSet++] & 0xFF;

        while ((offSet - tmp) < this.length) {
            RtcpFci rtcpFci;
            switch(count){
                case PSFB_PLI:
                    rtcpFci = new RtcpFciNobody();
                    offSet = rtcpFci.decode(rawData, offSet);
                    this.rtcpFcis.add(rtcpFci);
                    break;
                case PSFB_SLI:
                    break;
                case PSFB_RPSI:
                    break;
                case PSFB_FIR:
                    rtcpFci = new RtcpFciFir();
                    offSet = rtcpFci.decode(rawData, offSet);
                    this.rtcpFcis.add(rtcpFci);
                    break;
                case PSFB_AFB:
                    break;
                default:
                    break;
            }
        }

        return offSet;
    }

    public int encode(byte[] rawData, int offSet){
        int startPosition = offSet;
        offSet = super.encode(rawData, offSet);

        rawData[offSet++] = ((byte) ((this.senderSsrc & 0xFF000000) >> 24));
        rawData[offSet++] = ((byte) ((this.senderSsrc & 0x00FF0000) >> 16));
        rawData[offSet++] = ((byte) ((this.senderSsrc & 0x0000FF00) >> 8));
        rawData[offSet++] = ((byte) (this.senderSsrc & 0x000000FF));

        rawData[offSet++] = ((byte) ((this.mediaSsrc & 0xFF000000) >> 24));
        rawData[offSet++] = ((byte) ((this.mediaSsrc & 0x00FF0000) >> 16));
        rawData[offSet++] = ((byte) ((this.mediaSsrc & 0x0000FF00) >> 8));
        rawData[offSet++] = ((byte) (this.mediaSsrc & 0x000000FF));

        for(RtcpFci rtcpFci : rtcpFcis){
            if(rtcpFci != null){
                offSet = rtcpFci.encode(rawData, offSet);
            } else {
                break;
            }
        }

        this.length = (offSet - startPosition - 4) / 4;
        rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
        rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

        return offSet;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Payload-Specific Feedback Message:\n");
        builder.append("version=").append(this.version).append(", ");
        builder.append("padding=").append(this.padding).append(", ");
        builder.append("format=").append(this.count).append(", ");
        builder.append("packet type=").append(this.packetType).append(", ");
        builder.append("length=").append(this.length).append(", ");
        builder.append("ssrc=").append(this.senderSsrc).append(", ");
        for(RtcpFci fci : this.rtcpFcis){
            builder.append("\n").append(fci.toString());
        }
        return builder.toString();
    }
}
