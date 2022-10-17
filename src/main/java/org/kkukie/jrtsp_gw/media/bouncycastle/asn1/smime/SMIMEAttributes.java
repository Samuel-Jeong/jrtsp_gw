package org.kkukie.jrtsp_gw.media.bouncycastle.asn1.smime;

import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.kkukie.jrtsp_gw.media.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public interface SMIMEAttributes
{
    ASN1ObjectIdentifier smimeCapabilities = PKCSObjectIdentifiers.pkcs_9_at_smimeCapabilities;
    ASN1ObjectIdentifier encrypKeyPref = PKCSObjectIdentifiers.id_aa_encrypKeyPref;
}
