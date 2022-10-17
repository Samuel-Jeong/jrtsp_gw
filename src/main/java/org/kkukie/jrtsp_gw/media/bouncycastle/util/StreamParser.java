package org.kkukie.jrtsp_gw.media.bouncycastle.util;

import java.util.Collection;

public interface StreamParser
{
    Object read() throws StreamParsingException;

    Collection readAll() throws StreamParsingException;
}
