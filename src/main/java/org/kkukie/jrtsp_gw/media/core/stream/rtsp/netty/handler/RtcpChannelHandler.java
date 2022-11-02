package org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.Streamer;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.NettyChannelManager;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.base.RtcpType;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.packet.RtcpPacket;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.type.regular.RtcpReceiverReport;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.type.regular.base.RtcpHeader;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.type.regular.base.report.RtcpReportBlock;

/**
 * @class public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket>
 */

@Slf4j
public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final String streamerKey;
    private final String name;
    private final String listenIp;
    private final int listenPort;

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpChannelHandler(String streamerKey, String listenIp, int listenPort) {
        this.streamerKey = streamerKey;
        this.name = "RTCP_" + listenIp + ":" + listenPort;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        log.debug("[{}] ({}) RtcpChannelHandler is created. (listenIp={}, listenPort={})", streamerKey, name, listenIp, listenPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private static void handleReceiverReport(RtcpPacket rtcpPacket, Streamer streamer, long ssrc) {
        RtcpReceiverReport rtcpReceiverReport = (RtcpReceiverReport) rtcpPacket.getRtcpFormat();
        RtcpReportBlock rtcpReportBlock = rtcpReceiverReport.getReportBlockBySsrc(ssrc);
        if (rtcpReportBlock != null) {
            float fractionLost = ((float) rtcpReportBlock.getFraction() / 100);
            if (fractionLost >= 0 && fractionLost <= 0.01) {
                streamer.setCongestionLevel(0);
            } else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                streamer.setCongestionLevel(1);
            } else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                streamer.setCongestionLevel(2);
            } else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                streamer.setCongestionLevel(3);
            } else {
                streamer.setCongestionLevel(4);
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            ByteBuf buf = msg.content();
            if (buf == null) {
                return;
            }

            int readBytes = buf.readableBytes();
            if (readBytes <= 0) {
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            //log.debug("({}) data: [{}], readBytes: [{}]", name, ByteUtil.byteArrayToHex(data), readBytes);

            if (data.length >= RtcpHeader.LENGTH) {
                RtcpPacket rtcpPacket = new RtcpPacket(data);
                if (log.isTraceEnabled()) {
                    log.trace("[{}] ({}) {}", streamerKey, name, rtcpPacket);
                }

                int packetType = rtcpPacket.getRtcpHeader().getPacketType();
                if (packetType == RtcpType.RECEIVER_REPORT) {
                    for (Streamer streamer : NettyChannelManager.getInstance().getAllStreamers()) {
                        if (streamer == null) {
                            continue;
                        }

                        long audioSsrc = streamer.getAudioSsrc();
                        if (audioSsrc > 0) {
                            handleReceiverReport(rtcpPacket, streamer, audioSsrc);
                        }

                        long videoSsrc = streamer.getVideoSsrc();
                        if (videoSsrc > 0) {
                            handleReceiverReport(rtcpPacket, streamer, videoSsrc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[{}] ({}) Fail to handle the rtcp Packet.", streamerKey, name, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }
}
