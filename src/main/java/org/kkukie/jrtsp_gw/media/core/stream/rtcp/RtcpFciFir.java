package org.kkukie.jrtsp_gw.media.core.stream.rtcp;

/**
 *
 * @author kangmoo Heo
 */
public class RtcpFciFir implements RtcpFci {

    long ssrc = 0;
    int seqNo = 0;

    public RtcpFciFir(long ssrc, int seqNo) {
        this.ssrc = ssrc;
        this.seqNo = seqNo;
    }

    public RtcpFciFir() {
    }

    @Override
    public int decode(byte[] rawData, int offSet) {
        this.ssrc |= rawData[offSet++] & 0xFF;
        this.ssrc <<= 8;
        this.ssrc |= rawData[offSet++] & 0xFF;
        this.ssrc <<= 8;
        this.ssrc |= rawData[offSet++] & 0xFF;
        this.ssrc <<= 8;
        this.ssrc |= rawData[offSet++] & 0xFF;

        this.seqNo |= rawData[offSet++] & 0xFF;
        offSet += 3;
        return offSet;
    }

    @Override
    public int encode(byte[] rawData, int offSet) {
        rawData[offSet++] = ((byte) ((this.ssrc & 0xFF000000) >> 24));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x00FF0000) >> 16));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x0000FF00) >> 8));
        rawData[offSet++] = ((byte) (this.ssrc & 0x000000FF));

        rawData[offSet++] = (byte) (this.seqNo & 0xFF);
        offSet += 3;
        return offSet;
    }

    @Override
    public String toString() {
        return "FIR FCI:\nssrc=" + ssrc + ", seqNo=" + seqNo;
    }
}
