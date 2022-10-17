package org.kkukie.jrtsp_gw.media.bouncycastle.math.ec;

public interface ECLookupTable
{
    int getSize();
    ECPoint lookup(int index);
}
