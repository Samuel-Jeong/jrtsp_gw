package org.kkukie.jrtsp_gw.media.bouncycastle.crypto;

public interface RawAgreement
{
    void init(CipherParameters parameters);

    int getAgreementSize();

    void calculateAgreement(CipherParameters publicKey, byte[] buf, int off);
}
