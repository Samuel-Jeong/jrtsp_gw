package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.SRP6GroupParameters;

import java.math.BigInteger;

public class TlsSRPLoginParameters
{
    protected SRP6GroupParameters group;
    protected BigInteger verifier;
    protected byte[] salt;

    public TlsSRPLoginParameters(SRP6GroupParameters group, BigInteger verifier, byte[] salt)
    {
        this.group = group;
        this.verifier = verifier;
        this.salt = salt;
    }

    public SRP6GroupParameters getGroup()
    {
        return group;
    }

    public byte[] getSalt()
    {
        return salt;
    }

    public BigInteger getVerifier()
    {
        return verifier;
    }
}
