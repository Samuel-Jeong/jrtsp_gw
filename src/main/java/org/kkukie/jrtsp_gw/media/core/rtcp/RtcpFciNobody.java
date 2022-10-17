package org.kkukie.jrtsp_gw.media.core.rtcp;

/**
 *
 * @author kangmoo Heo
 */
public class RtcpFciNobody implements RtcpFci{

    @Override
    public int decode(byte[] rawData, int offSet) {
        return offSet;
    }

    @Override
    public int encode(byte[] rawData, int offSet) {
        return offSet;
    }

    @Override
    public String toString() {
        return "";
    }
}
