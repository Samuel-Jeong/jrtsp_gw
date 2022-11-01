package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command;


import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.AbstractCommand;

public class OmeOffer extends AbstractCommand {

    public static final String TYPE = "offer";

    public OmeOffer() {
        super(CommandType.OFFER);
    }

    @Override
    public String makeJson() {
        return null;
    }

}
