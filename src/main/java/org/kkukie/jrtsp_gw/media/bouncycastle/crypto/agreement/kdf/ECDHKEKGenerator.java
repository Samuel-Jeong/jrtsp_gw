package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.agreement.kdf;

import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.*;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.DataLengthException;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.DerivationParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Digest;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.DigestDerivationFunction;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.KDFParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.Pack;

import java.io.IOException;

/**
 * X9.63 based key derivation function for ECDH CMS.
 */
public class ECDHKEKGenerator
    implements DigestDerivationFunction
{
    private DigestDerivationFunction kdf;

    private ASN1ObjectIdentifier algorithm;
    private int                 keySize;
    private byte[]              z;

    public ECDHKEKGenerator(
        Digest digest)
    {
        this.kdf = new KDF2BytesGenerator(digest);
    }

    public void init(DerivationParameters param)
    {
        DHKDFParameters params = (DHKDFParameters)param;

        this.algorithm = params.getAlgorithm();
        this.keySize = params.getKeySize();
        this.z = params.getZ();
    }

    public Digest getDigest()
    {
        return kdf.getDigest();
    }

    public int generateBytes(byte[] out, int outOff, int len)
        throws DataLengthException, IllegalArgumentException
    {
        if (outOff + len > out.length)
        {
            throw new DataLengthException("output buffer too small");
        }

        // TODO Create an ASN.1 class for this (RFC3278)
        // ECC-CMS-SharedInfo
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new AlgorithmIdentifier(algorithm, DERNull.INSTANCE));
        v.add(new DERTaggedObject(true, 2, new DEROctetString(Pack.intToBigEndian(keySize))));

        try
        {
            kdf.init(new KDFParameters(z, new DERSequence(v).getEncoded(ASN1Encoding.DER)));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("unable to initialise kdf: " + e.getMessage());
        }

        return kdf.generateBytes(out, outOff, len);
    }
}
