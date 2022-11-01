package org.kkukie.jrtsp_gw.media.core.stream.rtp.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.SRTPProtectionProfile;

public enum SRTPParameters {

    // DTLS derived key and salt lengths for SRTP
    // http://tools.ietf.org/html/rfc5764#section-4.1.2

    //	SRTP_AES128_CM_HMAC_SHA1_80 (SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80, SRTPPolicy.AESCM_ENCRYPTION, 128, SRTPPolicy.HMACSHA1_AUTHENTICATION, 160, 80, 80, 112),
//	SRTP_AES128_CM_HMAC_SHA1_32 (SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32, SRTPPolicy.AESCM_ENCRYPTION, 128, SRTPPolicy.HMACSHA1_AUTHENTICATION, 160, 32, 80, 112),
    // hrosa - converted lengths to work with bytes, not bits (1 byte = 8 bits)
    SRTP_AES128_CM_HMAC_SHA1_80(SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80, SRTPPolicy.AESCM_ENCRYPTION, 16, SRTPPolicy.HMACSHA1_AUTHENTICATION, 20, 10, 10, 14),
    SRTP_AES128_CM_HMAC_SHA1_32(SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32, SRTPPolicy.AESCM_ENCRYPTION, 16, SRTPPolicy.HMACSHA1_AUTHENTICATION, 20, 4, 10, 14),
    SRTP_NULL_HMAC_SHA1_80(SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80, SRTPPolicy.NULL_ENCRYPTION, 0, SRTPPolicy.HMACSHA1_AUTHENTICATION, 20, 10, 10, 0),
    SRTP_NULL_HMAC_SHA1_32(SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32, SRTPPolicy.NULL_ENCRYPTION, 0, SRTPPolicy.HMACSHA1_AUTHENTICATION, 20, 4, 10, 0);

    private final int profile;
    private final int encType;
    private final int encKeyLength;
    private final int authType;
    private final int authKeyLength;
    private final int authTagLength;
    private final int rtcpAuthTagLength;
    private final int saltLength;

    SRTPParameters (int newProfile, int newEncType, int newEncKeyLength, int newAuthType, int newAuthKeyLength, int newAuthTagLength, int newRtcpAuthTagLength, int newSaltLength) {
        this.profile = newProfile;
        this.encType = newEncType;
        this.encKeyLength = newEncKeyLength;
        this.authType = newAuthType;
        this.authKeyLength = newAuthKeyLength;
        this.authTagLength = newAuthTagLength;
        this.rtcpAuthTagLength = newRtcpAuthTagLength;
        this.saltLength = newSaltLength;
    }

    public static SRTPParameters getSrtpParametersForProfile (int profileValue) {
        switch (profileValue) {
            case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80:
                return SRTP_AES128_CM_HMAC_SHA1_80;
            case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32:
                return SRTP_AES128_CM_HMAC_SHA1_32;
            case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80:
                return SRTP_NULL_HMAC_SHA1_80;
            case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32:
                return SRTP_NULL_HMAC_SHA1_32;
            default:
                throw new IllegalArgumentException("SRTP Protection Profile value %d is not allowed for DTLS SRTP. See http://tools.ietf.org/html/rfc5764#section-4.1.2 for valid values.");
        }
    }

    public int getProfile () {
        return profile;
    }

    public int getCipherKeyLength () {
        return encKeyLength;
    }

    public int getCipherSaltLength () {
        return saltLength;
    }

    /**
     * @return an initialized SRTPPolicy instance that matches the current SRTPParameter values for the SRTP stream
     */
    public SRTPPolicy getSrtpPolicy () {
        return new SRTPPolicy(encType, encKeyLength, authType, authKeyLength, authTagLength, saltLength);
    }

    /**
     * @return an initialized SRTPPolicy instance that matches the current SRTPParameter values for the SRTCP stream
     */
    public SRTPPolicy getSrtcpPolicy () {
        return new SRTPPolicy(encType, encKeyLength, authType, authKeyLength, rtcpAuthTagLength, saltLength);
    }

}
