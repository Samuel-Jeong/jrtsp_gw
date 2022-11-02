package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;

@Slf4j
public class OmeRequestOffer extends AbstractCommand {

    public static final String TYPE = "request_offer";

    public OmeRequestOffer() {
        super(CommandType.REQUEST_OFFER);
    }

    @Override
    public String makeJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", TYPE);

        String result = jsonObject.toString();
        log.debug("OmeRequestOffer: \n{}", WebSocketService.gson.toJson(jsonObject));

        return result;
    }

}
