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

package org.kkukie.jrtsp_gw.media.core.stream.dtls;

import lombok.NoArgsConstructor;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.x509.Certificate;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.digests.SHA1Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.digests.SHA256Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.digests.SHA512Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.*;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.util.PrivateKeyFactory;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.encoders.Hex;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.io.pem.PemObject;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.io.pem.PemReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Collection of utility functions for DTLS operations.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */

@NoArgsConstructor
public class TlsUtils {

    private static final String SHA_1 = "sha-1";
    private static final String SHA_256 = "sha-256";
    private static final String SHA_512 = "sha-512";

    static String fingerprint (String hashFunction, Certificate tlsCertificate) throws IOException {
        byte[] der = tlsCertificate.getEncoded();
        byte[] sha1 = digestOf(hashFunction, der);
        byte[] hexBytes = Hex.encode(sha1);
        String hex = new String(hexBytes, StandardCharsets.US_ASCII).toUpperCase();

        StringBuilder fp = new StringBuilder();
        int i = 0;
        fp.append(hex, i, i + 2);
        while ((i += 2) < hex.length()) {
            fp.append(':');
            fp.append(hex, i, i + 2);
        }

        switch (hashFunction) {
            case SHA_1:
                return SHA_1 + " " + fp;
            case SHA_512:
                return SHA_512 + " " + fp;
            default:
                return SHA_256 + " " + fp;
        }
    }

    static byte[] digestOf (String hashFunction, byte[] input) {
        Digest d;
        switch (hashFunction) {
            case SHA_1:
                d = new SHA1Digest();
                break;

            case SHA_512:
                d = new SHA512Digest();
                break;

            case SHA_256:
            default:
                d = new SHA256Digest();
                break;
        }

        d.update(input, 0, input.length);
        byte[] result = new byte[d.getDigestSize()];
        d.doFinal(result, 0);
        return result;
    }

    static TlsAgreementCredentials loadAgreementCredentials (TlsContext context, String[] certResources, String keyResource) throws IOException {
        TlsCertificate tlsCertificate = loadCertificateChain(certResources);
        AsymmetricKeyParameter privateKey = loadPrivateKeyResource(keyResource);
        return new DefaultTlsAgreementCredentials(tlsCertificate, privateKey);
    }

    static TlsEncryptionCredentials loadEncryptionCredentials (TlsContext context, String[] certResources, String keyResource) throws IOException {
        TlsCertificate tlsCertificate = loadCertificateChain(certResources);
        AsymmetricKeyParameter privateKey = loadPrivateKeyResource(keyResource);
        return new DefaultTlsEncryptionCredentials(context, tlsCertificate, privateKey);
    }

    static TlsSignerCredentials loadSignerCredentials (TlsContext context, String[] certResources, String keyResource) throws IOException {
        TlsCertificate tlsCertificate = loadCertificateChain(certResources);
        AsymmetricKeyParameter privateKey = loadPrivateKeyResource(keyResource);
        return new DefaultTlsSignerCredentials(context, tlsCertificate, privateKey);
    }

    static TlsSignerCredentials loadSignerCredentials (TlsServerContext context, String[] certResources, String keyResource, SignatureAndHashAlgorithm signatureAndHashAlgorithm) throws IOException {
        TlsCertificate tlsCertificate = loadCertificateChain(certResources);
        AsymmetricKeyParameter privateKey = loadPrivateKeyResource(keyResource);
        return new DefaultTlsSignerCredentials(context, tlsCertificate, privateKey, signatureAndHashAlgorithm);
    }

    static TlsCertificate loadCertificateChain (String[] resources) throws IOException {
        Certificate[] chain = new Certificate[resources.length];
        for (int i = 0; i < resources.length; ++i) {
            chain[i] = loadCertificateResource(resources[i]);
        }
        return new TlsCertificate(chain);
    }

    static Certificate loadCertificateResource (String resource) throws IOException {
        PemObject pem = loadPemResource(resource);
        if (pem.getType().endsWith("CERTIFICATE")) {
            return Certificate.getInstance(pem.getContent());
        }
        throw new IllegalArgumentException("'resource' doesn't specify a valid tlsCertificate");
    }

    static AsymmetricKeyParameter loadPrivateKeyResource (String resource) throws IOException {
        PemObject pem = loadPemResource(resource);
        if (pem.getType().endsWith("RSA PRIVATE KEY")) {
            RSAPrivateKey rsa = RSAPrivateKey.getInstance(pem.getContent());
            return new RSAPrivateCrtKeyParameters(rsa.getModulus(),
                    rsa.getPublicExponent(), rsa.getPrivateExponent(),
                    rsa.getPrime1(), rsa.getPrime2(), rsa.getExponent1(),
                    rsa.getExponent2(), rsa.getCoefficient());
        }
        if (pem.getType().endsWith("PRIVATE KEY")) {
            return PrivateKeyFactory.createKey(pem.getContent());
        }

        throw new IllegalArgumentException("'resource' doesn't specify a valid private key");
    }

    static PemObject loadPemResource (String resource) throws IOException {
        InputStream s = new FileInputStream(resource);
        try (PemReader p = new PemReader(new InputStreamReader(s))) {
            return p.readPemObject();
        }
    }

}
