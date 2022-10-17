package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.SRP6GroupParameters;

public interface TlsSRPGroupVerifier
{
    /**
     * Check whether the given SRP group parameters are acceptable for use.
     * 
     * @param group the {@link SRP6GroupParameters} to check
     * @return true if (and only if) the specified group parameters are acceptable
     */
    boolean accept(SRP6GroupParameters group);
}
