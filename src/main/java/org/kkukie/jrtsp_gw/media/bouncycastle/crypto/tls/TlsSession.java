package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

public interface TlsSession
{
    SessionParameters exportSessionParameters();

    byte[] getSessionID();

    void invalidate();

    boolean isResumable();
}
