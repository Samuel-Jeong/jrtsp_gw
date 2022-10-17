package org.kkukie.jrtsp_gw.media.bouncycastle.crypto;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.IOException;
import java.io.InputStream;

public interface KeyParser
{
    AsymmetricKeyParameter readKey(InputStream stream)
        throws IOException;
}
