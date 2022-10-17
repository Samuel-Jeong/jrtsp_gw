package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.RSAKeyParameters;

import java.io.IOException;

public class DefaultTlsEncryptionCredentials extends AbstractTlsEncryptionCredentials
{
    protected TlsContext context;
    protected TlsCertificate tlsCertificate;
    protected AsymmetricKeyParameter privateKey;

    public DefaultTlsEncryptionCredentials(TlsContext context, TlsCertificate tlsCertificate,
        AsymmetricKeyParameter privateKey)
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

        if (privateKey instanceof RSAKeyParameters)
        {
        }
        else
        {
            throw new IllegalArgumentException("'privateKey' type not supported: "
                + privateKey.getClass().getName());
        }

        this.context = context;
        this.tlsCertificate = tlsCertificate;
        this.privateKey = privateKey;
    }

    public TlsCertificate getCertificate()
    {
        return tlsCertificate;
    }

    public byte[] decryptPreMasterSecret(byte[] encryptedPreMasterSecret)
        throws IOException
    {
        return TlsRSAUtils.safeDecryptPreMasterSecret(context, (RSAKeyParameters)privateKey, encryptedPreMasterSecret);
    }
}
