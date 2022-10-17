package org.kkukie.jrtsp_gw.media.rtsp.stream.rtp.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RtpMeta {

    public static final String TRACK_ID_TAG = "trackID";
    public static final String AUDIO_TRACK_ID = "1";
    public static final String VIDEO_TRACK_ID = "2";
    private final AtomicInteger curSeqNum = new AtomicInteger(0);
    private final AtomicLong curTimeStamp = new AtomicLong(0);
    private long ssrc;

    public RtpMeta() {}

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public int getCurSeqNum() {
        return curSeqNum.get();
    }

    public void setCurSeqNum(int curSeqNum) {
        this.curSeqNum.set(curSeqNum);
    }

    public long getCurTimeStamp() {
        return curTimeStamp.get();
    }

    public void setCurTimeStamp(long curTimeStamp) {
        this.curTimeStamp.set(curTimeStamp);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
