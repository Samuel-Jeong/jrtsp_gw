package org.kkukie.jrtsp_gw.media.rtsp.rtcp.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class RtcpFormat {

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpFormat() {
    }
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
