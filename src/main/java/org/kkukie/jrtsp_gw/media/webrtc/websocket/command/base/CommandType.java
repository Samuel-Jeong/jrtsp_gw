package org.kkukie.jrtsp_gw.media.webrtc.websocket.command.base;

public enum CommandType {

    REQUEST_OFFER, // c > s (after connected)
    OFFER, // s > c (after request_offer)
    ANSWER, // c > s (after offer)
    CANDIDATE // c > s (after answer)

}
