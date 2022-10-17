package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.agreement;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.RawAgreement;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.X25519PublicKeyParameters;

public final class X25519Agreement
    implements RawAgreement
{
    private X25519PrivateKeyParameters privateKey;

    public void init(CipherParameters parameters)
    {
        this.privateKey = (X25519PrivateKeyParameters)parameters;
    }

    public int getAgreementSize()
    {
        return X25519PrivateKeyParameters.SECRET_SIZE;
    }

    public void calculateAgreement(CipherParameters publicKey, byte[] buf, int off)
    {
        privateKey.generateSecret((X25519PublicKeyParameters)publicKey, buf, off);
    }
}
