package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.generators;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CryptoServicesRegistrar;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.KeyGenerationParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECDomainParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.*;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ECKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator, ECConstants
{
    ECDomainParameters  params;
    SecureRandom        random;

    public void init(
        KeyGenerationParameters param)
    {
        ECKeyGenerationParameters  ecP = (ECKeyGenerationParameters)param;

        this.random = ecP.getRandom();
        this.params = ecP.getDomainParameters();

        if (this.random == null)
        {
            this.random = CryptoServicesRegistrar.getSecureRandom();
        }
    }

    /**
     * Given the domain parameters this routine generates an EC key
     * pair in accordance with X9.62 section 5.2.1 pages 26, 27.
     */
    public AsymmetricCipherKeyPair generateKeyPair()
    {
        BigInteger n = params.getN();
        int nBitLength = n.bitLength();
        int minWeight = nBitLength >>> 2;

        BigInteger d;
        for (;;)
        {
            d = BigIntegers.createRandomBigInteger(nBitLength, random);

            if (d.compareTo(TWO) < 0  || (d.compareTo(n) >= 0))
            {
                continue;
            }

            if (WNafUtil.getNafWeight(d) < minWeight)
            {
                continue;
            }

            break;
        }

        ECPoint Q = createBasePointMultiplier().multiply(params.getG(), d);

        return new AsymmetricCipherKeyPair(
            new ECPublicKeyParameters(Q, params),
            new ECPrivateKeyParameters(d, params));
    }

    protected ECMultiplier createBasePointMultiplier()
    {
        return new FixedPointCombMultiplier();
    }
}
