package org.kkukie.jrtsp_gw.media.core.stream.stun.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.media.core.model.DataChannel;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.core.stream.stun.model.StunMessageFactory;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.model.ice.IceInfo;

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

    private StunRequest bindingRequest = null;
    private byte[] encodedBindingRequest = null;

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

        try {
            bindingRequest = StunMessageFactory.createBindingRequest(
                    iceInfo.getRemoteUsername(),
                    iceInfo.getRemoteIcePasswd()
            );
            encodedBindingRequest = bindingRequest.encode();

            executor = new ScheduledThreadPoolExecutor(1);
            harvesterFuture = executor.scheduleWithFixedDelay(
                    () -> harvest(dataChannel, targetAddressList),
                    0, STUN_DELAY, TimeUnit.MILLISECONDS
            );
            if (!harvesterFuture.isDone()) {
                log.debug("|HarvestHandler({})| Started. (interval={}ms)", conferenceId, STUN_DELAY);
            }
        } catch (Exception e) {
            log.warn("|HarvestHandler({})| Fail to stun binding.", conferenceId, e);
        }
    }

    public void stop() {
        bindingRequest = null;
        encodedBindingRequest = null;

        if (harvesterFuture != null) {
            harvesterFuture.cancel(true);
            harvesterFuture = null;
        }

        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private void harvest(DataChannel dataChannel, List<InetSocketAddress> targetAddressList) {
        for (InetSocketAddress targetAddress : targetAddressList) {
            try {
                if (dataChannel.send(encodedBindingRequest, targetAddress)) {
                    if (log.isTraceEnabled()) {
                        log.trace("|HarvestHandler({})| Send StunRequest(tid={}) to [{}].",
                                conferenceId, DatatypeConverter.printHexBinary(bindingRequest.getTransactionId()), targetAddress
                        );
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("|HarvestHandler({})| !!! FAIL to send StunRequest(tid={}) to [{}].",
                                conferenceId, DatatypeConverter.printHexBinary(bindingRequest.getTransactionId()), targetAddress
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("|HarvestHandler({})| Fail to stun binding.", conferenceId, e);
            }
        }
    }

}
