package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Signer;

import java.io.ByteArrayOutputStream;

class SignerInputBuffer extends ByteArrayOutputStream
{
    void updateSigner(Signer s)
    {
        s.update(this.buf, 0, count);
    }
}