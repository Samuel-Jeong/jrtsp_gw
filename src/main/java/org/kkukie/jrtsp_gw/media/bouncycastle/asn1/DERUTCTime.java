package org.kkukie.jrtsp_gw.media.bouncycastle.asn1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * DER UTC time object.
 */
public class DERUTCTime
    extends ASN1UTCTime
{
    DERUTCTime(byte[] bytes)
    {
        super(bytes);
    }

    public DERUTCTime(Date time)
    {
        super(time);
    }

    public DERUTCTime(String time)
    {
        super(time);
    }

    private ASN1Primitive toDERObject(byte[] data) throws IOException
    {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        ASN1InputStream asnInputStream = new ASN1InputStream(inStream);
        return asnInputStream.readObject().toDERObject();
    }

}
