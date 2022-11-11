package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.ice.RTCIceCandidate;

import java.util.List;

/**
 * OFFER >
 * {
 * "candidates":
 *      [
 *      {"candidate":"candidate:0 1 UDP 50 222.235.208.2 30005 typ host","sdpMLineIndex":0},
 *      ...
 *      ],
 * "code":200,
 * "command":"offer",
 * "id":1278941828,
 * "peer_id":0,
 * "sdp":
 *      {
 *      "sdp":"v=0\r\no=OvenMediaEngine ...",
 *      "type":"offer"
 *      }
 * }
 */

/**
 * < ANSWER
 * {
 * "id":1278941828,
 * "peer_id":0,
 * "command":"answer",
 * "sdp":
 * {
 * "type":"answer",
 * "sdp":"v=0\r\no=- ..."
 * }
 * }
 */

@Slf4j
public class OmeAnswer extends AbstractCommand {

    public OmeAnswer(long id, int peerId, String sdp, List<RTCIceCandidate> candidates) {
        super(CommandType.ANSWER);

        getJsonObject().addProperty("id", id);
        getJsonObject().addProperty("peer_id", peerId);

        JsonObject sdpObject = new JsonObject();
        sdpObject.addProperty("type", getType().getName());
        sdpObject.addProperty("sdp", sdp);
        getJsonObject().add("sdp", sdpObject);

        if (candidates != null && !candidates.isEmpty()) {
            JsonArray candidateArray = new JsonArray();
            for (RTCIceCandidate candidate : candidates) {
                if (candidate == null) {
                    continue;
                }

                JsonObject candidateObject = new JsonObject();
                if (candidate.sdpMid != null && !candidate.sdpMid.isEmpty()) {
                    candidateObject.addProperty("sdpMid", candidate.sdpMid);
                }
                candidateObject.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
                if (candidate.sdp != null && !candidate.sdp.isEmpty()) {
                    candidateObject.addProperty("candidate", candidate.sdp);
                }
                candidateArray.add(candidateObject);
            }
            getJsonObject().add("candidates", candidateArray);
        }
    }

    @Override
    public String makeJson() {
        String result = getJsonObject().toString();
        log.debug("OmeAnswer: \n{}", WebSocketService.gson.toJson(getJsonObject()));
        return result;
    }

}
