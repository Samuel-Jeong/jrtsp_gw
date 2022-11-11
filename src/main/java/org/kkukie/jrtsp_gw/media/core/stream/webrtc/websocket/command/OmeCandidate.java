package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.ice.RTCIceCandidate;

import java.util.List;

@Slf4j
public class OmeCandidate extends AbstractCommand {

    public OmeCandidate(long id, int peerId, List<RTCIceCandidate> candidates) {
        super(CommandType.CANDIDATE);

        getJsonObject().addProperty("id", id);
        getJsonObject().addProperty("peer_id", peerId);

        if (candidates != null && !candidates.isEmpty()) {
            JsonArray candidateArray = new JsonArray();
            for (RTCIceCandidate candidate : candidates) {
                if (candidate == null) {
                    continue;
                }

                JsonObject candidateObject = new JsonObject();
                if (candidate.sdp != null && !candidate.sdp.isEmpty()) {
                    candidateObject.addProperty("candidate", candidate.sdp);
                }
                if (candidate.sdpMid != null && !candidate.sdpMid.isEmpty()) {
                    candidateObject.addProperty("sdpMid", candidate.sdpMid);
                }
                candidateObject.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
                candidateArray.add(candidateObject);
            }
            getJsonObject().add("candidates", candidateArray);
        }

    }

    @Override
    public String makeJson() {
        String result = getJsonObject().toString();
        log.debug("OmeCandidate: \n{}", WebSocketService.gson.toJson(getJsonObject()));
        return result;
    }

}
