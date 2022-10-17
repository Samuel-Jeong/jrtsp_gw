package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Digest;

public interface TlsHandshakeHash
    extends Digest
{
    void init(TlsContext context);

    TlsHandshakeHash notifyPRFDetermined();

    void trackHashAlgorithm(short hashAlgorithm);

    void sealHashAlgorithms();

    TlsHandshakeHash stopTracking();

    Digest forkPRFHash();

    byte[] getFinalHash(short hashAlgorithm);
}
