package org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.endo;

import java.math.BigInteger;

public interface GLVEndomorphism extends ECEndomorphism
{
    BigInteger[] decomposeScalar(BigInteger k);
}
