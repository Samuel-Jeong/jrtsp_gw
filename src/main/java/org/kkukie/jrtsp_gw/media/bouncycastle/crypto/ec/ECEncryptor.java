package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.ec;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.ECPoint;

public interface ECEncryptor
{
    void init(CipherParameters params);

    ECPair encrypt(ECPoint point);
}
