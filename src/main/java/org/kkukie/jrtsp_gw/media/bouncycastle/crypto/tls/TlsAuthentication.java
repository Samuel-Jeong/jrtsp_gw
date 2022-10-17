package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import java.io.IOException;

/**
 * Base interface to provide TLS authentication credentials.
 */
public interface TlsAuthentication
{
    /**
     * Called by the protocol handler to report the server tlsCertificate
     * Note: this method is responsible for tlsCertificate verification and validation
     *
     * @param serverTlsCertificate the server tlsCertificate received
     * @throws IOException
     */
    void notifyServerCertificate(TlsCertificate serverTlsCertificate)
        throws IOException;

    /**
     * Return client credentials in response to server's tlsCertificate request
     *
     * @param certificateRequest details of the tlsCertificate request
     * @return a TlsCredentials object or null for no client authentication
     * @throws IOException
     */
    TlsCredentials getClientCredentials(CertificateRequest certificateRequest)
        throws IOException;
}
