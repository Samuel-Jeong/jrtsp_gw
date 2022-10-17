package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.generators;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.EphemeralKeyPair;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.KeyEncoder;

public class EphemeralKeyPairGenerator
{
    private AsymmetricCipherKeyPairGenerator gen;
    private KeyEncoder keyEncoder;

    public EphemeralKeyPairGenerator(AsymmetricCipherKeyPairGenerator gen, KeyEncoder keyEncoder)
    {
        this.gen = gen;
        this.keyEncoder = keyEncoder;
    }

    public EphemeralKeyPair generate()
    {
        AsymmetricCipherKeyPair eph = gen.generateKeyPair();

        // Encode the ephemeral public key
         return new EphemeralKeyPair(eph, keyEncoder);
    }
}
