package org.kkukie.jrtsp_gw.media.core.stream.rtcp;

/**
 *
 * @author kangmoo Heo
 */
public interface RtcpFci {
    int decode(byte[] rawData, int offSet);
    int encode(byte[] rawData, int offSet);
}
