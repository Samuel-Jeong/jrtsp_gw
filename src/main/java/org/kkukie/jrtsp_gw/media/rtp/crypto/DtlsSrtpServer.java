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

package org.kkukie.jrtsp_gw.media.rtp.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.x509.Certificate;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.*;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents the DTLS SRTP server connection handler.
 * <p>
 * The implementation follows the advise from Pierrick Grasland and Tim Panton on this forum thread:
 * http://bouncy-castle.1462172.n4.nabble.com/DTLS-SRTP-with-bouncycastle-1-49-td4656286.html
 *
 * @author Ivelin Ivanov (ivelin.ivanov@telestax.com)
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class DtlsSrtpServer extends DefaultTlsServer {

    private static final Logger logger = LoggerFactory.getLogger(DtlsSrtpServer.class);

    // TlsCertificate resources
    private final String[] certificateResources;
    private final String keyResource;
    private final AlgorithmCertificate algorithmCertificate;
    private final ProtocolVersion minVersion;
    private final ProtocolVersion maxVersion;
    private final CipherSuite[] cipherSuites;
    private String hashFunction = "";
    // the server response to the client handshake request
    // http://tools.ietf.org/html/rfc5764#section-4.1.1
    private UseSRTPData serverSrtpData;
    // Asymmetric shared keys derived from the DTLS handshake and used for the SRTP encryption/
    private byte[] srtpMasterClientKey;
    private byte[] srtpMasterServerKey;
    private byte[] srtpMasterClientSalt;
    private byte[] srtpMasterServerSalt;
    // Policies
    private SRTPPolicy srtpPolicy;
    private SRTPPolicy srtcpPolicy;

    private byte[] masterSecret = null;

    public DtlsSrtpServer (ProtocolVersion minVersion, ProtocolVersion maxVersion, CipherSuite[] cipherSuites,
                           String[] certificatesPath, String keyPath, AlgorithmCertificate algorithmCertificate) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.cipherSuites = cipherSuites;
        this.certificateResources = certificatesPath;
        this.keyResource = keyPath;
        this.algorithmCertificate = algorithmCertificate;
    }

    @Override
    public void init (TlsServerContext context) {
        this.context = context;
    }

    public void notifyAlertRaised (short alertLevel, short alertDescription, String message, Exception cause) {
        logger.info("DTLS: notifyAlertRaised");
        if (alertLevel == AlertLevel.fatal) {
            logger.error("DTLS server raised alert (AlertLevel.{}, AlertDescription.{}, message='{}' {})", alertLevel, alertDescription, message, cause);
        } else {
            logger.warn("DTLS server raised alert (AlertLevel.{}, AlertDescription.{}, message='{}' {})", alertLevel, alertDescription, message, cause);
        }
    }

    public void notifyAlertReceived (short alertLevel, short alertDescription) {
        logger.info("DTLS: notifyAlertReceived");
        if (alertLevel == AlertLevel.fatal) {
            logger.error("DTLS server received alert (AlertLevel.{}, AlertDescription.{})", alertLevel, alertDescription);
        } else {
            logger.warn("DTLS server received alert (AlertLevel.{}, AlertDescription.{})", alertLevel, alertDescription);
        }
    }

    @Override
    public int getSelectedCipherSuite ( ) throws IOException {
        /*
         * TODO RFC 5246 7.4.3. In order to negotiate correctly, the server MUST check any candidate cipher suites against the
         * "signature_algorithms" extension before selecting them. This is somewhat inelegant but is a compromise designed to
         * minimize changes to the original cipher suite design.
         */

        /*
         * RFC 4429 5.1. A server that receives a  ClientHello containing one or both of these extensions MUST use the client's
         * enumerated capabilities to guide its selection of an appropriate cipher suite. One of the proposed ECC cipher suites
         * must be negotiated only if the server can successfully complete the handshake while using the curves and point
         * formats supported by the client [...].
         */
        logger.info("DTLS: getSelectedCipherSuite");
        boolean eccCipherSuitesEnabled = supportsClientECCCapabilities(this.namedCurves, this.clientECPointFormats);

        int[] cipherSuites = getCipherSuites();
        for (int i = 0; i < cipherSuites.length; ++i) {
            int cipherSuite = cipherSuites[i];

            if (Arrays.contains(this.offeredCipherSuites, cipherSuite)
                    && (eccCipherSuitesEnabled || !TlsECCUtils.isECCCipherSuite(cipherSuite))
                    && org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.TlsUtils.isValidCipherSuiteForVersion(cipherSuite, serverVersion)) {
                return this.selectedCipherSuite = cipherSuite;
            }
        }
        throw new TlsFatalAlert(AlertDescription.handshake_failure);
    }

    public CertificateRequest getCertificateRequest ( ) {
        Vector<SignatureAndHashAlgorithm> serverSigAlgs = null;
        if (org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.TlsUtils.isSignatureAlgorithmsExtensionAllowed(serverVersion)) {
            short[] hashAlgorithms = new short[]{HashAlgorithm.sha512, HashAlgorithm.sha384, HashAlgorithm.sha256, HashAlgorithm.sha224, HashAlgorithm.sha1};
            short[] signatureAlgorithms = new short[]{algorithmCertificate.getSignatureAlgorithm(), SignatureAlgorithm.ecdsa};

            serverSigAlgs = new Vector<>();
            for (int i = 0; i < hashAlgorithms.length; ++i) {
                for (int j = 0; j < signatureAlgorithms.length; ++j) {
                    serverSigAlgs.addElement(new SignatureAndHashAlgorithm(hashAlgorithms[i], signatureAlgorithms[j]));
                }
            }
        }
        return new CertificateRequest(new short[]{algorithmCertificate.getClientCertificate()}, serverSigAlgs, null);
    }

    public void notifyClientCertificate (TlsCertificate clientTlsCertificate) throws IOException {
        Certificate[] chain = clientTlsCertificate.getCertificateList();
        for (int i = 0; i != chain.length; i++) {
            Certificate entry = chain[i];
        }
    }

    protected ProtocolVersion getMaximumVersion ( ) {
        return maxVersion;
    }

    protected ProtocolVersion getMinimumVersion ( ) {
        return minVersion;
    }

    @Override
    protected TlsSignerCredentials getECDSASignerCredentials ( ) throws IOException {
        logger.info("DTLS: TlsSignerCredentials");
        return TlsUtils.loadSignerCredentials(context, certificateResources, keyResource, new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.ecdsa));
    }

    @Override
    protected TlsEncryptionCredentials getRSAEncryptionCredentials ( ) throws IOException {
        logger.info("DTLS: getRSAEncryptionCredentials");
        return TlsUtils.loadEncryptionCredentials(context, certificateResources, keyResource);
    }

    @SuppressWarnings("unchecked")
    protected TlsSignerCredentials getRSASignerCredentials ( ) throws IOException {
        /*
         * TODO Note that this code fails to provide default value for the client supported
         * algorithms if it wasn't sent.
         */
        logger.info("DTLS: getRSASignerCredentials");
        SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
        Vector<SignatureAndHashAlgorithm> sigAlgs = supportedSignatureAlgorithms;
        if (sigAlgs != null) {
            for (int i = 0; i < sigAlgs.size(); ++i) {
                SignatureAndHashAlgorithm sigAlg = sigAlgs.elementAt(i);
                if (sigAlg.getSignature() == SignatureAlgorithm.rsa) {
                    signatureAndHashAlgorithm = sigAlg;
                    break;
                }
            }

            if (signatureAndHashAlgorithm == null) {
                return null;
            }
        }
        return TlsUtils.loadSignerCredentials(context, certificateResources, keyResource, signatureAndHashAlgorithm);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Hashtable<Integer, byte[]> getServerExtensions ( ) throws IOException {
        logger.info("DTLS: getServerExtensions");
        Hashtable<Integer, byte[]> serverExtensions = (Hashtable<Integer, byte[]>) super.getServerExtensions();
        if (TlsSRTPUtils.getUseSRTPExtension(serverExtensions) == null) {
            if (serverExtensions == null) {
                serverExtensions = new Hashtable<>();
            }
            TlsSRTPUtils.addUseSRTPExtension(serverExtensions, serverSrtpData);
        }
        return serverExtensions;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void processClientExtensions (Hashtable newClientExtensions) throws IOException {
        logger.info("DTLS: processClientExtensions");
        super.processClientExtensions(newClientExtensions);

        // set to some reasonable default value
        int chosenProfile = SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80;
        UseSRTPData clientSrtpData = TlsSRTPUtils.getUseSRTPExtension(newClientExtensions);

        for (int profile : clientSrtpData.getProtectionProfiles()) {
            switch (profile) {
                case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32:
                case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80:
                case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32:
                case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80:
                    chosenProfile = profile;
                    break;
                default:
            }
        }

        // server chooses a mutually supported SRTP protection profile
        // http://tools.ietf.org/html/draft-ietf-avt-dtls-srtp-07#section-4.1.2
        int[] protectionProfiles = {chosenProfile};

        // server agrees to use the MKI offered by the client
        serverSrtpData = new UseSRTPData(protectionProfiles, clientSrtpData.getMki());
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

    /**
     * @return the shared secret key that will be used for the SRTP session
     */
    public void prepareSrtpSharedSecret ( ) {
        logger.debug("DtlsSrtpServer: Preparing SRTP Shared Secret...");
        SRTPParameters srtpParams = SRTPParameters.getSrtpParametersForProfile(serverSrtpData.getProtectionProfiles()[0]);
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
        System.arraycopy(sharedSecret, 0, srtpMasterClientKey, 0, keyLen);
        System.arraycopy(sharedSecret, keyLen, srtpMasterServerKey, 0, keyLen);
        System.arraycopy(sharedSecret, 2 * keyLen, srtpMasterClientSalt, 0, saltLen);
        System.arraycopy(sharedSecret, (2 * keyLen + saltLen), srtpMasterServerSalt, 0, saltLen);

        logger.debug("DtlsSrtpServer: SRTP Policy [authType={}] [encType={}]", srtpPolicy.getAuthType(), srtpPolicy.getEncType());

        logger.debug("DtlsSrtpServer: Done.");
    }

    public SRTPPolicy getSrtpPolicy ( ) {
        return srtpPolicy;
    }

    public SRTPPolicy getSrtcpPolicy ( ) {
        return srtcpPolicy;
    }

    public byte[] getSrtpMasterServerKey ( ) {
        return srtpMasterServerKey;
    }

    public byte[] getSrtpMasterServerSalt ( ) {
        return srtpMasterServerSalt;
    }

    public byte[] getSrtpMasterClientKey ( ) {
        return srtpMasterClientKey;
    }

    public byte[] getSrtpMasterClientSalt ( ) {
        return srtpMasterClientSalt;
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
            TlsCertificate chain = TlsUtils.loadCertificateChain(certificateResources);
            Certificate certificate = chain.getCertificateAt(0);
            return TlsUtils.fingerprint(this.hashFunction, certificate);
        } catch (IOException e) {
            logger.error("Could not get local fingerprint: " + e.getMessage());
            return "";
        }
    }

    @Override
    public int[] getCipherSuites ( ) {
        int[] cipherSuites = new int[this.cipherSuites.length];
        for (int i = 0; i < this.cipherSuites.length; i++) {
            cipherSuites[i] = this.cipherSuites[i].getValue();
        }
        return cipherSuites;
    }

}
