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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HarvestHandler {

    private final int STUN_DELAY; // milliseconds

    private final String callId;

    private ScheduledThreadPoolExecutor executor = null;
    private ScheduledFuture<?> harvesterFuture = null;

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

        executor = new ScheduledThreadPoolExecutor(1);
        harvesterFuture = executor.scheduleWithFixedDelay(
                () -> harvest(mediaInfo),
                0, STUN_DELAY, TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (harvesterFuture != null) {
            harvesterFuture.cancel(true);
            harvesterFuture = null;
        }

        if (executor != null) {
            executor.shutdown();
            executor = null;
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
