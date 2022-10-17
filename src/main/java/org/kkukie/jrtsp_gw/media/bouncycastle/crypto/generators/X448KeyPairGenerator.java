package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.generators;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.KeyGenerationParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.X448PrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.X448PublicKeyParameters;

import java.security.SecureRandom;

public class X448KeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private SecureRandom random;

    public void init(KeyGenerationParameters parameters)
    {
        this.random = parameters.getRandom();
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        X448PrivateKeyParameters privateKey = new X448PrivateKeyParameters(random);
        X448PublicKeyParameters publicKey = privateKey.generatePublicKey();
        return new AsymmetricCipherKeyPair(publicKey, privateKey);
    }
}
