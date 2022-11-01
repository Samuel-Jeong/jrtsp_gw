package org.kkukie.jrtsp_gw.media.core.stream.rtsp.rtcp.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class RtcpFormat {

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    protected RtcpFormat() {}
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        return null;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
