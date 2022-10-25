package org.kkukie.jrtsp_gw.media.stun.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.media.core.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.stream.model.DataChannel;
import org.kkukie.jrtsp_gw.media.stun.model.StunMessageFactory;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.IceInfo;
import org.kkukie.jrtsp_gw.session.media.MediaInfo;

import java.net.InetSocketAddress;
import java.util.Queue;

@Slf4j
public class HarvestHandler {

    private final int STUN_DELAY; // milliseconds

    private final String callId;
    private Thread harvester = null;

    public HarvestHandler(String callId) {
        this.callId = callId;

        this.STUN_DELAY = ConfigManager.getStunConfig().getHarvestIntervalMs();
    }

    public void start(MediaInfo mediaInfo) {
        if (mediaInfo == null) {
            log.warn("|IceHandler({})| Fail to start harvesting. MediaInfo is null.", callId);
            return;
        }

        stop();
        harvester = new Thread(
                () -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            harvest(mediaInfo);
                            Thread.sleep(STUN_DELAY);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
        );
        harvester.start();
    }

    public void stop() {
        if (harvester != null) {
            harvester.interrupt();
            harvester = null;
        }
    }

    private void harvest(MediaInfo mediaInfo) {
        DataChannel dataChannel = mediaInfo.getDataChannel();
        if (dataChannel != null) {
            Queue<InetSocketAddress> targetAddressQueue = mediaInfo.getTargetAddressQueue();
            if (targetAddressQueue == null) { return; }

            IceInfo iceInfo = mediaInfo.getIceInfo();
            if (iceInfo == null) { return; }

            for (InetSocketAddress targetAddress : targetAddressQueue) {
                try {
                    StunRequest bindingRequest = StunMessageFactory.createBindingRequest(
                            iceInfo.getRemoteUsername(),
                            iceInfo.getRemoteIcePasswd()
                    );
                    dataChannel.send(bindingRequest.encode(), targetAddress);
                    //log.debug("|IceHandler({})| Send StunRequest to [{}]", callId, targetAddress);
                } catch (Exception e) {
                    //log.warn("|IceHandler({})| Fail to stun binding.", callId, e);
                }
            }
        }
    }

}
