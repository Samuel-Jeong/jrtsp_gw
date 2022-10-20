package org.kkukie.jrtsp_gw.media.stream.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.media.core.scheduler.ServiceScheduler;
import org.kkukie.jrtsp_gw.media.core.scheduler.WallClock;
import org.kkukie.jrtsp_gw.media.dtls.DtlsHandler;
import org.kkukie.jrtsp_gw.media.dtls.DtlsListener;
import org.kkukie.jrtsp_gw.media.rtp.channels.PacketHandler;
import org.kkukie.jrtsp_gw.media.rtp.channels.PacketHandlerPipeline;
import org.kkukie.jrtsp_gw.media.rtp.crypto.DtlsSrtpClientProvider;
import org.kkukie.jrtsp_gw.media.rtp.crypto.DtlsSrtpServerProvider;
import org.kkukie.jrtsp_gw.media.rtp.format.RTPFormats;
import org.kkukie.jrtsp_gw.media.rtp.statistics.RtpStatistics;
import org.kkukie.jrtsp_gw.media.rtsp.rtcp.module.RtpClock;
import org.kkukie.jrtsp_gw.media.stream.handler.RtcpHandler;
import org.kkukie.jrtsp_gw.media.stream.handler.RtpHandler;
import org.kkukie.jrtsp_gw.media.stream.manager.ChannelMaster;
import org.kkukie.jrtsp_gw.media.stun.candidate.IceComponent;
import org.kkukie.jrtsp_gw.media.stun.events.IceEventListener;
import org.kkukie.jrtsp_gw.media.stun.events.SelectedCandidatesEvent;
import org.kkukie.jrtsp_gw.media.stun.handler.IceHandler;
import org.kkukie.jrtsp_gw.media.stun.model.IceAuthenticatorImpl;
import org.kkukie.jrtsp_gw.session.SessionManager;
import org.kkukie.jrtsp_gw.session.media.MediaInfo;
import org.kkukie.jrtsp_gw.session.media.MediaType;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Getter
public class DataChannel implements DtlsListener, IceEventListener {

    private static final int RTP_PRIORITY = 4; // a packet each 20ms
    private static final int STUN_PRIORITY = 3; // a packet each 400ms
    private static final int RTCP_PRIORITY = 2; // a packet each 5s
    private static final int DTLS_PRIORITY = 1; // only for handshake

    private static final int BUFFER_SIZE = 8192;

    private final String callId;
    private final SocketAddress localMediaAddress;
    private SocketAddress realRemoteAddress = null;
    private DatagramChannel mediaChannel;

    public final PacketHandlerPipeline handlers = new PacketHandlerPipeline();

    private final boolean isSecure;

    private final boolean isRtcpMux;

    private IceHandler iceHandler;
    private DtlsHandler dtlsHandler;
    private RtpHandler rtpHandler;
    private RtcpHandler rtcpHandler;

    private DtlsSrtpServerProvider dtlsServerProvider;
    private DtlsSrtpClientProvider dtlsClientProvider;

    private final Map<String, RTPFormats> mediaFormatMap;

    private final Queue<byte[]> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<byte[]> pendingData = new ConcurrentLinkedQueue<>();;

    private final ServiceScheduler scheduler = new ServiceScheduler();

    private final ByteBuffer recvBuffer;

    protected SelectionKey selectionKey;

    private final String localIceUfrag;
    private final String localIcePasswd;

    private final ChannelMaster channelMaster;
    private final MediaInfo mediaInfo;

    ////////////////////////////////////////////////////////////////////////

    public DataChannel(ChannelMaster channelMaster, MediaInfo mediaInfo,
                       String callId, SocketAddress localMediaAddress, Map<String, RTPFormats> mediaFormatMap,
                       boolean isSecure, boolean isRtcpMux,
                       String localIceUfrag, String localIcePasswd) {
        this.channelMaster = channelMaster;
        this.mediaInfo = mediaInfo;

        this.callId = callId;
        this.localMediaAddress = localMediaAddress;
        this.mediaFormatMap = mediaFormatMap;
        this.isSecure = isSecure;
        this.isRtcpMux = isRtcpMux;
        this.recvBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        this.localIceUfrag = localIceUfrag;
        this.localIcePasswd = localIcePasswd;

        initChannel();
        initIce(localIceUfrag, localIcePasswd);
    }

    private void initChannel() {
        try {
            freeChannel();

            mediaChannel = DatagramChannel.open();
            mediaChannel.configureBlocking(false);
            mediaChannel.bind(localMediaAddress);

            channelMaster.registerChannel(mediaChannel, this);
        } catch (Exception e) {
            log.warn("|DataChannel({})| Fail to create the media channel.", callId, e);
        }
    }

    private void freeChannel() {
        try {
            channelMaster.unregisterChannel(this);

            if (mediaChannel != null) {
                mediaChannel.close();
                mediaChannel = null;
            }
        } catch (Exception e) {
            log.warn("|DataChannel({})| Fail to remove the media channel.", callId, e);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private void initRtp(Map<String, RTPFormats> mediaFormatMap) {
        RtpClock rtpClock = new RtpClock(new WallClock());
        RtpClock oobClock = new RtpClock(new WallClock());
        RtpStatistics rtpStatistics = new RtpStatistics(rtpClock);

        initRtpHandler(mediaFormatMap, rtpClock, oobClock, rtpStatistics);
        initRtcpHandler(rtpStatistics);
    }

    private void initRtpHandler(Map<String, RTPFormats> mediaFormatMap,
                                RtpClock rtpClock,
                                RtpClock oobClock, RtpStatistics rtpStatistics) {
        rtpHandler = new RtpHandler(
                callId,
                rtpClock, oobClock,
                rtpStatistics,
                mediaFormatMap
        );
        rtpHandler.setRtpRecvCallback(mediaInfo::handleRtpPacket);
        rtpHandler.setPipelinePriority(RTP_PRIORITY);
        handlers.addHandler(rtpHandler);
        if (isSecure) {
            rtpHandler.enableSrtp(dtlsHandler);
        } else {
            rtpHandler.disableSrtp();
        }
    }

    private void initRtcpHandler(RtpStatistics rtpStatistics) {
        if (isRtcpMux) {
            scheduler.start();
            rtcpHandler = new RtcpHandler(
                    callId,
                    scheduler, rtpStatistics, MediaType.AUDIO.getName(), realRemoteAddress
            );
            rtcpHandler.setPipelinePriority(RTCP_PRIORITY);
            handlers.addHandler(rtcpHandler);
            if (isSecure) {
                rtcpHandler.enableSRTCP(dtlsHandler);
            } else {
                rtcpHandler.disableSRTCP();
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private void initIce(String localIceUfrag, String localIcePasswd) {
        iceHandler = new IceHandler(callId, IceComponent.RTP_ID, this);

        IceAuthenticatorImpl iceAuthenticator = new IceAuthenticatorImpl();
        iceAuthenticator.setUfrag(localIceUfrag);
        iceAuthenticator.setPassword(localIcePasswd);
        iceHandler.setAuthenticator(iceAuthenticator);

        iceHandler.setPipelinePriority(STUN_PRIORITY);
        handlers.addHandler(iceHandler);

        iceHandler.startHarvester(mediaInfo);
    }

    /////////////////////////////////////////////////////////////////////////

    private void initDtls() {

        if (isSecure) {
            DtlsConfig dtlsConfig = ConfigManager.getDtlsConfig();
            String keyPath = dtlsConfig.getKeyPath();
            String certPath = dtlsConfig.getCertPath();
            File keyFile = new File(keyPath);
            File certFile = new File(certPath);
            if (!keyFile.exists() || !certFile.exists()) {
                log.error("|DataChannel({})| Fail to find the key or cert file. (keyPath={}, certPath={})", callId, keyPath, certPath);
                return;
            }

            dtlsClientProvider = new DtlsSrtpClientProvider(certPath, keyPath);
            dtlsServerProvider = new DtlsSrtpServerProvider(certPath, keyPath);

            dtlsHandler = new DtlsHandler(callId, dtlsServerProvider, dtlsClientProvider, realRemoteAddress);
            dtlsHandler.setChannel(mediaChannel);
            dtlsHandler.addListener(this);
            dtlsHandler.setPipelinePriority(DTLS_PRIORITY);
            handlers.addHandler(dtlsHandler);
        }
    }

    public void onSelectedCandidates (boolean useCandidate) {
        if (isSecure && dtlsHandler != null) {
            dtlsHandler.handshake(useCandidate);
        }
    }

    public boolean isConnected() {
        return mediaChannel != null && mediaChannel.isConnected();
    }

    public boolean isOpen() {
        if(this.mediaChannel != null) {
            return this.mediaChannel.isOpen();
        }
        return false;
    }

    public void connect(SocketAddress address) throws IOException {
        if(this.mediaChannel == null) {
            throw new IOException("|DataChannel(" + callId + ")| No channel available to connect.");
        }
        this.mediaChannel.connect(address);
    }

    public void disconnect() throws IOException {
        if(isConnected()) {
            this.mediaChannel.disconnect();
        }
    }
    
    /////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////

    public boolean hasPendingData() {
        return !this.pendingData.isEmpty();
    }

    public byte[] receive() {
        recvBuffer.clear();
        
        int dataLength;
        try {
            SocketAddress remotePeer = mediaChannel.receive(recvBuffer);
            if (!isConnected() && remotePeer != null) {
                connect(remotePeer);
            }
            dataLength = this.recvBuffer.position();
        } catch (IOException e) {
            dataLength = -1;
        }

        // Stop if socket was shutdown or error occurred
        if (dataLength == -1) {
            close();
        } else if (dataLength > 0) {
            // Copy data from buffer so we don't mess with original
            byte[] dataCopy = new byte[dataLength];
            this.recvBuffer.rewind();
            this.recvBuffer.get(dataCopy, 0, dataLength);

            // Delegate work to the proper handler
            PacketHandler handler = this.handlers.getHandler(dataCopy);
            if (handler != null) {
                try {
                    byte[] response = handler.handle(
                            dataCopy, dataLength, 0,
                            (InetSocketAddress) mediaChannel.getLocalAddress(),
                            (InetSocketAddress) mediaChannel.getRemoteAddress()
                    );
                    if (response != null && response.length > 0) {
                        queueData(response);
                    }
                } catch (Exception e) {
                    log.error("|DataChannel({})| Could not handle incoming packet.", callId, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("|DataChannel({})| No protocol handler was found to process an incoming packet. Packet will be dropped.", callId);
                }
            }
            return dataCopy;
        }

        return null;
    }

    public void queueData(final byte[] data) {
        if (data != null && data.length > 0) {
            this.pendingData.offer(data);
        }
    }

    public void send() throws IOException {
        if (this.mediaChannel == null) { return; }

        byte[] data = this.pendingData.poll();
        if (data != null) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            if (realRemoteAddress == null) { return; }
            this.mediaChannel.send(buffer, realRemoteAddress);
            // Keep sending queued data, recursive style
            send();
        }
    }

    public void send(byte[] data, SocketAddress remoteAddress) throws IOException {
        if (remoteAddress == null) { return; }
        if (data != null) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            this.mediaChannel.send(buffer, remoteAddress);
        }
    }

    public void flush() {
        try {
            this.recvBuffer.clear();
            // lets clear the receiver
            SocketAddress currAddress;
            do {
                if (mediaChannel != null && mediaChannel.isOpen()) {
                    currAddress = mediaChannel.receive(recvBuffer);
                    this.recvBuffer.clear();
                } else {
                    currAddress = null;
                }
            } while (currAddress != null);
        } catch (Exception e) {
            log.warn("|DataChannel({})| Stopped flushing the channel abruptly.", callId, e);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    public void close() {
        rtpHandler = null;
        rtcpHandler = null;

        if (iceHandler != null) {
            iceHandler.stopHarvester();
            iceHandler = null;
        }

        try {
            if (dtlsHandler != null) {
                dtlsHandler.close();
                dtlsHandler = null;
            }
        } catch (IOException e) {
            // ignore
        }

        realRemoteAddress = null;
        scheduler.stop();
        flush();
        freeChannel();
        if (selectionKey != null) {
            selectionKey.cancel();
        }
    }

    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onDtlsHandshakeComplete ( ) {
        log.debug("|DataChannel({})| DTLS handshake completed for RTP candidate.", callId);
        if (this.isRtcpMux) {
            this.rtcpHandler.joinRtpSession();
        }
    }

    @Override
    public void onDtlsHandshakeFailed (Throwable e) {
        log.warn("|DataChannel({})| DTLS handshake failed for RTP candidate.", callId, e);
        SessionManager.getInstance().deleteCall(callId);
        close();
    }

    @Override
    public void onSelectedCandidates(SelectedCandidatesEvent selectedCandidatesEvent, boolean useCandidate) {
        if (realRemoteAddress != null) { return; }

        realRemoteAddress = selectedCandidatesEvent.getRemotePeer();
        if (realRemoteAddress == null) {
            log.warn("|DataChannel({})| Fail to get the remote address from SelectedCandidatesEvent. Fail to open dtls.", callId);
            return;
        } else {
            mediaInfo.getTargetAddressQueue().removeIf(
                    inetSocketAddress -> !inetSocketAddress.equals(realRemoteAddress)
            );
        }

        initDtls();
        initRtp(mediaFormatMap);

        onSelectedCandidates(useCandidate);
    }

}
