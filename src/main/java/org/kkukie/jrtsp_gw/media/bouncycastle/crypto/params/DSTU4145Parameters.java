package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params;

import org.kkukie.jrtsp_gw.media.bouncycastle.util.Arrays;

public class DSTU4145Parameters
    extends ECDomainParameters
{
    private final byte[] dke;

    public DSTU4145Parameters(ECDomainParameters ecParameters, byte[] dke)
    {
        super(ecParameters.getCurve(), ecParameters.getG(), ecParameters.getN(), ecParameters.getH(), ecParameters.getSeed());

        this.dke = Arrays.clone(dke);
    }

    public byte[] getDKE()
    {
        return Arrays.clone(dke);
    }
}
