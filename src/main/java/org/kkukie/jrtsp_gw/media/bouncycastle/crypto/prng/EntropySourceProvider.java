package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.prng;

public interface EntropySourceProvider
{
    EntropySource get(final int bitsRequired);
}
