package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.tls;

import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.ASN1Encoding;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.ASN1Primitive;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.x509.Certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

/**
 * Parsing and encoding of a <i>TlsCertificate</i> struct from RFC 4346.
 * <pre>
 * opaque ASN.1Cert&lt;2^24-1&gt;;
 *
 * struct {
 *     ASN.1Cert certificate_list&lt;0..2^24-1&gt;;
 * } TlsCertificate;
 * </pre>
 *
 * @see Certificate
 */
public class TlsCertificate {

    public static final TlsCertificate EMPTY_CHAIN = new TlsCertificate(new Certificate[0]);

    protected Certificate[] certificateList;

    public TlsCertificate(Certificate[] certificateList) {
        if (certificateList == null)
        {
            throw new IllegalArgumentException("'certificateList' cannot be null");
        }

        this.certificateList = certificateList;
    }

    /**
     * @return an array of {@link Certificate} representing a tlsCertificate
     *         chain.
     */
    public Certificate[] getCertificateList() {
        return cloneCertificateList();
    }

    public Certificate getCertificateAt(int index) {
        return certificateList[index];
    }

    public int getLength()
    {
        return certificateList.length;
    }

    /**
     * @return <code>true</code> if this tlsCertificate chain contains no certificates, or
     *         <code>false</code> otherwise.
     */
    public boolean isEmpty()
    {
        return certificateList.length == 0;
    }

    /**
     * Encode this {@link TlsCertificate} to an {@link OutputStream}.
     *
     * @param output the {@link OutputStream} to encode to.
     * @throws IOException
     */
    public void encode(OutputStream output) throws IOException {
        Vector derEncodings = new Vector(this.certificateList.length);

        int totalLength = 0;
        for (int i = 0; i < this.certificateList.length; ++i)
        {
            byte[] derEncoding = certificateList[i].getEncoded(ASN1Encoding.DER);
            derEncodings.addElement(derEncoding);
            totalLength += derEncoding.length + 3;
        }

        TlsUtils.checkUint24(totalLength);
        TlsUtils.writeUint24(totalLength, output);

        for (int i = 0; i < derEncodings.size(); ++i)
        {
            byte[] derEncoding = (byte[])derEncodings.elementAt(i);
            TlsUtils.writeOpaque24(derEncoding, output);
        }
    }

    /**
     * Parse a {@link TlsCertificate} from an {@link InputStream}.
     *
     * @param input the {@link InputStream} to parse from.
     * @return a {@link TlsCertificate} object.
     * @throws IOException
     */
    public static TlsCertificate parse(InputStream input) throws IOException {
        int totalLength = TlsUtils.readUint24(input);
        if (totalLength == 0)
        {
            return EMPTY_CHAIN;
        }

        byte[] certListData = TlsUtils.readFully(totalLength, input);

        ByteArrayInputStream buf = new ByteArrayInputStream(certListData);

        Vector certificate_list = new Vector();
        while (buf.available() > 0)
        {
            byte[] berEncoding = TlsUtils.readOpaque24(buf);
            ASN1Primitive asn1Cert = TlsUtils.readASN1Object(berEncoding);
            certificate_list.addElement(Certificate.getInstance(asn1Cert));
        }

        Certificate[] certificateList = new Certificate[certificate_list.size()];
        for (int i = 0; i < certificate_list.size(); i++)
        {
            certificateList[i] = (Certificate)certificate_list.elementAt(i);
        }
        return new TlsCertificate(certificateList);
    }

    protected Certificate[] cloneCertificateList() {
        Certificate[] result = new Certificate[certificateList.length];
        System.arraycopy(certificateList, 0, result, 0, result.length);
        return result;
    }
}
