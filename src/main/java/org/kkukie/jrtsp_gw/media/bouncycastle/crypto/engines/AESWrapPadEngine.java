package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.engines;

public class AESWrapPadEngine
    extends RFC5649WrapEngine
{
    public AESWrapPadEngine()
    {
        super(new AESEngine());
    }
}
