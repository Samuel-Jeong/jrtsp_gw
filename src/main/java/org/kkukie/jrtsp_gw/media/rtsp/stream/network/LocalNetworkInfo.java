package org.kkukie.jrtsp_gw.media.rtsp.stream.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LocalNetworkInfo {

    private final String listenIp;
    private final int listenPort;
    private final boolean isTcp;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
