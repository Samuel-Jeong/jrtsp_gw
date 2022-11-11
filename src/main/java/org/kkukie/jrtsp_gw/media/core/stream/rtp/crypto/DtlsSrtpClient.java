/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.kkukie.jrtsp_gw.media.core.stream.rtp.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class DtlsSrtpClient extends DefaultTlsClient {

    private static final Logger logger = LoggerFactory.getLogger(DtlsSrtpClient.class);

    private final ProtocolVersion minVersion;
    private final ProtocolVersion maxVersion;

    // TlsCertificate resources
    private final String[] certificateResources;
    private final String keyResource;
    private final AlgorithmCertificate algorithmCertificate;
    private final CipherSuite[] cipherSuites;
    private String hashFunction = "";
    // Asymmetric shared keys derived from the DTLS handshake and used for the SRTP encryption/
    private byte[] srtpMasterClientKey;
    private byte[] srtpMasterServerKey;
    private byte[] srtpMasterClientSalt;
    private byte[] srtpMasterServerSalt;

    // Policies
    private SRTPPolicy srtpPolicy;
    private SRTPPolicy srtcpPolicy;

    private UseSRTPData clientSrtpData;

    private TlsSession session = null;

    private byte[] masterSecret = null;

    public DtlsSrtpClient (ProtocolVersion minVersion, ProtocolVersion maxVersion, CipherSuite[] cipherSuites,
                           String[] certificatesPath, String keyPath, AlgorithmCertificate algorithmCertificate) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.cipherSuites = cipherSuites;
        this.certificateResources = certificatesPath;
        this.keyResource = keyPath;
        this.algorithmCertificate = algorithmCertificate;
    }

    @Override
    public void init (TlsClientContext context) {
        this.context = context;
    }

    public TlsSession getSessionToResume () {
        return this.session;
    }

    public ProtocolVersion getMinimumVersion () {
        return minVersion;
    }

    public ProtocolVersion getClientVersion () {
        return maxVersion;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Hashtable<Integer, byte[]> getClientExtensions () throws IOException {
        //see : http://bouncy-castle.1462172.n4.nabble.com/DTLS-SRTP-with-bouncycastle-1-49-td4656286.html
        Hashtable<Integer, byte[]> clientExtensions = super.getClientExtensions();
        if (clientExtensions == null) {
            clientExtensions = new Hashtable<>();
        }
        int[] protectionProfiles = {SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80}; //this is the only one supported for now
        byte[] mki = new byte[]{};  //do not use mki
        clientSrtpData = new UseSRTPData(protectionProfiles, mki);
        TlsSRTPUtils.addUseSRTPExtension(clientExtensions, clientSrtpData);
        return clientExtensions;
    }

    public void notifyServerVersion (ProtocolVersion version) throws IOException {
        if (!this.getMinimumVersion().isEqualOrEarlierVersionOf(version)) {
            throw new TlsFatalAlert((short) 70);
        }
    }

    public void notifySelectedCipherSuite (int selectedCipherSuite) {
        super.notifySelectedCipherSuite(selectedCipherSuite);
    }

    public void notifySelectedCompressionMethod (short selectedCompressionMethod) {
        super.notifySelectedCompressionMethod(selectedCompressionMethod);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void processServerExtensions (Hashtable newServerExtensions) throws IOException {
        super.processServerExtensions(newServerExtensions);
    }


    /**
     * Gets the fingerprint of the TlsCertificate associated to the server.
     *
     * @return The fingerprint of the server tlsCertificate. Returns an empty
     * String if the server does not contain a tlsCertificate.
     */
    public String generateFingerprint (String hashFunction) {
        try {
            this.hashFunction = hashFunction;
            TlsCertificate dtlsCertChain = TlsUtils.loadCertificateChain(certificateResources);

            org.kkukie.jrtsp_gw.media.bouncycastle.asn1.x509.Certificate certificate = dtlsCertChain.getCertificateAt(0);
            return TlsUtils.fingerprint(this.hashFunction, certificate);
        } catch (IOException e) {
            logger.error("Could not get local fingerprint: {}", e.getMessage());
            return "";
        }
    }


    /**
     * @return the shared secret key that will be used for the SRTP session
     */
    public void prepareSrtpSharedSecret () {
        logger.debug("DtlsSrtpClient: Preparing SRTP Shared Secret...");
        SRTPParameters srtpParams = SRTPParameters.getSrtpParametersForProfile(clientSrtpData.getProtectionProfiles()[0]);
        final int keyLen = srtpParams.getCipherKeyLength();
        final int saltLen = srtpParams.getCipherSaltLength();

        srtpPolicy = srtpParams.getSrtpPolicy();
        srtcpPolicy = srtpParams.getSrtcpPolicy();

        srtpMasterClientKey = new byte[keyLen];
        srtpMasterServerKey = new byte[keyLen];
        srtpMasterClientSalt = new byte[saltLen];
        srtpMasterServerSalt = new byte[saltLen];

        // 2* (key + salt length) / 8. From http://tools.ietf.org/html/rfc5764#section-4-2
        // No need to divide by 8 here since lengths are already in bits
        byte[] sharedSecret = getKeyingMaterial(2 * (keyLen + saltLen));

        /*
         *
         * See: http://tools.ietf.org/html/rfc5764#section-4.2
         *
         * sharedSecret is an equivalent of :
         *
         * struct {
         *     client_write_SRTP_master_key[SRTPSecurityParams.master_key_len];
         *     server_write_SRTP_master_key[SRTPSecurityParams.master_key_len];
         *     client_write_SRTP_master_salt[SRTPSecurityParams.master_salt_len];
         *     server_write_SRTP_master_salt[SRTPSecurityParams.master_salt_len];
         *  } ;
         *
         * Here, client = local configuration, server = remote.
         * NOTE [ivelin]: 'local' makes sense if this code is used from a DTLS SRTP client.
         *                Here we run as a server, so 'local' referring to the client is actually confusing.
         *
         * l(k) = KEY length
         * s(k) = salt length
         *
         * So we have the following repartition :
         *                           l(k)                                 2*l(k)+s(k)
         *                                                   2*l(k)                       2*(l(k)+s(k))
         * +------------------------+------------------------+---------------+-------------------+
         * + local key           |    remote key    | local salt   | remote salt   |
         * +------------------------+------------------------+---------------+-------------------+
         */
//        System.arraycopy(sharedSecret, 0, srtpMasterClientKey, 0, keyLen);
//        System.arraycopy(sharedSecret, keyLen, srtpMasterServerKey, 0, keyLen);
//        System.arraycopy(sharedSecret, 2 * keyLen, srtpMasterClientSalt, 0, saltLen);
//        System.arraycopy(sharedSecret, (2 * keyLen + saltLen), srtpMasterServerSalt, 0, saltLen);

        System.arraycopy(sharedSecret, 0, srtpMasterServerKey, 0, keyLen);
        System.arraycopy(sharedSecret, keyLen, srtpMasterClientKey, 0, keyLen);
        System.arraycopy(sharedSecret, 2 * keyLen, srtpMasterServerSalt, 0, saltLen);
        System.arraycopy(sharedSecret, (2 * keyLen + saltLen), srtpMasterClientSalt, 0, saltLen);

        logger.debug("DtlsSrtpClient: SRTP Policy [authType={}] [encType={}]", srtpPolicy.getAuthType(), srtpPolicy.getEncType());
        logger.debug("DtlsSrtpClient: Done.");
    }

    public SRTPPolicy getSrtpPolicy () {
        return srtpPolicy;
    }

    public SRTPPolicy getSrtcpPolicy () {
        return srtcpPolicy;
    }

    public byte[] getSrtpMasterServerKey () {
        return srtpMasterServerKey;
    }

    public byte[] getSrtpMasterServerSalt () {
        return srtpMasterServerSalt;
    }

    public byte[] getSrtpMasterClientKey () {
        return srtpMasterClientKey;
    }

    public byte[] getSrtpMasterClientSalt () {
        return srtpMasterClientSalt;
    }

    public byte[] getKeyingMaterial (int length) {
        context.getSecurityParameters().setMasterSecret(masterSecret);
        return context.exportKeyingMaterial(ExporterLabel.dtls_srtp, null, length);
    }

    @Override
    public void setSrtpMasterKey (byte[] masterSecret) {
        //this.masterSecret = masterSecret;
        this.masterSecret = new byte[masterSecret.length];
        System.arraycopy(masterSecret, 0, this.masterSecret, 0, masterSecret.length);
    }

    public TlsKeyExchange getKeyExchange () throws IOException {
        return super.getKeyExchange();
    }

    @Override
    public TlsAuthentication getAuthentication () throws IOException {
        return new TlsAuthentication() {
            @Override
            public void notifyServerCertificate (TlsCertificate tlsCertificate) throws IOException {
                logger.debug("client notifyServerCertificate");
            }

            @Override
            public TlsCredentials getClientCredentials (CertificateRequest certificateRequest) throws IOException {
                boolean ok = false;
                short[] certificateTypes = certificateRequest.getCertificateTypes();
                if (certificateTypes == null) return null;

                for (short certificateType : certificateTypes) {
                    if (certificateType == ClientCertificateType.rsa_sign) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) return null;

                SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
                Vector sigAlgs = certificateRequest.getSupportedSignatureAlgorithms();
                if (sigAlgs != null) {
                    for (int i = 0; i < sigAlgs.size(); ++i) {
                        SignatureAndHashAlgorithm sigAlg = (SignatureAndHashAlgorithm) sigAlgs.elementAt(i);
                        if (sigAlg.getSignature() == SignatureAlgorithm.rsa) {
                            signatureAndHashAlgorithm = sigAlg;
                            break;
                        }
                    }

                    if (signatureAndHashAlgorithm == null) {
                        return null;
                    }
                }
                AsymmetricKeyParameter dtlsPrivateKey = TlsUtils.loadPrivateKeyResource(keyResource);
                TlsCertificate dtlsCertChain = TlsUtils.loadCertificateChain(certificateResources);
                return new DefaultTlsSignerCredentials(context, dtlsCertChain, dtlsPrivateKey, signatureAndHashAlgorithm);
            }
        };
    }

    public void notifyHandshakeComplete () throws IOException {
        super.notifyHandshakeComplete();

        TlsSession newSession = context.getResumableSession();
        if (newSession != null) {
            this.session = newSession;
        }
//        getKeys();
    }

}
