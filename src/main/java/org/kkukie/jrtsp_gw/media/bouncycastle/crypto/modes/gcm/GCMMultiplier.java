package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.modes.gcm;

public interface GCMMultiplier
{
    void init(byte[] H);
    void multiplyH(byte[] x);
}
