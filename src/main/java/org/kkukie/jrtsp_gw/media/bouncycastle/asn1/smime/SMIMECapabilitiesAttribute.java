package org.kkukie.jrtsp_gw.media.bouncycastle.asn1.smime;

import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.DERSequence;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.DERSet;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.cms.Attribute;

public class SMIMECapabilitiesAttribute
    extends Attribute
{
    public SMIMECapabilitiesAttribute(
        SMIMECapabilityVector capabilities)
    {
        super(SMIMEAttributes.smimeCapabilities,
                new DERSet(new DERSequence(capabilities.toASN1EncodableVector())));
    }
}
