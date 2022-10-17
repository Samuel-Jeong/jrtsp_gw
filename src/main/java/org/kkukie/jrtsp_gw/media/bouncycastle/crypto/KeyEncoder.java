package org.kkukie.jrtsp_gw.media.bouncycastle.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;

public interface KeyEncoder
{
    byte[] getEncoded(AsymmetricKeyParameter keyParameter);
}
