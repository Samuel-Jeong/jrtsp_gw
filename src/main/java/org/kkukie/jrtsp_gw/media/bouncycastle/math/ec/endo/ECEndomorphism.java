package org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.endo;

import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.ECPointMap;

public interface ECEndomorphism
{
    ECPointMap getPointMap();

    boolean hasEfficientPointMap();
}
