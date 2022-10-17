package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CryptoException;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.RSAKeyParameters;

import java.io.IOException;

public class DefaultTlsSignerCredentials
    extends AbstractTlsSignerCredentials
{
    protected TlsContext context;
    protected TlsCertificate tlsCertificate;
    protected AsymmetricKeyParameter privateKey;
    protected SignatureAndHashAlgorithm signatureAndHashAlgorithm;

    protected TlsSigner signer;

    public DefaultTlsSignerCredentials(TlsContext context, TlsCertificate tlsCertificate, AsymmetricKeyParameter privateKey)
    {
        this(context, tlsCertificate, privateKey, null);
    }

    public DefaultTlsSignerCredentials(TlsContext context, TlsCertificate tlsCertificate, AsymmetricKeyParameter privateKey,
                                       SignatureAndHashAlgorithm signatureAndHashAlgorithm)
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
        if (TlsUtils.isTLSv12(context) && signatureAndHashAlgorithm == null)
        {
            throw new IllegalArgumentException("'signatureAndHashAlgorithm' cannot be null for (D)TLS 1.2+");
        }

        if (privateKey instanceof RSAKeyParameters)
        {
            this.signer = new TlsRSASigner();
        }
        else if (privateKey instanceof DSAPrivateKeyParameters)
        {
            this.signer = new TlsDSSSigner();
        }
        else if (privateKey instanceof ECPrivateKeyParameters)
        {
            this.signer = new TlsECDSASigner();
        }
        else
        {
            throw new IllegalArgumentException("'privateKey' type not supported: " + privateKey.getClass().getName());
        }

        this.signer.init(context);

        this.context = context;
        this.tlsCertificate = tlsCertificate;
        this.privateKey = privateKey;
        this.signatureAndHashAlgorithm = signatureAndHashAlgorithm;
    }

    public TlsCertificate getCertificate()
    {
        return tlsCertificate;
    }

    public byte[] generateCertificateSignature(byte[] hash)
        throws IOException
    {
        try
        {
            if (TlsUtils.isTLSv12(context))
            {
                return signer.generateRawSignature(signatureAndHashAlgorithm, privateKey, hash);
            }
            else
            {
                return signer.generateRawSignature(privateKey, hash);
            }
        }
        catch (CryptoException e)
        {
            throw new TlsFatalAlert(AlertDescription.internal_error, e);
        }
    }

    public SignatureAndHashAlgorithm getSignatureAndHashAlgorithm()
    {
        return signatureAndHashAlgorithm;
    }
}
