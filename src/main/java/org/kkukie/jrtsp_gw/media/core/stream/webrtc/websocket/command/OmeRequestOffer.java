package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;

@Slf4j
public class OmeRequestOffer extends AbstractCommand {

    public OmeRequestOffer() {
        super(CommandType.REQUEST_OFFER);
    }

    @Override
    public String makeJson() {
        String result = getJsonObject().toString();
        log.debug("OmeRequestOffer: \n{}", WebSocketService.gson.toJson(getJsonObject()));
        return result;
    }

}

