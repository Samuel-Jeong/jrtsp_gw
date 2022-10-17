package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.BasicAgreement;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.BigIntegers;

import java.math.BigInteger;

public class DefaultTlsAgreementCredentials
    extends AbstractTlsAgreementCredentials
{
    protected TlsCertificate tlsCertificate;
    protected AsymmetricKeyParameter privateKey;

    protected BasicAgreement basicAgreement;
    protected boolean truncateAgreement;

    public DefaultTlsAgreementCredentials(TlsCertificate tlsCertificate, AsymmetricKeyParameter privateKey)
    {
        if (tlsCertificate == null)
        {
            throw new IllegalArgumentException("'tlsCertificate' cannot be null");
        }
        if (tlsCertificate.isEmpty())
        {
            throw new IllegalArgumentException("'tlsCertificate' cannot be empty");
        }
        if (privateKey == null)
        {
            throw new IllegalArgumentException("'privateKey' cannot be null");
        }
        if (!privateKey.isPrivate())
        {
            throw new IllegalArgumentException("'privateKey' must be private");
        }

        if (privateKey instanceof DHPrivateKeyParameters)
        {
            basicAgreement = new DHBasicAgreement();
            truncateAgreement = true;
        }
        else if (privateKey instanceof ECPrivateKeyParameters)
        {
            basicAgreement = new ECDHBasicAgreement();
            truncateAgreement = false;
        }
        else
        {
            throw new IllegalArgumentException("'privateKey' type not supported: "
                + privateKey.getClass().getName());
        }

        this.tlsCertificate = tlsCertificate;
        this.privateKey = privateKey;
    }

    public TlsCertificate getCertificate()
    {
        return tlsCertificate;
    }

    public byte[] generateAgreement(AsymmetricKeyParameter peerPublicKey)
    {
        basicAgreement.init(privateKey);
        BigInteger agreementValue = basicAgreement.calculateAgreement(peerPublicKey);

        if (truncateAgreement)
        {
            return BigIntegers.asUnsignedByteArray(agreementValue);
        }

        return BigIntegers.asUnsignedByteArray(basicAgreement.getFieldSize(), agreementValue);
    }
}
