package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsSignerCredentials
    extends TlsCredentials
{
    byte[] generateCertificateSignature(byte[] hash)
        throws IOException;

    SignatureAndHashAlgorithm getSignatureAndHashAlgorithm();
}
