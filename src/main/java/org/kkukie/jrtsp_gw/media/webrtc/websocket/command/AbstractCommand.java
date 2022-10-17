package org.kkukie.jrtsp_gw.media.webrtc.websocket.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractCommand {

    private CommandType type;

    AbstractCommand(CommandType type) {
        this.type = type;
    }

    public abstract String makeJson();

}
