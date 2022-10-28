package org.kkukie.jrtsp_gw.media.webrtc.websocket.command;


import org.kkukie.jrtsp_gw.media.webrtc.websocket.command.base.AbstractCommand;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.command.base.CommandType;

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
