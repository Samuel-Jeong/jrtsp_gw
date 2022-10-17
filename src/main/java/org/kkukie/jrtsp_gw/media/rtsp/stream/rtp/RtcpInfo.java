package org.kkukie.jrtsp_gw.media.rtsp.stream.rtp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

@Data
public class RtcpInfo {

    public static final int RTCP_SR_LIMIT_COUNT = 5;
    private int curRtcpSrCount = 5;
    private int spc = 0;
    private int congestionLevel = 0;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
