package org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.custom.sec;

import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.*;
import org.kkukie.jrtsp_gw.media.bouncycastle.math.raw.Nat160;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;

public class SecP160K1Curve extends ECCurve.AbstractFp
{
    public static final BigInteger q = SecP160R2Curve.q;

    private static final int SECP160K1_DEFAULT_COORDS = COORD_JACOBIAN;

    protected SecP160K1Point infinity;

    public SecP160K1Curve()
    {
        super(q);

        this.infinity = new SecP160K1Point(this, null, null);

        this.a = fromBigInteger(ECConstants.ZERO);
        this.b = fromBigInteger(BigInteger.valueOf(7));
        this.order = new BigInteger(1, Hex.decode("0100000000000000000001B8FA16DFAB9ACA16B6B3"));
        this.cofactor = BigInteger.valueOf(1);
        this.coord = SECP160K1_DEFAULT_COORDS;
    }

    protected ECCurve cloneCurve()
    {
        return new SecP160K1Curve();
    }

    public boolean supportsCoordinateSystem(int coord)
    {
        switch (coord)
        {
        case COORD_JACOBIAN:
            return true;
        default:
            return false;
        }
    }

    public BigInteger getQ()
    {
        return q;
    }

    public int getFieldSize()
    {
        return q.bitLength();
    }

    public ECFieldElement fromBigInteger(BigInteger x)
    {
        return new SecP160R2FieldElement(x);
    }

    protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression)
    {
        return new SecP160K1Point(this, x, y, withCompression);
    }

    protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression)
    {
        return new SecP160K1Point(this, x, y, zs, withCompression);
    }

    public ECPoint getInfinity()
    {
        return infinity;
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] points, int off, final int len)
    {
        final int FE_INTS = 5;

        final int[] table = new int[len * FE_INTS * 2];
        {
            int pos = 0;
            for (int i = 0; i < len; ++i)
            {
                ECPoint p = points[off + i];
                Nat160.copy(((SecP160R2FieldElement)p.getRawXCoord()).x, 0, table, pos); pos += FE_INTS;
                Nat160.copy(((SecP160R2FieldElement)p.getRawYCoord()).x, 0, table, pos); pos += FE_INTS;
            }
        }

        return new ECLookupTable()
        {
            public int getSize()
            {
                return len;
            }

            public ECPoint lookup(int index)
            {
                int[] x = Nat160.create(), y = Nat160.create();
                int pos = 0;

                for (int i = 0; i < len; ++i)
                {
                    int MASK = ((i ^ index) - 1) >> 31;

                    for (int j = 0; j < FE_INTS; ++j)
                    {
                        x[j] ^= table[pos + j] & MASK;
                        y[j] ^= table[pos + FE_INTS + j] & MASK;
                    }

                    pos += (FE_INTS * 2);
                }

                return createRawPoint(new SecP160R2FieldElement(x), new SecP160R2FieldElement(y), false);
            }
        };
    }
}
