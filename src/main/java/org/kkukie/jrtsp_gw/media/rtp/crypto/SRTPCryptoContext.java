/**
 * Code derived and adapted from the Jitsi client side SRTP framework.
 * <p>
 * Distributed under LGPL license.
 * See terms of license at gnu.org.
 */
package org.kkukie.jrtsp_gw.media.rtp.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.BlockCipher;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Mac;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.digests.SHA1Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.digests.SHA256Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.engines.AESFastEngine;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.engines.TwofishEngine;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.macs.HMac;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.KeyParameter;
import org.kkukie.jrtsp_gw.media.rtp.RtpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Formatter;

/**
 * SRTPCryptoContext class is the core class of SRTP implementation. There can
 * be multiple SRTP sources in one SRTP session. And each SRTP stream has a
 * corresponding SRTPCryptoContext object, identified by SSRC. In this way,
 * different sources can be protected independently.
 * <p>
 * SRTPCryptoContext class acts as a manager class and maintains all the
 * information used in SRTP transformation. It is responsible for deriving
 * encryption keys / salting keys / authentication keys from master keys. And it
 * will invoke certain class to encrypt / decrypt (transform / reverse
 * transform) RTP packets. It will hold a replay check db and do replay check
 * against incoming packets.
 * <p>
 * Refer to section 3.2 in RFC3711 for detailed description of cryptographic
 * context.
 * <p>
 * Cryptographic related parameters, i.e. encryption mode / authentication mode,
 * master encryption key and master salt key are determined outside the scope of
 * SRTP implementation. They can be assigned manually, or can be assigned
 * automatically using some key management protocol, such as MIKEY (RFC3830),
 * SDES (RFC4568) or Phil Zimmermann's ZRTP protocol (RFC6189).
 *
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTPCryptoContext {

    private static final Logger logger = LoggerFactory.getLogger(SRTPCryptoContext.class);

    /**
     * The replay check windows size
     */
    private static final long REPLAY_WINDOW_SIZE = 64;
    /**
     * Encryption / Authentication policy for this session
     */
    private final SRTPPolicy policy;
    /**
     * implements the counter cipher mode for RTP according to RFC 3711
     */
    private final SRTPCipherCTR cipherCtr = new SRTPCipherCTR();
    /**
     * Temp store.
     */
    private final byte[] tagStore;
    /**
     * Temp store.
     */
    private final byte[] ivStore = new byte[16];
    /**
     * Temp store.
     */
    private final byte[] rbStore = new byte[4];
    /**
     * this is a working store, used by some methods to avoid new operations the
     * methods must use this only to store results for immediate processing
     */
    private final byte[] tempStore = new byte[100];
    byte[] tempBuffer = new byte[RtpPacket.RTP_PACKET_MAX_SIZE];
    /**
     * RTP SSRC of this cryptographic context
     */
    private final long ssrcCtx;
    /**
     * Master key identifier
     */
    private final byte[] mki;
    /**
     * Roll-Over-Counter, see RFC3711 section 3.2.1 for detailed description
     * a 32-bit unsigned rollover counter (ROC), which records how many
     *       times the 16-bit RTP sequence number has been reset to zero after
     *       passing through 65,535.
     */
    private int roc;
    /**
     * Roll-Over-Counter guessed from packet
     */
    private int guessedROC;
    /**
     * RTP sequence number of the packet current processing
     */
    private int seqNum;
    /**
     * Whether we have the sequence number of current packet
     */
    private boolean seqNumSet;
    /**
     * Key Derivation Rate, used to derive session keys from master keys
     */
    private final long keyDerivationRate;
    /**
     * Bit mask for replay check
     */
    private long replayWindow;
    /**
     * Master encryption key
     */
    private final byte[] masterKey;
    /**
     * Master salting key
     */
    private final byte[] masterSalt;
    /**
     * Derived session encryption key
     */
    private final byte[] encKey;
    /**
     * Derived session authentication key
     */
    private byte[] authKey;
    /**
     * Derived session salting key
     */
    private final byte[] saltKey;
    /**
     * The HMAC object we used to do packet authentication
     */
    private Mac hmac;
    private Mac hmac256;
    /**
     * The symmetric cipher engines we need here
     */
    private BlockCipher cipher = null;
    /**
     * Used inside F8 mode only
     */
    private BlockCipher cipherF8 = null;

    /**
     * Construct an empty SRTPCryptoContext using ssrc. The other parameters are
     * set to default null value.
     *
     * @param ssrcIn SSRC of this SRTPCryptoContext
     */
    public SRTPCryptoContext (long ssrcIn) {
        ssrcCtx = ssrcIn;
        mki = null;
        roc = 0;
        guessedROC = 0;
        seqNum = 0;
        keyDerivationRate = 0;
        masterKey = null;
        masterSalt = null;
        encKey = null;
        authKey = null;
        saltKey = null;
        seqNumSet = false;
        policy = null;
        tagStore = null;
    }

    /**
     * Construct a normal SRTPCryptoContext based on the given parameters.
     *
     * @param ssrcIn   the RTP SSRC that this SRTP cryptographic context protects.
     * @param rocIn    the initial Roll-Over-Counter according to RFC 3711. These are
     *                 the upper 32 bit of the overall 48 bit SRTP packet index.
     *                 Refer to chapter 3.2.1 of the RFC.
     * @param kdr      the key derivation rate defines when to recompute the SRTP
     *                 session keys. Refer to chapter 4.3.1 in the RFC.
     * @param masterK  byte array holding the master key for this SRTP cryptographic
     *                 context. Refer to chapter 3.2.1 of the RFC about the role of
     *                 the master key.
     * @param masterS  byte array holding the master salt for this SRTP cryptographic
     *                 context. It is used to computer the initialization vector that
     *                 in turn is input to compute the session key, session
     *                 authentication key and the session salt.
     * @param policyIn SRTP policy for this SRTP cryptographic context, defined the
     *                 encryption algorithm, the authentication algorithm, etc
     */
    @SuppressWarnings("fallthrough")
    public SRTPCryptoContext (long ssrcIn, int rocIn, long kdr, byte[] masterK,
                              byte[] masterS, SRTPPolicy policyIn) {
        ssrcCtx = ssrcIn;
        mki = null;
        roc = rocIn;
        guessedROC = 0;
        seqNum = 0;
        keyDerivationRate = kdr;
        seqNumSet = false;

        policy = policyIn;

        masterKey = new byte[policy.getEncKeyLength()];
        System.arraycopy(masterK, 0, masterKey, 0, masterK.length);

        masterSalt = new byte[policy.getSaltKeyLength()];
        System.arraycopy(masterS, 0, masterSalt, 0, masterS.length);

        hmac = new HMac(new SHA1Digest());
        hmac256 = new HMac(new SHA256Digest());

        switch (policy.getEncType()) {
            case SRTPPolicy.AESF8_ENCRYPTION:
                cipherF8 = new AESFastEngine();
                //$FALL-THROUGH$

            case SRTPPolicy.AESCM_ENCRYPTION:
                cipher = new AESFastEngine();
                encKey = new byte[policy.getEncKeyLength()];
                saltKey = new byte[policy.getSaltKeyLength()];
                break;

            case SRTPPolicy.TWOFISHF8_ENCRYPTION:
                cipherF8 = new TwofishEngine();

            case SRTPPolicy.TWOFISH_ENCRYPTION:
                cipher = new TwofishEngine();
                encKey = new byte[this.policy.getEncKeyLength()];
                saltKey = new byte[this.policy.getSaltKeyLength()];
                break;

            case SRTPPolicy.NULL_ENCRYPTION:
            default:
                encKey = null;
                saltKey = null;
                break;
        }

        switch (policy.getAuthType()) {
            case SRTPPolicy.NULL_AUTHENTICATION:
                authKey = null;
                tagStore = null;
                break;

            case SRTPPolicy.HMACSHA1_AUTHENTICATION:
                hmac = new HMac(new SHA1Digest());
                authKey = new byte[policy.getAuthKeyLength()];
                tagStore = new byte[hmac.getMacSize()];
                break;

            default:
                tagStore = null;
        }
    }

    /**
     * Close the crypto context.
     * <p>
     * The close functions deletes key data and performs a cleanup of the crypto
     * context.
     * <p>
     * Clean up key data, maybe this is the second time however, sometimes we
     * cannot know if the CryptoCOntext was used and the application called
     * deriveSrtpKeys(...) .
     */
    public void close () {
        Arrays.fill(masterKey, (byte) 0);
        Arrays.fill(masterSalt, (byte) 0);
    }

    /**
     * Get the authentication tag length of this SRTP cryptographic context
     *
     * @return the authentication tag length of this SRTP cryptographic context
     */
    public int getAuthTagLength () {
        return policy.getAuthTagLength();
    }

    /**
     * Get the MKI length of this SRTP cryptographic context
     *
     * @return the MKI length of this SRTP cryptographic context
     */
    public int getMKILength () {
        return this.mki == null ? 0 : this.mki.length;
    }

    /**
     * Get the SSRC of this SRTP cryptographic context
     *
     * @return the SSRC of this SRTP cryptographic context
     */
    public long getSSRC () {
        return ssrcCtx;
    }

    /**
     * Get the Roll-Over-Counter of this SRTP cryptographic context
     *
     * @return the Roll-Over-Counter of this SRTP cryptographic context
     */
    public int getROC () {
        return roc;
    }

    /**
     * Set the Roll-Over-Counter of this SRTP cryptographic context
     *
     * @param rocIn the Roll-Over-Counter of this SRTP cryptographic context
     */
    public void setROC (int rocIn) {
        roc = rocIn;
    }

    /**
     * Transform a RTP packet into a SRTP packet. This method is called when a
     * normal RTP packet ready to be sent.
     * <p>
     * Operations done by the transformation may include: encryption, using
     * either Counter Mode encryption, or F8 Mode encryption, adding
     * authentication tag, currently HMC SHA1 method.
     * <p>
     * Both encryption and authentication functionality can be turned off as
     * long as the SRTPPolicy used in this SRTPCryptoContext is requires no
     * encryption and no authentication. Then the packet will be sent out
     * untouched. However this is not encouraged. If no SRTP feature is enabled,
     * then we shall not use SRTP TransformConnector. We should use the original
     * method (RTPManager managed transportation) instead.
     *
     * @param pkt the RTP packet that is going to be sent out
     */
    public void transformPacket (RawPacket pkt) {
        /* Encrypt the packet using Counter Mode encryption */
        if (policy.getEncType() == SRTPPolicy.AESCM_ENCRYPTION || policy.getEncType() == SRTPPolicy.TWOFISH_ENCRYPTION) {
            processPacketAESCM(pkt);
        } else if (policy.getEncType() == SRTPPolicy.AESF8_ENCRYPTION || policy.getEncType() == SRTPPolicy.TWOFISHF8_ENCRYPTION) {
            /* Encrypt the packet using F8 Mode encryption */
            processPacketAESF8(pkt);
        }

        /* Authenticate the packet */
        if (policy.getAuthType() != SRTPPolicy.NULL_AUTHENTICATION) {
            authenticatePacketHMCSHA1(pkt, roc);
            pkt.append(tagStore, policy.getAuthTagLength());
        }

        /* Update the ROC if necessary */
        int seqNo = pkt.getSequenceNumber();
        if (seqNo == 0xFFFF) {
            roc++;
        }
    }

    /**
     * Transform a SRTP packet into a RTP packet. This method is called when a
     * SRTP packet is received.
     * <p>
     * Operations done by the this operation include: Authentication check,
     * Packet replay check and Decryption.
     * <p>
     * Both encryption and authentication functionality can be turned off as
     * long as the SRTPPolicy used in this SRTPCryptoContext requires no
     * encryption and no authentication. Then the packet will be sent out
     * untouched. However this is not encouraged. If no SRTP feature is enabled,
     * then we shall not use SRTP TransformConnector. We should use the original
     * method (RTPManager managed transportation) instead.
     *
     * @param pkt the RTP packet that is just received
     * @return true if the packet can be accepted false if the packet failed
     * authentication or failed replay check
     */
    public boolean reverseTransformPacket (RawPacket pkt) {
        int seqNo = pkt.getSequenceNumber();

        if (!seqNumSet) {
            seqNumSet = true;
            seqNum = seqNo;
        }

        // Guess the SRTP index (48 bit), see rFC 3711, 3.3.1
        // Stores the guessed roc in this.guessedROC
        long guessedIndex = guessIndex(seqNo);
        // Replay control
        if (!checkReplay(seqNo, guessedIndex)) {
            return false;
        }

        // Authenticate packet
        if (policy.getAuthType() != SRTPPolicy.NULL_AUTHENTICATION) {
            int tagLength = policy.getAuthTagLength();

            // get original authentication and store in tempStore
            pkt.readRegionToBuff(pkt.getLength() - tagLength, tagLength, tempStore);
            pkt.shrink(tagLength);

            // save computed authentication in tagStore
            authenticatePacketHMCSHA1(pkt, guessedROC);

            for (int i = 0; i < tagLength; i++) {
                if ((tempStore[i] & 0xff) != (tagStore[i] & 0xff)) {
                    return false;
                }
            }
        }

        // Decrypt packet
        switch (policy.getEncType()) {
            case SRTPPolicy.AESCM_ENCRYPTION:
            case SRTPPolicy.TWOFISH_ENCRYPTION:
                // using Counter Mode encryption
                processPacketAESCM(pkt);
                break;

            case SRTPPolicy.AESF8_ENCRYPTION:
            case SRTPPolicy.TWOFISHF8_ENCRYPTION:
                // using F8 Mode encryption
                processPacketAESF8(pkt);
                break;

            default:
                return false;
        }

        update(seqNo, guessedIndex);
        return true;
    }

    /**
     * Perform Counter Mode AES encryption / decryption
     *
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESCM (RawPacket pkt) {
        long ssrc = pkt.getSSRC();
        int seqNo = pkt.getSequenceNumber();
        long index = ((long) this.roc << 16) | seqNo;

        ivStore[0] = saltKey[0];
        ivStore[1] = saltKey[1];
        ivStore[2] = saltKey[2];
        ivStore[3] = saltKey[3];

        int i;
        for (i = 4; i < 8; i++) {
            ivStore[i] = (byte) ((0xFF & (ssrc >> ((7 - i) * 8))) ^ this.saltKey[i]);
        }

        for (i = 8; i < 14; i++) {
            ivStore[i] = (byte) ((0xFF & (byte) (index >> ((13 - i) * 8))) ^ this.saltKey[i]);
        }

        ivStore[14] = ivStore[15] = 0;

        final int payloadOffset = pkt.getHeaderLength();
        final int payloadLength = pkt.getPayloadLength();

        cipherCtr.process(cipher, pkt.getBuffer(), payloadOffset, payloadLength, ivStore);
    }

    /**
     * Perform F8 Mode AES encryption / decryption
     *
     * @param pkt the RTP packet to be encrypted / decrypted
     */
    public void processPacketAESF8 (RawPacket pkt) {
        // 11 bytes of the RTP header are the 11 bytes of the iv
        // the first byte of the RTP header is not used.
        ByteBuffer buf = pkt.getBuffer();
        buf.compact();
        buf.get(ivStore, 0, 12);
        ivStore[0] = 0;

        // set the ROC in network order into IV
        ivStore[12] = (byte) (this.roc >> 24);
        ivStore[13] = (byte) (this.roc >> 16);
        ivStore[14] = (byte) (this.roc >> 8);
        ivStore[15] = (byte) this.roc;

        final int payloadOffset = pkt.getHeaderLength();
        final int payloadLength = pkt.getPayloadLength();

        SRTPCipherF8.process(cipher, pkt.getBuffer(), payloadOffset, payloadLength, ivStore, cipherF8);
    }

    /**
     * Authenticate a packet. Calculated authentication tag is returned.
     *
     * @param pkt   the RTP packet to be authenticated
     * @param rocIn Roll-Over-Counter
     */
    private void authenticatePacketHMCSHA1 (RawPacket pkt, int rocIn) {
        ByteBuffer buf = pkt.getBuffer();
        buf.rewind();
        int len = buf.remaining();
        buf.get(tempBuffer, 0, len);
        hmac.update(tempBuffer, 0, len);
        rbStore[0] = (byte) (rocIn >> 24);
        rbStore[1] = (byte) (rocIn >> 16);
        rbStore[2] = (byte) (rocIn >> 8);
        rbStore[3] = (byte) rocIn;
        hmac.update(rbStore, 0, rbStore.length);
        hmac.doFinal(tagStore, 0);
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    /**
     * Checks if a packet is a replayed on based on its sequence number.
     * <p>
     * This method supports a 64 packet history relative the the given sequence
     * number.
     * <p>
     * Sequence Number is guaranteed to be real (not faked) through
     * authentication.
     *
     * @param seqNo        sequence number of the packet
     * @param guessedIndex guessed roc
     * @return true if this sequence number indicates the packet is not a
     * replayed one, false if not
     */
    boolean checkReplay (int seqNo, long guessedIndex) {
        // compute the index of previously received packet and its
        // delta to the new received packet
        long localIndex = (((long) this.roc) << 16) | this.seqNum;
        long delta = guessedIndex - localIndex;

        if (delta > 0) {
            /* Packet not yet received */
            return true;
        } else {
            if (-delta > REPLAY_WINDOW_SIZE) {
                /* Packet too old */
                return false;
            } else {
                /* Packet already received ! */
                /* Packet not yet received */
                return ((this.replayWindow >> (-delta)) & 0x1) == 0;
            }
        }
    }

    /**
     * Compute the initialization vector, used later by encryption algorithms,
     * based on the lable, the packet index, key derivation rate and master salt
     * key.
     *
     * @param label label specified for each type of iv
     * @param index 48bit RTP packet index
     */
    private void computeIv (long label, long index) {
        long key_id;

        if (keyDerivationRate == 0) {
            key_id = label << 48;
        } else {
            key_id = ((label << 48) | (index / keyDerivationRate));
        }

        System.arraycopy(masterSalt, 0, ivStore, 0, 7);

        for (int i = 7; i < 14; i++) {
            ivStore[i] = (byte) ((byte) (0xFF & (key_id >> (8 * (13 - i)))) ^ masterSalt[i]);
        }

        ivStore[14] = ivStore[15] = 0;
    }

    /**
     * Derives the srtp session keys from the master key
     *
     * @param index the 48 bit SRTP packet index
     */
    public void deriveSrtpKeys (long index) {
        // compute the session encryption key
        long label = 0;
        computeIv(label, index);

        KeyParameter encryptionKey = new KeyParameter(masterKey);
        cipher.init(true, encryptionKey);
        Arrays.fill(masterKey, (byte) 0);

        cipherCtr.getCipherStream(cipher, encKey, policy.getEncKeyLength(), ivStore);

        // compute the session authentication key
        if (authKey != null) {
            label = 0x01;
            computeIv(label, index);
            cipherCtr.getCipherStream(cipher, authKey, policy.getAuthKeyLength(), ivStore);

            if ((policy.getAuthType()) == SRTPPolicy.HMACSHA1_AUTHENTICATION) {
                KeyParameter key = new KeyParameter(authKey);
                hmac.init(key);
            }

            Arrays.fill(authKey, (byte) 0);
        }

        // compute the session salt
        label = 0x02;
        computeIv(label, index);
        cipherCtr.getCipherStream(cipher, saltKey, policy.getSaltKeyLength(), ivStore);
        Arrays.fill(masterSalt, (byte) 0);

        // As last step: initialize cipher with derived encryption key.
        if (cipherF8 != null) {
            SRTPCipherF8.deriveForIV(cipherF8, encKey, saltKey);
        }
        encryptionKey = new KeyParameter(encKey);
        cipher.init(true, encryptionKey);

        Arrays.fill(encKey, (byte) 0);
    }

    /**
     * Compute (guess) the new SRTP index based on the sequence number of a
     * received RTP packet.
     *
     * @param seqNo sequence number of the received RTP packet
     * @return the new SRTP packet index
     */
    private long guessIndex (int seqNo) {
        if (this.seqNum < 32768) {
            if (seqNo - this.seqNum > 32768) {
                guessedROC = roc - 1;
            } else {
                guessedROC = roc;
            }
        } else {
            if (this.seqNum - 32768 > seqNo) {
                guessedROC = roc + 1;
            } else {
                guessedROC = roc;
            }
        }

        return ((long) guessedROC) << 16 | seqNo;
    }

    /**
     * Update the SRTP packet index.
     * <p>
     * This method is called after all checks were successful. See section 3.3.1
     * in RFC3711 for detailed description.
     *
     * @param seqNo        sequence number of the accepted packet
     * @param guessedIndex guessed roc
     */
    private void update (int seqNo, long guessedIndex) {
        long delta = guessedIndex - (((long) this.roc) << 16 | this.seqNum);

        /* update the replay bit mask */
        if (delta > 0) {
            replayWindow = replayWindow << delta;
            replayWindow |= 1;
        } else {
            replayWindow |= (1L << delta);
        }

        if (seqNo > seqNum) {
            //logger.debug("SRTPCryptoContext.update: Updated seq number. [{} > {}]", this.seqNum, seqNo);
            seqNum = seqNo & 0xffff;
        }
        if (this.guessedROC > this.roc) {
            roc = guessedROC;
            seqNum = seqNo & 0xffff;
        }
    }

    /**
     * Derive a new SRTPCryptoContext for use with a new SSRC
     * <p>
     * This method returns a new SRTPCryptoContext initialized with the data of
     * this SRTPCryptoContext. Replacing the SSRC, Roll-over-Counter, and the
     * key derivation rate the application cab use this SRTPCryptoContext to
     * encrypt / decrypt a new stream (Synchronization source) inside one RTP
     * session.
     * <p>
     * Before the application can use this SRTPCryptoContext it must call the
     * deriveSrtpKeys method.
     *
     * @param ssrc       The SSRC for this context
     * @param roc        The Roll-Over-Counter for this context
     * @param deriveRate The key derivation rate for this context
     * @return a new SRTPCryptoContext with all relevant data set.
     */
    public SRTPCryptoContext deriveContext (long ssrc, int roc, long deriveRate) {
        return new SRTPCryptoContext(ssrc, roc, deriveRate, masterKey, masterSalt, policy);
    }

}
