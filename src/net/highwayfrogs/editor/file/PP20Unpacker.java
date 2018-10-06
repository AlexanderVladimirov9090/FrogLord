package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.writer.BitReader;

/**
 * PP20 Unpacker: Unpacks PowerPacker compressed data.
 * Source:
 * Author: Josef Jelinek
 * URL: https://github.com/josef-jelinek/tiny-mod-player/blob/master/lib.gamod/src/gamod/unpack/PowerPacker.java
 * Copied on August 11, 2018. There is no license attached to the repository, however the author has explicitly granted permission to use this code.
 */
public class PP20Unpacker {
    /**
     * Is a given byte array PP20 compressed data?
     * @param a The bytes to test.
     * @return isCompressed
     */
    public static boolean isCompressed(byte[] a) {
        byte[] headerTest = PP20Packer.MARKER.getBytes();
        for (int i = 0; i < headerTest.length; i++)
            if (headerTest[i] != a[i])
                return false;
        return a.length > 11;
    }

    /**
     * Unpacks PP20 compressed data.
     * @param data The data to unpack.
     * @return unpackedData
     */
    public static byte[] unpackData(byte[] data) {
        Utils.verify(isCompressed(data), "Not PowerPacker (PP20) compressed data!");
        int[] offsetBitLengths = getOffsetBitLengths(data);
        int skip = data[data.length - 1] & 0xFF; // Last byte contains the amount of bits to trash.
        byte[] out = new byte[getDecodedDataSize(data)];
        int outPos = out.length;
        BitReader in = new BitReader(data, data.length - 5);
        in.readBits(skip); // skipped bits

        while (outPos > 0)
            outPos = decodeSegment(in, out, outPos, offsetBitLengths);
        return out;
    }

    private static int[] getOffsetBitLengths(byte[] data) {
        int[] a = new int[4];
        for (int i = 0; i < 4; i++)
            a[i] = data[i + 4];
        return a;
    }

    private static int getDecodedDataSize(byte[] data) {
        int i = data.length - 2;
        return (data[i - 2] & 0xFF) << 16 | (data[i - 1] & 0xFF) << 8 | data[i] & 0xFF;
    }

    private static int decodeSegment(BitReader in, byte[] out, int outPos, int[] offsetBitLengths) {
        if (in.readBit() == PP20Packer.READ_FROM_INPUT_BIT)
            outPos = copyFromInput(in, out, outPos);
        if (outPos > 0)
            outPos = copyFromDecoded(in, out, outPos, offsetBitLengths);
        return outPos;
    }

    // Appears to put it into the table.
    private static int copyFromInput(BitReader reader, byte[] out, int bytePos) {
        int count = 1, countInc;
        while ((countInc = reader.readBits(PP20Packer.INPUT_BIT_LENGTH)) == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Read the string size. If it == 3, that means the length might be longer.
            count += PP20Packer.INPUT_CONTINUE_WRITING_BITS;

        for (count += countInc; count > 0; count--) // Register the string in the table.
            out[--bytePos] = (byte) reader.readBits(Constants.BITS_PER_BYTE);

        return bytePos;
    }

    private static int copyFromDecoded(BitReader in, byte[] out, int bytePos, int[] offsetBitLengths) {
        int maxCompressionLevel = offsetBitLengths.length - 1;

        int run = in.readBits(2); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
        int offBits = run == maxCompressionLevel && in.readBit() == 0 ? PP20Packer.OPTIONAL_BITS_SMALL_OFFSET : offsetBitLengths[run];
        int off = in.readBits(offBits);

        int runInc = 0;
        if (run == maxCompressionLevel) // The length might be extended further.
            while ((runInc = in.readBits(PP20Packer.OFFSET_BIT_LENGTH)) == PP20Packer.OFFSET_CONTINUE_WRITING_BITS) // Keep adding until the three read bits are not '111', meaning the length has stopped.
                run += PP20Packer.OFFSET_CONTINUE_WRITING_BITS;

        run += PP20Packer.MINIMUM_DECODE_DATA_LENGTH;
        run += runInc;

        for (int i = 0; i < run; i++, bytePos--)
            out[bytePos - 1] = out[bytePos + off];

        return bytePos;
    }
}
