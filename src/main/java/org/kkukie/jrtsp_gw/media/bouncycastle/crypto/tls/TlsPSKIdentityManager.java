package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

public interface TlsPSKIdentityManager
{
    byte[] getHint();

    byte[] getPSK(byte[] identity);
}
