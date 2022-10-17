package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.KeyGenerationParameters;

import java.security.SecureRandom;

public class Ed448KeyGenerationParameters
    extends KeyGenerationParameters
{
    public Ed448KeyGenerationParameters(SecureRandom random)
    {
        super(random, 448);
    }
}
