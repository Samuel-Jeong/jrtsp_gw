/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.kkukie.jrtsp_gw.media.core.stream.rtp.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.ProtocolVersion;

import static org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls.CipherSuite.*;

/**
 * @author guilherme.jansen@telestax.com
 */
public class DtlsSrtpServerProvider {

    private static final int[] defaultCipherSuites = {
            TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,
            TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
            TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_256_CBC_SHA256, TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
            TLS_DHE_RSA_WITH_AES_256_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_GCM_SHA384, TLS_RSA_WITH_AES_128_GCM_SHA256,
            TLS_RSA_WITH_AES_256_CBC_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA256, TLS_RSA_WITH_AES_256_CBC_SHA, TLS_RSA_WITH_AES_128_CBC_SHA,
            TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256};

    private final ProtocolVersion minVersion;
    private final ProtocolVersion maxVersion;
    private final CipherSuite[] cipherSuites;
    private final String[] certificatePaths;
    private final String keyPath;
    private final AlgorithmCertificate algorithmCertificate;

    public DtlsSrtpServerProvider (String certificatePath, String keyPath) {
        this.minVersion = ProtocolVersion.DTLSv10;
        this.maxVersion = ProtocolVersion.DTLSv12;
        this.cipherSuites = new CipherSuite[defaultCipherSuites.length];
        for (int idx = 0; idx < defaultCipherSuites.length; idx++) {
            cipherSuites[idx] = CipherSuite.getEnum(defaultCipherSuites[idx]);
        }
        this.certificatePaths = new String[]{certificatePath};
        this.keyPath = keyPath;
        this.algorithmCertificate = AlgorithmCertificate.RSA;
    }
    public DtlsSrtpServer provide () {
        return new DtlsSrtpServer(
                minVersion, maxVersion,
                cipherSuites, certificatePaths,
                keyPath, algorithmCertificate
        );
    }

}
