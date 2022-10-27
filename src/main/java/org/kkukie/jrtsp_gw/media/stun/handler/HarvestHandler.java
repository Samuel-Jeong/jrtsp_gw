package org.kkukie.jrtsp_gw.media.stun.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.media.core.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.stream.model.DataChannel;
import org.kkukie.jrtsp_gw.media.stun.model.StunMessageFactory;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.IceInfo;

import javax.xml.bind.DatatypeConverter;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HarvestHandler {

    private final int STUN_DELAY; // milliseconds

    private final String conferenceId;

    private ScheduledThreadPoolExecutor executor = null;
    private ScheduledFuture<?> harvesterFuture = null;

    public HarvestHandler(String conferenceId) {
        this.conferenceId = conferenceId;

        this.STUN_DELAY = ConfigManager.getStunConfig().getHarvestIntervalMs();
    }

    public void start(DataChannel dataChannel, IceInfo iceInfo, List<InetSocketAddress> targetAddressList) {
        if (dataChannel == null) {
            log.warn("|HarvestHandler({})| Fail to start harvesting. DataChannel is null.", conferenceId);
            return;
        }
        if (iceInfo == null) {
            log.warn("|HarvestHandler({})| Fail to start harvesting. IceInfo is null.", conferenceId);
            return;
        }
        if (targetAddressList == null) {
            log.warn("|HarvestHandler({})| Fail to start harvesting. TargetAddressQueue is null.", conferenceId);
            return;
        }

        stop();

        executor = new ScheduledThreadPoolExecutor(1);
        harvesterFuture = executor.scheduleWithFixedDelay(
                () -> harvest(dataChannel, iceInfo, targetAddressList),
                0, STUN_DELAY, TimeUnit.MILLISECONDS
        );
        if (!harvesterFuture.isDone()) {
            log.debug("|HarvestHandler({})| Started. (interval={}ms)", conferenceId, STUN_DELAY);
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

    private void harvest(DataChannel dataChannel, IceInfo iceInfo, List<InetSocketAddress> targetAddressList) {
        for (InetSocketAddress targetAddress : targetAddressList) {
            try {
                StunRequest bindingRequest = StunMessageFactory.createBindingRequest(
                        iceInfo.getRemoteUsername(),
                        iceInfo.getRemoteIcePasswd()
                );
                dataChannel.send(bindingRequest.encode(), targetAddress);
                log.debug("|HarvestHandler({})| Send StunRequest(tid={}) to [{}].",
                        conferenceId, DatatypeConverter.printHexBinary(bindingRequest.getTransactionId()), targetAddress
                );
            } catch (Exception e) {
                log.warn("|HarvestHandler({})| Fail to stun binding.", conferenceId, e);
            }
        }
    }

}
