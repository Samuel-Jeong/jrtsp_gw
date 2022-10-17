package org.kkukie.jrtsp_gw.media.bouncycastle.math.field;

import java.math.BigInteger;

public interface FiniteField
{
    BigInteger getCharacteristic();

    int getDimension();
}
