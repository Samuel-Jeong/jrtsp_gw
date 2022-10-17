package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.ec;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.ECPoint;

public interface ECDecryptor
{
    void init(CipherParameters params);

    ECPoint decrypt(ECPair cipherText);
}
