package org.kkukie.jrtsp_gw.media.core.stream.dtls;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.DTLSClientProtocol;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.DTLSServerProtocol;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.DTLSTransport;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.DatagramTransport;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.crypto.*;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DtlsHandler implements PacketHandler, DatagramTransport {

    public static final int DEFAULT_MTU = 1500;
    public static final int MAX_DELAY = 20000;
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0);
    private static final int MIN_IP_OVERHEAD = 20;
    private static final int MAX_IP_OVERHEAD = MIN_IP_OVERHEAD + 64;
    private static final int UDP_OVERHEAD = 8;

    private final String conferenceId;

    private final int receiveLimit;
    private final int sendLimit;
    private final Queue<ByteBuffer> rxQueue;
    private final List<DtlsListener> listeners;
    // Packet Handler properties
    private int pipelinePriority;
    // Network properties
    private final int mtu;
    // DTLS Handshake properties
    private DtlsSrtpServer server;
    private final DtlsSrtpClient client;

    private long startTime;
    private volatile boolean handshakeComplete;
    private volatile boolean handshakeFailed;
    private volatile boolean handshaking;
    private Thread worker;

    // SRTP properties
    // http://tools.ietf.org/html/rfc5764#section-4.2
    private PacketTransformer srtpEncoder;
    private PacketTransformer srtpDecoder;
    private PacketTransformer srtcpEncoder;
    private PacketTransformer srtcpDecoder;

    private final DtlsSrtpServerProvider tlsServerProvider;

    private DatagramChannel datagramChannel;

    private final SocketAddress remoteAddress;

    public DtlsHandler (String conferenceId, SocketAddress remoteAddress) {
        this.conferenceId = conferenceId;
        this.pipelinePriority = 0;
        this.remoteAddress = remoteAddress;

        // Network properties
        this.mtu = DEFAULT_MTU;
        this.receiveLimit = mtu - MIN_IP_OVERHEAD - UDP_OVERHEAD;
        this.sendLimit = mtu - MAX_IP_OVERHEAD - UDP_OVERHEAD;

        // Handshake properties
        DtlsConfig dtlsConfig = ConfigManager.getDtlsConfig();
        DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(dtlsConfig.getCertPath(), dtlsConfig.getKeyPath());
        DtlsSrtpClientProvider dtlsClientProvider = new DtlsSrtpClientProvider(dtlsConfig.getCertPath(), dtlsConfig.getKeyPath());
        this.tlsServerProvider = dtlsServerProvider;
        this.server = dtlsServerProvider.provide();
        this.client = dtlsClientProvider.provide();

        this.listeners = new ArrayList<>();
        this.rxQueue = new ConcurrentLinkedQueue<>();
        this.startTime = 0L;
        this.handshakeComplete = false;
        this.handshakeFailed = false;
        this.handshaking = false;
    }

    public void setChannel (DatagramChannel datagramChannel) {
        this.datagramChannel = datagramChannel;
    }

    public void addListener (DtlsListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public boolean isHandshakeComplete () {
        return handshakeComplete;
    }

    public boolean isHandshakeFailed () {
        return handshakeFailed;
    }

    public boolean isHandshaking () {
        return handshaking;
    }

    private byte[] getMasterServerKey (boolean isServer) {
        if (isServer) {
            return server.getSrtpMasterServerKey();
        } else {
            return client.getSrtpMasterServerKey();
        }
    }

    private byte[] getMasterServerSalt (boolean isServer) {
        if (isServer) {
            return server.getSrtpMasterServerSalt();
        } else {
            return client.getSrtpMasterServerSalt();
        }
    }

    private byte[] getMasterClientKey (boolean isServer) {
        if (isServer) {
            return server.getSrtpMasterClientKey();
        } else {
            return client.getSrtpMasterClientKey();
        }
    }

    private byte[] getMasterClientSalt (boolean isServer) {
        if (isServer) {
            return server.getSrtpMasterClientSalt();
        } else {
            return client.getSrtpMasterClientSalt();
        }
    }

    private SRTPPolicy getSrtpPolicy (boolean isServer) {
        if (isServer) {
            return server.getSrtpPolicy();
        } else {
            return client.getSrtpPolicy();
        }
    }

    private SRTPPolicy getSrtcpPolicy (boolean isServer) {
        if (isServer) {
            return server.getSrtcpPolicy();
        } else {
            return client.getSrtcpPolicy();
        }
    }

    /**
     * Generates an SRTP encoder for outgoing RTP packets using keying material from the DTLS handshake.
     */
    private PacketTransformer generateRtpEncoder (boolean isServer) {
        return new SRTPTransformEngine(
                getMasterServerKey(isServer),
                getMasterServerSalt(isServer),
                getSrtpPolicy(isServer),
                getSrtcpPolicy(isServer))
                .getRTPTransformer();
    }

    /**
     * Generates an SRTP decoder for incoming RTP packets using keying material from the DTLS handshake.
     */
    private PacketTransformer generateRtpDecoder (boolean isServer) {
        return new SRTPTransformEngine(
                getMasterClientKey(isServer),
                getMasterClientSalt(isServer),
                getSrtpPolicy(isServer),
                getSrtcpPolicy(isServer))
                .getRTPTransformer();
    }

    /**
     * Generates an SRTCP encoder for outgoing RTCP packets using keying material from the DTLS handshake.
     */
    private PacketTransformer generateRtcpEncoder (boolean isServer) {
        return new SRTPTransformEngine(getMasterServerKey(isServer),
                getMasterServerSalt(isServer),
                getSrtpPolicy(isServer),
                getSrtcpPolicy(isServer))
                .getRTCPTransformer();
    }

    /**
     * Generates an SRTCP decoder for incoming RTCP packets using keying material from the DTLS handshake.
     */
    private PacketTransformer generateRtcpDecoder (boolean isServer) {
        return new SRTPTransformEngine(getMasterClientKey(isServer),
                getMasterClientSalt(isServer),
                getSrtpPolicy(isServer),
                getSrtcpPolicy(isServer))
                .getRTCPTransformer();
    }

    /**
     * Decodes an RTP Packet
     *
     * @param packet The encoded RTP packet
     * @return The decoded RTP packet. Returns null is packet is not valid.
     */
    public byte[] decodeRTP (byte[] packet, int offset, int length) {
        return this.srtpDecoder.reverseTransform(packet, offset, length);
    }

    /**
     * Encodes an RTP packet
     *
     * @param packet The decoded RTP packet
     * @return The encoded RTP packet
     */
    public byte[] encodeRTP (byte[] packet, int offset, int length) {
        return this.srtpEncoder.transform(packet, offset, length);
    }

    /**
     * Decodes an RTCP Packet
     *
     * @param packet The encoded RTP packet
     * @return The decoded RTP packet. Returns null is packet is not valid.
     */
    public byte[] decodeRTCP (byte[] packet, int offset, int length) {
        return this.srtcpDecoder.reverseTransform(packet, offset, length);
    }

    /**
     * Encodes an RTCP packet
     *
     * @param packet The decoded RTP packet
     * @return The encoded RTP packet
     */
    public byte[] encodeRTCP (byte[] packet, int offset, int length) {
        return this.srtcpEncoder.transform(packet, offset, length);
    }

    public void handshake (boolean useCandidate) {
        if (!handshaking && !handshakeComplete) {
            this.handshaking = true;
            this.startTime = System.currentTimeMillis();
            if (useCandidate) {
                this.worker = new Thread(new HandshakeClient(), "DTLS-Client-" + THREAD_COUNTER.incrementAndGet());
                this.worker.start();
            } else {
                this.worker = new Thread(new HandshakeServer(), "DTLS-Server-" + THREAD_COUNTER.incrementAndGet());
                this.worker.start();
            }
        }
    }

    private void fireHandshakeComplete () {
        if (!this.listeners.isEmpty()) {
            for (DtlsListener listener : listeners) {
                listener.onDtlsHandshakeComplete();
                log.debug("|DtlsHandler({})| DTLS DONE", conferenceId);
            }
        }
    }

    private void fireHandshakeFailed (Throwable e) {
        if (!this.listeners.isEmpty()) {
            for (DtlsListener listener : listeners) {
                listener.onDtlsHandshakeFailed(e);
                log.warn("|DtlsHandler({})| DTLS FAILED", conferenceId);
            }
        }
    }

    public void reset () {
        this.server = this.tlsServerProvider.provide();
        this.datagramChannel = null;
        this.srtcpDecoder = null;
        this.srtcpEncoder = null;
        this.srtpDecoder = null;
        this.srtpEncoder = null;
        this.startTime = 0L;
        this.handshakeComplete = false;
        this.handshakeFailed = false;
        this.handshaking = false;
        this.listeners.clear();
    }

    @Override
    public int compareTo (PacketHandler o) {
        if (o == null) {
            return 1;
        }
        return this.getPipelinePriority() - o.getPipelinePriority();
    }

    @Override
    public boolean canHandle (byte[] packet) {
        return canHandle(packet, packet.length, 0);
    }

    @Override
    public boolean canHandle (byte[] packet, int dataLength, int offset) {
        // https://tools.ietf.org/html/rfc5764#section-5.1.2
        int contentType = packet[offset] & 0xff;
        return contentType > 19 && contentType < 64;
    }

    @Override
    public byte[] handle (byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) {
        return this.handle(packet, packet.length, 0, localPeer, remotePeer);
    }

    @Override
    public byte[] handle (byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) {
        this.rxQueue.offer(ByteBuffer.wrap(packet, offset, dataLength));
        return null;
    }

    @Override
    public int getPipelinePriority () {
        return this.pipelinePriority;
    }

    public void setPipelinePriority (int pipelinePriority) {
        this.pipelinePriority = pipelinePriority;
    }

    @Override
    public int getReceiveLimit () {
        return this.receiveLimit;
    }

    @Override
    public int getSendLimit () {
        return this.sendLimit;
    }

    @Override
    public int receive (byte[] buf, int off, int len, int waitMillis) throws IOException {
        // MEDIA-48: DTLS handshake thread does not terminate
        // https://telestax.atlassian.net/browse/MEDIA-48
        if (this.hasTimeout()) {
            close();
            IllegalStateException illegalStateException = new IllegalStateException("Handshake is taking too long! (>" + MAX_DELAY + "ms");
            fireHandshakeFailed(illegalStateException);
            throw illegalStateException;
        }

        int attempts = waitMillis;
        do {
            ByteBuffer data = this.rxQueue.poll();
            if (data != null && data.limit() > 0) {
                int limit = data.limit();
                data.get(buf, off, limit);
                return limit;
            }

            try {
                Thread.sleep(1);
            } catch (Exception e) {
                log.warn("|DtlsHandler({})| Could not sleep thread to receive DTLS data.", conferenceId);
            } finally {
                attempts--;
            }
        } while (attempts > 0);

        // Throw IO exception if no data was received in this interval. Restarts outbound flight.
        throw new SocketTimeoutException("Could not receive DTLS packet in " + waitMillis);
    }

    @Override
    public void send (byte[] buf, int off, int len) throws IOException {
        if (!hasTimeout()) {
            if (this.datagramChannel != null && datagramChannel.isOpen()) {
                this.datagramChannel.send(
                        ByteBuffer.wrap(buf, off, len),
                        remoteAddress
                );
            } else {
                log.warn("|DtlsHandler({})| Handler skipped send operation because channel is not open or connected.", conferenceId);
            }
        } else {
            log.warn("|DtlsHandler({})| Handler has timed out so send operation will be skipped.", conferenceId);
        }
    }

    @Override
    public void close () throws IOException {
        this.rxQueue.clear();
        this.startTime = 0L;
        this.datagramChannel = null;
    }

    private boolean hasTimeout () {
        return (System.currentTimeMillis() - this.startTime) > MAX_DELAY;
    }

    private class HandshakeServer implements Runnable {

        public void run () {
            DtlsHandler.this.rxQueue.clear();
            SecureRandom secureRandom = new SecureRandom();
            DTLSServerProtocol serverProtocol = new DTLSServerProtocol(secureRandom);

            try {
                // Perform the handshake in a non-blocking fashion
                DTLSTransport dtlsTransport = serverProtocol.accept(server, DtlsHandler.this);
                if (dtlsTransport == null) {
                    log.warn("|DtlsHandler({})| serverProtocol.accept result is null", conferenceId);
                }
                // Prepare the shared key to be used in RTP streaming
                server.prepareSrtpSharedSecret();

                // Generate encoders for DTLS traffic
                PacketTransformer packetTransformer = srtpDecoder = generateRtpDecoder(true);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeServer: generateRtpDecoder is null", conferenceId);
                }
                packetTransformer = srtpEncoder = generateRtpEncoder(true);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeServer: generateRtpEncoder is null", conferenceId);
                }
                packetTransformer = srtcpDecoder = generateRtcpDecoder(true);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeServer: generateRtcpDecoder is null", conferenceId);
                }
                packetTransformer = srtcpEncoder = generateRtcpEncoder(true);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeServer: generateRtcpEncoder is null", conferenceId);
                }

                // Declare handshake as complete
                handshakeComplete = true;
                handshakeFailed = false;
                handshaking = false;

                // Warn listeners handshake completed
                fireHandshakeComplete();
            } catch (Exception e) {
                log.error("|DtlsHandler({})| DtlsHandler.HandshakeServer: DTLS handshake failed. Reason:", conferenceId, e);

                // Declare handshake as failed
                handshakeComplete = false;
                handshakeFailed = true;
                handshaking = false;

                // Warn listeners handshake completed
                fireHandshakeFailed(e);
            }
        }

    }

    private class HandshakeClient implements Runnable {

        public void run () {
            DtlsHandler.this.rxQueue.clear();
            SecureRandom secureRandom = new SecureRandom();
            DTLSClientProtocol clientProtocol = new DTLSClientProtocol(secureRandom);

            try {
                // Perform the handshake in a non-blocking fashion
                DTLSTransport dtlsTransport = clientProtocol.connect(client, DtlsHandler.this);
                if (dtlsTransport == null) {
                    log.warn("|DtlsHandler({})| clientProtocol.connect result is null", conferenceId);
                }
                // Prepare the shared key to be used in RTP streaming
                client.prepareSrtpSharedSecret();

                // Generate encoders for DTLS traffic
                PacketTransformer packetTransformer = srtpDecoder = generateRtpDecoder(false);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeClient: generateRtpDecoder is null", conferenceId);
                }
                packetTransformer = srtpEncoder = generateRtpEncoder(false);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeClient: generateRtpEncoder is null", conferenceId);
                }
                packetTransformer = srtcpDecoder = generateRtcpDecoder(false);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeClient: generateRtcpDecoder is null", conferenceId);
                }
                packetTransformer = srtcpEncoder = generateRtcpEncoder(false);
                if (packetTransformer == null) {
                    log.warn("|DtlsHandler({})| DtlsHandler.HandshakeClient: generateRtcpEncoder is null", conferenceId);
                }

                // Declare handshake as complete
                handshakeComplete = true;
                handshakeFailed = false;
                handshaking = false;

                // Warn listeners handshake completed
                fireHandshakeComplete();
            } catch (Exception e) {
                log.error("|DtlsHandler({})| DtlsHandler.HandshakeClient: DTLS handshake failed. Reason:", conferenceId, e);

                // Declare handshake as failed
                handshakeComplete = false;
                handshakeFailed = true;
                handshaking = false;

                // Warn listeners handshake completed
                fireHandshakeFailed(e);
            }
        }

    }

}

