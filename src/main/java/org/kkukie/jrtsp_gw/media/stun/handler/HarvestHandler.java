package org.kkukie.jrtsp_gw.media.stun.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.media.core.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.stream.model.DataChannel;
import org.kkukie.jrtsp_gw.media.stun.model.StunMessageFactory;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.IceInfo;
import org.kkukie.jrtsp_gw.session.media.MediaInfo;

import javax.xml.bind.DatatypeConverter;
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

        executor = new ScheduledThreadPoolExecutor(2);
        harvesterFuture = executor.scheduleWithFixedDelay(
                () -> harvest(mediaInfo),
                0, STUN_DELAY, TimeUnit.MILLISECONDS
        );
        if (!harvesterFuture.isDone()) {
            log.debug("|HarvestHandler({})| Started. (interval={}ms)", callId, STUN_DELAY);
        }
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
            if (targetAddressQueue == null) {
                log.warn("|HarvestHandler({})| Fail to get the targetAddressQueue from mediainfo.", callId);
                return;
            }

            IceInfo iceInfo = mediaInfo.getIceInfo();
            if (iceInfo == null) {
                log.warn("|HarvestHandler({})| Fail to get the iceInfo from mediainfo.", callId);
                return;
            }

            for (InetSocketAddress targetAddress : targetAddressQueue) {
                try {
                    StunRequest bindingRequest = StunMessageFactory.createBindingRequest(
                            iceInfo.getRemoteUsername(),
                            iceInfo.getRemoteIcePasswd()
                    );
                    dataChannel.send(bindingRequest.encode(), targetAddress);
                    log.debug("|HarvestHandler({})| Send StunRequest(tid={}) to [{}].",
                            callId, DatatypeConverter.printHexBinary(bindingRequest.getTransactionId()), targetAddress
                    );
                } catch (Exception e) {
                    log.warn("|HarvestHandler({})| Fail to stun binding.", callId, e);
                }
            }
        } else {
            log.warn("|HarvestHandler({})| Fail to get the data channel from mediainfo.", callId);
        }
    }

}
