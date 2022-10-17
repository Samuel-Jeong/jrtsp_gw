package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsCipherFactory
{
    /**
     * See enumeration classes EncryptionAlgorithm, MACAlgorithm for appropriate argument values
     */
    TlsCipher createCipher(TlsContext context, int encryptionAlgorithm, int macAlgorithm)
        throws IOException;
}
