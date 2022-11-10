package org.kkukie.jrtsp_gw.media.core.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.manager.PacketHandlerMaster;
import org.kkukie.jrtsp_gw.media.core.manager.PacketSelector;
import org.kkukie.jrtsp_gw.media.core.stream.dtls.DtlsListener;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandler;
import org.kkukie.jrtsp_gw.media.core.stream.stun.events.IceEventListener;
import org.kkukie.jrtsp_gw.media.core.stream.stun.events.SelectedCandidatesEvent;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.model.ice.IceInfo;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.media.MediaSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * [관리 항목]
 * 1. 로컬 미디어 채널(소켓) 생성
 * 2. 네트워크 패킷 핸들러 관리
 *      - IceHandler
 *      - DtlsHandler
 *      - RtpHandler
 *      - RtcpHandler
 * 3. DTLS
 * 4. PacketSelector 를 사용하여 채널 등록
 *
 */
@Slf4j
@Getter
@Setter
public class DataChannel implements DtlsListener, IceEventListener {

    private static final int BUFFER_SIZE = 8192;

    private final String conferenceId;

    private final MediaSession mediaSession;

    protected SelectionKey selectionKey;
    private DatagramChannel mediaChannel;
    private final SocketAddress localMediaAddress;
    private SocketAddress realRemoteAddress = null;
    private List<InetSocketAddress> targetAddressList = null;

    private final ByteBuffer recvBuffer;
    private final Queue<byte[]> pendingData;

    private final PacketHandlerMaster packetHandlerMaster;
    private final PacketSelector packetSelector;

    ////////////////////////////////////////////////////////////////////////
    public DataChannel(PacketSelector packetSelector, MediaSession mediaSession,
                       String conferenceId, SocketAddress localMediaAddress) {
        this.packetSelector = packetSelector;
        this.mediaSession = mediaSession;
        this.conferenceId = conferenceId;
        this.localMediaAddress = localMediaAddress;
        this.recvBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.pendingData = new ConcurrentLinkedQueue<>();
        this.packetHandlerMaster = new PacketHandlerMaster(conferenceId, mediaSession);
    }

    public void initChannel() {
        try {
            freeChannel();

            mediaChannel = DatagramChannel.open();
            mediaChannel.configureBlocking(false);
            mediaChannel.bind(localMediaAddress);

            packetSelector.registerChannel(mediaChannel, this);
        } catch (Exception e) {
            log.warn("|DataChannel({})| Fail to create the media channel.", conferenceId, e);
        }
    }

    public void close() {
        packetHandlerMaster.reset();

        flush();
        freeChannel();
        if (selectionKey != null) {
            selectionKey.cancel();
        }
        realRemoteAddress = null;
    }

    private void freeChannel() {
        try {
            packetSelector.unregisterChannel(this);

            if (mediaChannel != null) {
                mediaChannel.close();
                mediaChannel = null;
            }
        } catch (Exception e) {
            log.warn("|DataChannel({})| Fail to remove the media channel.", conferenceId, e);
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
            throw new IOException("|DataChannel(" + conferenceId + ")| No channel available to connect.");
        }
        this.mediaChannel.connect(address);
    }

    public void disconnect() throws IOException {
        if(isConnected()) {
            this.mediaChannel.disconnect();
        }
    }

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
            PacketHandler handler = packetHandlerMaster.getHandlers().getHandler(dataCopy);
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
                    log.error("|DataChannel({})| Could not handle incoming packet.", conferenceId, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("|DataChannel({})| No protocol handler was found to process an incoming packet. Packet will be dropped.", conferenceId);
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

    public boolean send(byte[] data, SocketAddress remoteAddress) throws IOException {
        if (remoteAddress == null) { return false; }
        if (data != null) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            return this.mediaChannel.send(buffer, remoteAddress) > 0;
        }
        return false;
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
            log.warn("|DataChannel({})| Stopped flushing the channel abruptly.", conferenceId, e);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    public void initIce(IceInfo iceInfo, List<InetSocketAddress> targetAddressList) {
        this.targetAddressList = targetAddressList;
        packetHandlerMaster.initIce(iceInfo, targetAddressList, this);
    }

    @Override
    public void onDtlsHandshakeComplete () {
        log.debug("|DataChannel({})| DTLS handshake completed for RTP candidate.", conferenceId);
        if (mediaSession.isRtcpMux()) {
            packetHandlerMaster.getRtcpHandler().joinRtpSession();
        }
    }

    @Override
    public void onDtlsHandshakeFailed (Throwable e) {
        log.warn("|DataChannel({})| DTLS handshake failed for RTP candidate.", conferenceId, e);
        ConferenceMaster.getInstance().deleteConference(conferenceId);
        close();
    }

    @Override
    public void onSelectedCandidates(SelectedCandidatesEvent selectedCandidatesEvent, boolean useCandidate) {
        if (realRemoteAddress != null) { return; }

        realRemoteAddress = selectedCandidatesEvent.getRemotePeer();
        if (realRemoteAddress == null) {
            log.warn("|DataChannel({})| Fail to get the remote address from SelectedCandidatesEvent. Fail to open dtls.", conferenceId);
            return;
        } else {
            if (targetAddressList != null) {
                targetAddressList.removeIf(
                        inetSocketAddress -> !inetSocketAddress.equals(realRemoteAddress)
                );
            }
        }

        synchronized (packetHandlerMaster) {
            packetHandlerMaster.initDtls(mediaChannel, realRemoteAddress, this);
            packetHandlerMaster.initRtp(mediaChannel, realRemoteAddress, mediaSession.getMediaFormatMap());
            packetHandlerMaster.selectCandidate(useCandidate);
        }
    }

}