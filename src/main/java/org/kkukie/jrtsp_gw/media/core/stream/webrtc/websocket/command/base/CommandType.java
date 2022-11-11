package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base;

import lombok.Getter;

@Getter
public enum CommandType {

    REQUEST_OFFER("request_offer"), // c > s (after connected)
    OFFER("offer"), // s > c (after request_offer)
    ANSWER("answer"), // c > s (after offer)
    CANDIDATE("candidate") // c > s (after answer)
    ;

    private final String name;

    CommandType(String name) {
        this.name = name;
    }

}
