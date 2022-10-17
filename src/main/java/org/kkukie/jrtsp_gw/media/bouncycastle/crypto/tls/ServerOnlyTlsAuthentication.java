package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

public abstract class ServerOnlyTlsAuthentication
    implements TlsAuthentication
{
    public final TlsCredentials getClientCredentials(CertificateRequest certificateRequest)
    {
        return null;
    }
}
