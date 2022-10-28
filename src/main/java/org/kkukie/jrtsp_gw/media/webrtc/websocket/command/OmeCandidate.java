package org.kkukie.jrtsp_gw.media.webrtc.websocket.command;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.WebSocketService;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.model.ice.RTCIceCandidate;

import java.util.List;

@Slf4j
public class OmeCandidate extends AbstractCommand {

    public static final String TYPE = "candidate";

    private final long id;
    private final int peerId;
    private final List<RTCIceCandidate> candidates;

    public OmeCandidate(long id, int peerId, List<RTCIceCandidate> candidates) {
        super(CommandType.CANDIDATE);

        this.id = id;
        this.peerId = peerId;
        this.candidates = candidates;
    }

    @Override
    public String makeJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("peer_id", peerId);
        jsonObject.addProperty("command", TYPE);

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
            jsonObject.add("candidates", candidateArray);
        }

        String result = jsonObject.toString();
        log.debug("OmeCandidate: \n{}", WebSocketService.gson.toJson(jsonObject));

        return result;
    }

}
