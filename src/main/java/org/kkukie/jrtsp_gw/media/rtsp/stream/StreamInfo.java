package org.kkukie.jrtsp_gw.media.rtsp.stream;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.kkukie.jrtsp_gw.media.rtsp.base.MediaType;

import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class StreamInfo {

    public static final byte TCP_RTP_MAGIC_NUMBER = 0X24;

    private final MediaType mediaType;
    private final String callId;
    private final String sessionId;
    private final String trackId;
    private String clientUserAgent = null;
    private UdpStream udpStream = null;
    private ChannelHandlerContext rtspChannelContext = null;

}
