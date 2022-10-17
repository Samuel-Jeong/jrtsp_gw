package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.ec;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;

public interface ECPairTransform
{
    void init(CipherParameters params);

    ECPair transform(ECPair cipherText);
}
