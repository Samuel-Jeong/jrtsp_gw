package org.kkukie.jrtsp_gw.media.rtsp.stream.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;

@Data
@NoArgsConstructor
public class TargetNetworkInfo {

    transient InetSocketAddress rtpTargetAddress = null;
    transient InetSocketAddress rtcpTargetAddress = null;
    private String uri = null;
    private String destIp = null;
    private int rtpDestPort = 0; // rtp destination port
    private int rtcpDestPort = 0; // rtcp destination port
    private transient Channel rtpDestChannel = null; /* 메시지 송신용 채널 */
    private transient Channel rtcpDestChannel = null; /* 메시지 송신용 채널 */

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
