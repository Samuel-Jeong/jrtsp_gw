/**
 * Code derived and adapted from the Jitsi client side SRTP framework.
 * <p>
 * Distributed under LGPL license.
 * See terms of license at gnu.org.
 */
package org.kkukie.jrtsp_gw.media.core.stream.rtp.crypto;

/**
 * Defines how to get <tt>PacketTransformer</tt>s for RTP and RTCP packets. A
 * single <tt>PacketTransformer</tt> can be used for both RTP and RTCP packets
 * or there can be two separate <tt>PacketTransformer</tt>s.
 *
 * @author Bing SU (nova.su@gmail.com)
 */
public interface TransformEngine {

    /**
     * Gets the <tt>PacketTransformer</tt> for RTP packets.
     *
     * @return the <tt>PacketTransformer</tt> for RTP packets
     */
    PacketTransformer getRTPTransformer ();

    /**
     * Gets the <tt>PacketTransformer</tt> for RTCP packets.
     *
     * @return the <tt>PacketTransformer</tt> for RTCP packets
     */
    PacketTransformer getRTCPTransformer ();

}
