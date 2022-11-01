/*
 * 
 * Code derived and adapted from the Jitsi client side STUN framework.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.address;

import org.kkukie.jrtsp_gw.media.core.stream.stun.messages.attributes.StunAttribute;

/**
 * The XOR-PEER-ADDRESS attribute is given by a TURN client to indicates the
 * peer destination address of its relayed packet.
 * 
 * It has the same format as XOR-MAPPED-ADDRESS.
 */
public class XorPeerAddressAttribute extends XorMappedAddressAttribute {

	public static final String NAME = "XOR-PEER-ADDRESS";

	public XorPeerAddressAttribute() {
		super(StunAttribute.XOR_PEER_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
}
