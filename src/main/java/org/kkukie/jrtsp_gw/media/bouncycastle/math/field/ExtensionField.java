package org.kkukie.jrtsp_gw.media.bouncycastle.math.field;

public interface ExtensionField extends FiniteField
{
    FiniteField getSubfield();

    int getDegree();
}
