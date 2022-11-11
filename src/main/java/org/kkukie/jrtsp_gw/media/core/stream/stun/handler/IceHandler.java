package org.kkukie.jrtsp_gw.media.core.stream.stun.handler;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.model.DataChannel;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandler;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.PacketHandlerException;
import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.TransportAddress;
import org.kkukie.jrtsp_gw.media.core.stream.stun.events.IceEventListener;
import org.kkukie.jrtsp_gw.media.core.stream.stun.events.SelectedCandidatesEvent;
import org.kkukie.jrtsp_gw.media.core.stream.stun.exception.StunException;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunMessage;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunResponse;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.StunAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.StunAttributeFactory;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.ErrorCodeAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.MessageIntegrityAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.UsernameAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.model.StunMessageFactory;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.model.ice.IceInfo;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunMessage.MAGIC_COOKIE;

@Slf4j
public class IceHandler implements PacketHandler {

    private final String conferenceId;
    private final short componentId;
    private final IceEventListener iceListener;
    private final AtomicBoolean candidateSelected;
    private IceAuthenticator authenticator;
    private int pipelinePriority = 1;

    private HarvestHandler harvestHandler = null;

    public IceHandler(String conferenceId, short componentId, IceEventListener iceListener) {
        this.conferenceId = conferenceId;

        switch (componentId) {
            case 1:
            case 2:
                this.componentId = componentId;
                this.iceListener = iceListener;
                this.candidateSelected = new AtomicBoolean(false);
                return;
            default:
                throw new IllegalArgumentException("|IceHandler(" + conferenceId + ")| Invalid component ID: " + componentId);
        }
    }

    public short getComponentId() {
        return this.componentId;
    }

    public void setAuthenticator(IceAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    public int compareTo(PacketHandler packetHandler) {
        return (packetHandler == null) ? 1 : (this.getPipelinePriority() - packetHandler.getPipelinePriority());
    }

    public boolean canHandle(byte[] packet) {
        return this.canHandle(packet, packet.length, 0);
    }

    public boolean canHandle(byte[] packet, int dataLength, int offset) {
        byte b0 = packet[offset];
        int b0Int = b0 & 255;
        if (b0Int < 2 && dataLength >= 20) {
            boolean firstBitsValid = ((b0 & 192) == 0);
            boolean hasMagicCookie = (packet[offset + 4] == MAGIC_COOKIE[0])
                    && (packet[offset + 5] == MAGIC_COOKIE[1])
                    && (packet[offset + 6] == MAGIC_COOKIE[2])
                    && (packet[offset + 7] == MAGIC_COOKIE[3]);
            return firstBitsValid && hasMagicCookie;
        } else {
            return false;
        }
    }

    public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
        return this.handle(packet, packet.length, 0, localPeer, remotePeer);
    }

    public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
        try {
            StunMessage message = StunMessage.decode(packet, (char)offset, (char)dataLength);
            if (message instanceof StunRequest) {
                return this.processRequest((StunRequest)message, localPeer, remotePeer);
            } else {
                return message instanceof StunResponse ? this.processResponse((StunResponse)message, remotePeer) : null;
            }
        } catch (StunException stunException) {
            throw new PacketHandlerException("|IceHandler(" + conferenceId + ")| Could not decode STUN packet", stunException);
        } catch (IOException ioException) {
            throw new PacketHandlerException(ioException.getMessage(), ioException);
        }
    }

    private byte[] processRequest(StunRequest request, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws IOException, StunException {
        byte[] transactionID = request.getTransactionId();

        UsernameAttribute remoteUnameAttribute = (UsernameAttribute)request.getAttribute('\u0006');
        if (remoteUnameAttribute == null) {
            StunResponse errorResponse = new StunResponse();
            errorResponse.setTransactionID(transactionID);
            errorResponse.setMessageType('đ');
            errorResponse.addAttribute(StunAttributeFactory.createErrorCodeAttribute('Ɛ', ErrorCodeAttribute.getDefaultReasonPhrase('Ɛ')));
            return errorResponse.encode();
        } else {
            log.debug("|HarvestHandler({})| Recv StunRequest(tid={}) from [{}].",
                    conferenceId, DatatypeConverter.printHexBinary(transactionID), remotePeer
            );

            String remoteUsername = new String(remoteUnameAttribute.getUsername(), StandardCharsets.UTF_8).trim();
            if (!this.authenticator.validateUsername(remoteUsername)) {
                log.warn("|IceHandler({})| validateUsername: fail. (remoteUsername={})", conferenceId, remoteUsername);
                return null;
            } else {
                int colonPos = remoteUsername.indexOf(":");
                String localUFrag = remoteUsername.substring(0, colonPos);
                String remoteUfrag = remoteUsername.substring(colonPos + 1);
                TransportAddress transportAddress = new TransportAddress(remotePeer.getAddress(), remotePeer.getPort(), TransportAddress.TransportProtocol.UDP);
                StunResponse response = StunMessageFactory.createBindingResponse(request, transportAddress);

                try {
                    response.setTransactionID(transactionID);
                } catch (StunException stunException) {
                    throw new IOException("|IceHandler(" + conferenceId + ")| Illegal STUN Transaction ID: " + new String(transactionID), stunException);
                }

                String localUsername = remoteUfrag.concat(":").concat(localUFrag);
                StunAttribute unameAttribute = StunAttributeFactory.createUsernameAttribute(localUsername);
                response.addAttribute(unameAttribute);

                byte[] localKey = this.authenticator.getLocalKey(localUFrag);
                MessageIntegrityAttribute integrityAttribute = StunAttributeFactory.createMessageIntegrityAttribute(remoteUsername, localKey);
                response.addAttribute(integrityAttribute);

                if (!this.candidateSelected.get()) {
                    this.candidateSelected.set(true);
                    if (log.isDebugEnabled()) {
                        log.debug("|IceHandler({})| Selected candidate={} (local={})", conferenceId, remotePeer, localPeer.toString());
                    }

                    this.iceListener.onSelectedCandidates(
                            new SelectedCandidatesEvent(remotePeer),
                            request.containsAttribute('%')
                    );
                }

                log.debug("|HarvestHandler({})| Send StunResponse(tid={}) to [{}].",
                        conferenceId, DatatypeConverter.printHexBinary(response.getTransactionId()), remotePeer
                );
                return response.encode();
            }
        }
    }

    private byte[] processResponse(StunResponse response, InetSocketAddress remotePeer) {
        if (!response.isSuccessResponse()) { return null; }
        try {
            for (StunAttribute attribute : response.getAttributes()) {
                if (attribute == null) {
                    continue;
                }

                char attributeType = attribute.getAttributeType();
                if (attributeType == StunAttribute.MESSAGE_INTEGRITY) {
                    log.debug("|HarvestHandler({})| Recv StunResponse(tid={}) from [{}].",
                            conferenceId, DatatypeConverter.printHexBinary(response.getTransactionId()), remotePeer
                    );
                    this.iceListener.onSelectedCandidates(
                            new SelectedCandidatesEvent(remotePeer),
                            true
                    );
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("|IceHandler({})| processResponse.Exception", conferenceId, e);
        }
        return null;
    }

    public int getPipelinePriority() {
        return this.pipelinePriority;
    }

    public void setPipelinePriority(int pipelinePriority) {
        this.pipelinePriority = pipelinePriority;
    }

    public void startHarvester(DataChannel dataChannel, IceInfo iceInfo, List<InetSocketAddress> targetAddressList) {
        stopHarvester();
        harvestHandler = new HarvestHandler(conferenceId);
        harvestHandler.start(dataChannel, iceInfo, targetAddressList);
    }

    public void stopHarvester() {
        if (harvestHandler != null) {
            harvestHandler.stop();
            harvestHandler = null;
        }
    }

}