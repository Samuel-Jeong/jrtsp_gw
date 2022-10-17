package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.generators;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPublicKeyParameters;

public class DSTU4145KeyPairGenerator
    extends ECKeyPairGenerator
{
    public AsymmetricCipherKeyPair generateKeyPair()
    {
        AsymmetricCipherKeyPair pair = super.generateKeyPair();

        ECPublicKeyParameters pub = (ECPublicKeyParameters)pair.getPublic();
        ECPrivateKeyParameters priv = (ECPrivateKeyParameters)pair.getPrivate();

        pub = new ECPublicKeyParameters(pub.getQ().negate(), pub.getParameters());

        return new AsymmetricCipherKeyPair(pub, priv);
    }
}
