package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.prng;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.prng.drbg.SP80090DRBG;

interface DRBGProvider
{
    SP80090DRBG get(EntropySource entropySource);
}
