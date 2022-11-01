package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractCommand {

    private CommandType type;

    protected AbstractCommand(CommandType type) {
        this.type = type;
    }

    public abstract String makeJson();

}
