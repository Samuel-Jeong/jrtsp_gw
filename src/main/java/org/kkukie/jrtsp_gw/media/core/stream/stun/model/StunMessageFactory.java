package org.kkukie.jrtsp_gw.media.core.stream.stun.model;

import org.kkukie.jrtsp_gw.media.core.stream.rtp.channels.TransportAddress;
import org.kkukie.jrtsp_gw.media.core.stream.stun.exception.StunException;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunMessage;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunRequest;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.StunResponse;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.StunAttributeFactory;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.address.XorMappedAddressAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.FingerprintAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.MessageIntegrityAttribute;
import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.general.UsernameAttribute;

import java.nio.charset.StandardCharsets;

public class StunMessageFactory {

    private StunMessageFactory() {}

    public static StunResponse createBindingResponse(StunRequest request, TransportAddress mappedAddress) throws IllegalArgumentException {
        StunResponse bindingResponse = new StunResponse();
        bindingResponse.setMessageType(StunMessage.BINDING_SUCCESS_RESPONSE);

        // XOR mapped address
        XorMappedAddressAttribute xorMappedAddressAttribute = StunAttributeFactory.createXorMappedAddressAttribute(
                mappedAddress, request.getTransactionId()
        );
        bindingResponse.addAttribute(xorMappedAddressAttribute);

        return bindingResponse;
    }

    public static StunRequest createBindingRequest(String username, String remoteIcePwd) throws IllegalArgumentException, StunException {
        StunRequest bindingRequest = new StunRequest();

        bindingRequest.setMessageType(StunMessage.BINDING_REQUEST);
        bindingRequest.setTransactionID(TransactionId.createNewTransactionID().getBytes());

        // USERNAME
        UsernameAttribute usernameAttribute = new UsernameAttribute();
        usernameAttribute.setUsername(username.getBytes(StandardCharsets.UTF_8));
        bindingRequest.addAttribute(usernameAttribute);

        // MESSAGE INTEGRITY
        MessageIntegrityAttribute messageIntegrityAttribute = StunAttributeFactory.createMessageIntegrityAttribute(
                username, remoteIcePwd.getBytes(StandardCharsets.UTF_8)
        );
        bindingRequest.addAttribute(messageIntegrityAttribute);

        // FINGERPRINT
        FingerprintAttribute fingerprintAttribute = StunAttributeFactory.createFingerprintAttribute();
        bindingRequest.addAttribute(fingerprintAttribute);

        return bindingRequest;
    }

}
