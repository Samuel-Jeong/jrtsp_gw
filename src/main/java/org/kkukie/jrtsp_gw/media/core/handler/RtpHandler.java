package org.kkukie.jrtsp_gw.media.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.dtls.DtlsHandler;
import org.kkukie.jrtsp_gw.media.core.stream.rtcp.RtcpHeader;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.RtpInfo;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.RtpPacket;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandler;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandlerException;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.format.RTPFormat;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.statistics.RtpStatistics;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.module.RtpClock;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class RtpHandler implements PacketHandler {

    private final String conferenceId;

    private final RtpClock rtpClock;
    private final RtpStatistics statistics;
    private final RtpPacket rtpPacket;

    private int pipelinePriority;

    private final boolean loopable;

    private boolean secure;
    private DtlsHandler dtlsHandler;

    private final Map<String, RTPFormats> mediaFormatMap;

    private Consumer<RtpInfo> rtpRecvCallback = whatever -> {};

    public RtpHandler (String conferenceId,
                       RtpClock clock, RtpStatistics statistics,
                       Map<String, RTPFormats> mediaFormatMap) {
        this.conferenceId = conferenceId;

        this.mediaFormatMap = mediaFormatMap;
        this.pipelinePriority = 0;

        this.rtpClock = clock;
        this.statistics = statistics;
        this.rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);

        this.loopable = false;
        this.secure = false;
    }

    @Override
    public void destroy() {
        ByteBuffer packetBuffer = rtpPacket.getBuffer();
        if (packetBuffer != null) {
            packetBuffer.clear();
        }

        dtlsHandler = null;

        for (RTPFormats rtpFormats : mediaFormatMap.values()) {
            if (rtpFormats != null) {
                rtpFormats.clean();
            }
        }
        mediaFormatMap.clear();
    }

    public int getPipelinePriority () {
        return pipelinePriority;
    }

    public void setPipelinePriority (int pipelinePriority) {
        this.pipelinePriority = pipelinePriority;
    }

    public void enableSrtp (final DtlsHandler handler) {
        this.secure = true;
        this.dtlsHandler = handler;
    }

    public void disableSrtp () {
        this.secure = false;
        this.dtlsHandler = null;
    }

    public boolean canHandle (byte[] packet) {
        return canHandle(packet, packet.length, 0);
    }

    public boolean canHandle (byte[] packet, int dataLength, int offset) {
        /*
         * The RTP header has the following format:
         *
         * 0                   1                   2                   3
         * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |                           timestamp                           |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |           synchronization source (SSRC) identifier            |
         * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * |            contributing source (CSRC) identifiers             |
         * |                             ....                              |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *
         * The first twelve octets are present in every RTP packet, while the
         * list of CSRC identifiers is present only when inserted by a mixer.
         *
         * The version defined by RFC3550 specification is two.
         */
        // Packet must be equal or greater than an RTP Packet Header
        if (dataLength >= RtpPacket.FIXED_HEADER_SIZE) {
            // The most significant 2 bits of every RTP message correspond to the version.
            // Currently supported version is 2 according to RFC3550
            byte b0 = packet[offset];
            int b0Int = b0 & 0xff;

            // Differentiate between RTP, STUN and DTLS packets in the pipeline
            // https://tools.ietf.org/html/rfc5764#section-5.1.2
            if (b0Int > 127 && b0Int < 192) {
                int version = (b0 & 0xC0) >> 6;

                if (RtpPacket.VERSION == version) {
                    /*
                     * When RTP and RTCP packets are multiplexed onto a single port, the RTCP packet type field occupies the
                     * same position in the packet as the combination of the RTP marker (M) bit and the RTP payload type (PT).
                     * This field can be used to distinguish RTP and RTCP packets when two restrictions are observed:
                     *
                     * 1) the RTP payload type values used are distinct from the RTCP packet types used.
                     *
                     * 2) for each RTP payload type (PT), PT+128 is distinct from the RTCP packet types used. The first
                     * constraint precludes a direct conflict between RTP payload type and RTCP packet type; the second
                     * constraint precludes a conflict between an RTP data packet with the marker bit set and an RTCP packet.
                     */
                    int type = packet[offset + 1] & 0xff & 0x7f;
                    int rtcpType = type + 128;

                    // RTP payload types 72-76 conflict with the RTCP SR, RR, SDES, BYE,
                    // and APP packets defined in the RTP specification
                    switch (rtcpType) {
                        case RtcpHeader.RTCP_SR:
                        case RtcpHeader.RTCP_RR:
                        case RtcpHeader.RTCP_SDES:
                        case RtcpHeader.RTCP_BYE:
                        case RtcpHeader.RTCP_APP:
                            return false;
                        default:
                            return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public byte[] handle (byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
        return this.handle(packet, packet.length, 0, localPeer, remotePeer);
    }

    @Override
    public byte[] handle (byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
        // Do not handle data while DTLS handshake is ongoing. WebRTC calls only.
        if (this.secure && !this.dtlsHandler.isHandshakeComplete()) {
            return null;
        }

        if (this.secure) {
            // Decode SRTP packet into RTP. WebRTC calls only.
            byte[] decoded = this.dtlsHandler.decodeRTP(packet, offset, dataLength);
            if (decoded == null || decoded.length == 0) {
                log.warn("|RtpHandler({})| SRTP packet is not valid! Dropping packet.", conferenceId);
                return null;
            } else {
                // Transform incoming data directly into an RTP Packet
                ByteBuffer buffer = this.rtpPacket.getBuffer();
                buffer.clear();
                buffer.put(decoded);
                buffer.flip();
            }
        } else {
            // Transform incoming data directly into an RTP Packet
            ByteBuffer buffer = this.rtpPacket.getBuffer();
            buffer.clear();
            buffer.put(packet, offset, dataLength);
            buffer.flip();
        }

        // For RTP keep-alive purposes
        this.statistics.setLastHeartbeat(this.rtpClock.getWallClock().getTime());
        // RTP v0 packets are used in some applications. Discarded since we do not handle them.
        if (rtpPacket.getVersion() != 0) {
            // Queue packet into the jitter buffer
            if (rtpPacket.getBuffer().limit() > 0) {
                if (loopable) {
                    // Update statistics for RTCP
                    this.statistics.onRtpReceive(rtpPacket);
                    this.statistics.onRtpSent(rtpPacket);
                    // Return same packet (looping) so it can be transmitted
                    return packet;
                } else {
                    // Update statistics for RTCP
                    this.statistics.onRtpReceive(rtpPacket);

                    // Write packet
                    String curMediaType = getMediaType(rtpPacket.getPayloadType());
                    if (curMediaType != null && !curMediaType.isEmpty()) {
                        this.onRtpReceive(new RtpInfo(rtpPacket, remotePeer, localPeer, curMediaType));
                    }
                }
            } else {
                log.warn("|RtpHandler({})| Skipping packet because limit of the packets buffer is zero", conferenceId);
            }
        }

        return null;
    }

    private String getMediaType(int payloadType) {
        String curMediaType = null;
        for (Map.Entry<String, RTPFormats> entry : mediaFormatMap.entrySet()) {
            if (entry == null) { continue; }

            String mediaType = entry.getKey();
            if (mediaType == null || mediaType.isEmpty()) { continue; }

            RTPFormats formats = entry.getValue();
            if (formats == null) { continue; }

            RTPFormat rtpFormat = formats.getRTPFormat(payloadType);
            if (rtpFormat != null) {
                curMediaType = mediaType;
                break;
            }
        }
        return curMediaType;
    }

    public int compareTo (PacketHandler o) {
        if (o == null) {
            return 1;
        }
        return this.getPipelinePriority() - o.getPipelinePriority();
    }

    public void onRtpReceive (RtpInfo rtpInfo){
        rtpRecvCallback.accept(rtpInfo);
    }

    public void setRtpRecvCallback (Consumer<RtpInfo> rtpRecvCallback) {
        this.rtpRecvCallback = rtpRecvCallback;
    }

    public RtpStatistics getStatistics() {
        return statistics;
    }

}
