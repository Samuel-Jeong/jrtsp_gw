package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractCommand {

    private CommandType type;

    private final JsonObject jsonObject;

    protected AbstractCommand(CommandType type) {
        this.type = type;
        this.jsonObject = new JsonObject();
        jsonObject.addProperty("command", getType().getName());
    }

    public abstract String makeJson();

}
