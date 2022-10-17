package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.macs;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.BlockCipher;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Mac;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.paddings.BlockCipherPadding;

public class CFBBlockCipherMac
    implements Mac
{
    private byte[]              mac;

    private byte[]              buf;
    private int                 bufOff;
    private MacCFBBlockCipher   cipher;
    private BlockCipherPadding  padding = null;


    private int                 macSize;

    /**
     * create a standard MAC based on a CFB block cipher. This will produce an
     * authentication code half the length of the block size of the cipher, with
     * the CFB mode set to 8 bits.
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     */
    public CFBBlockCipherMac(
        BlockCipher     cipher)
    {
        this(cipher, 8, (cipher.getBlockSize() * 8) / 2, null);
    }

    /**
     * create a standard MAC based on a CFB block cipher. This will produce an
     * authentication code half the length of the block size of the cipher, with
     * the CFB mode set to 8 bits.
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param padding the padding to be used.
     */
    public CFBBlockCipherMac(
        BlockCipher         cipher,
        BlockCipherPadding  padding)
    {
        this(cipher, 8, (cipher.getBlockSize() * 8) / 2, padding);
    }

    /**
     * create a standard MAC based on a block cipher with the size of the
     * MAC been given in bits. This class uses CFB mode as the basis for the
     * MAC generation.
     * <p>
     * Note: the size of the MAC must be at least 24 bits (FIPS Publication 81),
     * or 16 bits if being used as a data authenticator (FIPS Publication 113),
     * and in general should be less than the size of the block cipher as it reduces
     * the chance of an exhaustive attack (see Handbook of Applied Cryptography).
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param cfbBitSize the size of an output block produced by the CFB mode.
     * @param macSizeInBits the size of the MAC in bits, must be a multiple of 8.
     */
    public CFBBlockCipherMac(
        BlockCipher         cipher,
        int                 cfbBitSize,
        int                 macSizeInBits)
    {
        this(cipher, cfbBitSize, macSizeInBits, null);
    }

    /**
     * create a standard MAC based on a block cipher with the size of the
     * MAC been given in bits. This class uses CFB mode as the basis for the
     * MAC generation.
     * <p>
     * Note: the size of the MAC must be at least 24 bits (FIPS Publication 81),
     * or 16 bits if being used as a data authenticator (FIPS Publication 113),
     * and in general should be less than the size of the block cipher as it reduces
     * the chance of an exhaustive attack (see Handbook of Applied Cryptography).
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param cfbBitSize the size of an output block produced by the CFB mode.
     * @param macSizeInBits the size of the MAC in bits, must be a multiple of 8.
     * @param padding a padding to be used.
     */
    public CFBBlockCipherMac(
        BlockCipher         cipher,
        int                 cfbBitSize,
        int                 macSizeInBits,
        BlockCipherPadding  padding)
    {
        if ((macSizeInBits % 8) != 0)
        {
            throw new IllegalArgumentException("MAC size must be multiple of 8");
        }

        mac = new byte[cipher.getBlockSize()];

        this.cipher = new MacCFBBlockCipher(cipher, cfbBitSize);
        this.padding = padding;
        this.macSize = macSizeInBits / 8;

        buf = new byte[this.cipher.getBlockSize()];
        bufOff = 0;
    }

    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    public void init(
        CipherParameters    params)
    {
        reset();

        cipher.init(params);
    }

    public int getMacSize()
    {
        return macSize;
    }

    public void update(
        byte        in)
    {
        if (bufOff == buf.length)
        {
            cipher.processBlock(buf, 0, mac, 0);
            bufOff = 0;
        }

        buf[bufOff++] = in;
    }

    public void update(
        byte[]      in,
        int         inOff,
        int         len)
    {
        if (len < 0)
        {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }

        int blockSize = cipher.getBlockSize();
        int resultLen = 0;
        int gapLen = blockSize - bufOff;

        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);

            resultLen += cipher.processBlock(buf, 0, mac, 0);

            bufOff = 0;
            len -= gapLen;
            inOff += gapLen;

            while (len > blockSize)
            {
                resultLen += cipher.processBlock(in, inOff, mac, 0);

                len -= blockSize;
                inOff += blockSize;
            }
        }

        System.arraycopy(in, inOff, buf, bufOff, len);

        bufOff += len;
    }

    public int doFinal(
        byte[]  out,
        int     outOff)
    {
        int blockSize = cipher.getBlockSize();

        //
        // pad with zeroes
        //
        if (this.padding == null)
        {
            while (bufOff < blockSize)
            {
                buf[bufOff] = 0;
                bufOff++;
            }
        }
        else
        {
            padding.addPadding(buf, bufOff);
        }

        cipher.processBlock(buf, 0, mac, 0);

        cipher.getMacBlock(mac);

        System.arraycopy(mac, 0, out, outOff, macSize);

        reset();

        return macSize;
    }

    /**
     * Reset the mac generator.
     */
    public void reset()
    {
        /*
         * clean the buffer.
         */
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = 0;
        }

        bufOff = 0;

        /*
         * reset the underlying cipher.
         */
        cipher.reset();
    }
}
