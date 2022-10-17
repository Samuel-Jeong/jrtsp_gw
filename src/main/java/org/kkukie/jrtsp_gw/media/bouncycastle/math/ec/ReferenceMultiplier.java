package org.kkukie.jrtsp_gw.media.bouncycastle.math.ec;

import java.math.BigInteger;

public class ReferenceMultiplier extends AbstractECMultiplier
{
    protected ECPoint multiplyPositive(ECPoint p, BigInteger k)
    {
        return ECAlgorithms.referenceMultiply(p, k);
    }
}
