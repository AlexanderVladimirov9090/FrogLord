package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.writer.BitWriter;
import net.highwayfrogs.editor.system.IntList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find any code or documentation on how PP20 compresses data.
 * So, this was created from research and attempts to reverse the unpacker.
 *
 * PP20 is a Lz77 (sliding window compression) variant.
 * It resembles LZSS, but not exactly.
 *
 * Useful Links:
 * - https://en.wikipedia.org/wiki/LZ77_and_LZ78
 * - https://eblong.com/zarf/blorb/mod-spec.txt
 *
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    private static final byte[] COMPRESSION_SETTINGS = {0x07, 0x07, 0x07, 0x07}; // PP20 compression settings.
    private static int[] COMPRESSION_SETTING_MAX_OFFSETS;
    public static final int OPTIONAL_BITS_SMALL_OFFSET = 7;
    public static final int INPUT_BIT_LENGTH = 2;
    public static final int INPUT_CONTINUE_WRITING_BITS = 3;
    public static final int OFFSET_BIT_LENGTH = 3;
    public static final int OFFSET_CONTINUE_WRITING_BITS = 7;
    public static final int READ_FROM_INPUT_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;
    public static final String MARKER = "PP20";

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        data = Utils.reverseCloneByteArray(data);

        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data);
        byte[] completeData = new byte[compressedData.length + 12];
        System.arraycopy(compressedData, 0, completeData, 8, compressedData.length); // Copy compressed data.

        for (int i = 0; i < MARKER.length(); i++)
            completeData[i] = (byte) MARKER.charAt(i);

        System.arraycopy(COMPRESSION_SETTINGS, 0, completeData, 4, COMPRESSION_SETTINGS.length);
        byte[] array = ByteBuffer.allocate(Constants.INTEGER_SIZE).putInt(data.length).array();
        System.arraycopy(array, 1, completeData, completeData.length - 4, array.length - 1);
        return completeData;
    }

    private static int findLongest(byte[] data, int bufferEnd, List<Byte> target, Map<Byte, IntList> dictionary) {
        target.clear();
        byte startByte = data[bufferEnd];
        target.add(startByte);

        IntList possibleResults = dictionary.get(startByte);
        if (possibleResults == null)
            return -1;

        if (COMPRESSION_SETTING_MAX_OFFSETS == null) { // Generate cached offset values. (This is a rather heavy operation, so we cache the stuff.)
            COMPRESSION_SETTING_MAX_OFFSETS = new int[COMPRESSION_SETTINGS.length + MINIMUM_DECODE_DATA_LENGTH];
            for (int i = 0; i < COMPRESSION_SETTING_MAX_OFFSETS.length; i++)
                COMPRESSION_SETTING_MAX_OFFSETS[i] = getMaximumOffset(i);
        }

        int bestIndex = -1;
        int minIndex = 0;

        for (int resultId = possibleResults.size() - 1; resultId >= 0; resultId--) {
            int testIndex = possibleResults.get(resultId);
            int targetSize = target.size();

            if (COMPRESSION_SETTING_MAX_OFFSETS.length > targetSize) // We'd rather cache this variable, as it's rather expensive to calculate.
                minIndex = Math.max(0, bufferEnd - COMPRESSION_SETTING_MAX_OFFSETS[targetSize]);

            if (minIndex > testIndex)
                break; // We've gone too far.

            boolean existingPass = true;
            for (int i = 1; i < targetSize; i++) {
                if (data[i + bufferEnd] != data[i + testIndex]) {
                    existingPass = false;
                    break;
                }
            }

            if (!existingPass)
                continue; // It didn't match existing data read up to here, it can't be the longest.

            // Grow the target for as long as it matches.
            byte temp;
            int j = targetSize;
            while (data.length > bufferEnd + j && (temp = data[j + bufferEnd]) == data[j + testIndex]) { // Break on reaching end of data, or when the data stops matching.
                target.add(temp);
                j++;
            }

            int newSize = target.size();
            if (newSize >= MINIMUM_DECODE_DATA_LENGTH // Verify large enough that it can be compressed.
                    && newSize != targetSize) // Prefer the ones closest to bufferEnd (Lower offsets = Smaller file). (Ie: They're read first, therefore if we found a duplicate example, we want to rely on the one we've already read.
                bestIndex = testIndex;
        }

        return bestIndex;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();
        writer.setReverseBytes(true);

        List<Byte> noMatchQueue = new ArrayList<>();
        List<Byte> searchList = new ArrayList<>();

        Map<Byte, IntList> dictionary = new HashMap<>();
        for (int i = 0; i < data.length; i++) {
            byte temp = data[i];

            int bestIndex = findLongest(data, i, searchList, dictionary);
            int byteOffset = i - bestIndex - 1;

            if (bestIndex >= 0) { // Verify the compression index was found.
                if (!noMatchQueue.isEmpty()) { // When a compressed one has been reached, write all the data in-between, if there is any.
                    writeRawData(writer, Utils.toArray(noMatchQueue));
                    noMatchQueue.clear();
                } else {
                    writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));
                }

                writeDataReference(writer, searchList.size(), byteOffset);

                int recordIndex = i;
                for (byte recordByte : searchList)
                    dictionary.computeIfAbsent(recordByte, k -> new IntList()).add(recordIndex++);

                i += searchList.size() - 1;
            } else { // It's not large enough to be compressed.
                noMatchQueue.add(temp);

                // Add current byte to the search dictionary.
                if (!dictionary.containsKey(temp))
                    dictionary.put(temp, new IntList());
                dictionary.get(temp).add(i);
            }
        }
        if (!noMatchQueue.isEmpty()) // Add whatever remains at the end, if there is any.
            writeRawData(writer, Utils.toArray(noMatchQueue));

        return writer.toByteArray();
    }

    private static int getMaximumOffset(int byteLength) {
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.max(0, Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH));
        int offsetSize = COMPRESSION_SETTINGS[compressionLevel];
        return (int) Math.pow(2, offsetSize);
    }

    private static void writeDataReference(BitWriter writer, int byteLength, int byteOffset) {
        // Calculate compression level.
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH);

        boolean maxCompression = (compressionLevel == maxCompressionIndex);
        boolean useSmallOffset = maxCompression && Math.pow(2, OPTIONAL_BITS_SMALL_OFFSET) > byteOffset;
        int offsetSize = useSmallOffset ? OPTIONAL_BITS_SMALL_OFFSET : COMPRESSION_SETTINGS[compressionLevel];

        int writeLength = byteLength - compressionLevel;
        writer.writeBits(Utils.getBits(compressionLevel, 2));

        if (maxCompression) {
            writer.writeBit(useSmallOffset ? Constants.BIT_FALSE : Constants.BIT_TRUE);
            writeLength -= MINIMUM_DECODE_DATA_LENGTH;
        }

        writer.writeBits(Utils.getBits(byteOffset, offsetSize));

        if (maxCompression) {
            int writtenNum;
            do { // Write the length of the data.
                writtenNum = Math.min(writeLength, PP20Packer.OFFSET_CONTINUE_WRITING_BITS);
                writeLength -= writtenNum;
                writer.writeBits(Utils.getBits(writtenNum, PP20Packer.OFFSET_BIT_LENGTH));
            } while (writeLength > 0);

            if (writtenNum == PP20Packer.OFFSET_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
                writer.writeBits(new int[OFFSET_BIT_LENGTH]);
        }
    }

    private static void writeRawData(BitWriter writer, byte[] data) {
        writer.writeBit(READ_FROM_INPUT_BIT); // Indicates this should readFromInput, not readFromAbove.

        int writeLength = data.length - 1;
        int writtenNum;

        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.INPUT_CONTINUE_WRITING_BITS);
            writeLength -= writtenNum;
            writer.writeBits(Utils.getBits(writtenNum, PP20Packer.INPUT_BIT_LENGTH));
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[PP20Packer.INPUT_BIT_LENGTH]);

        for (byte toWrite : data) // Writes the data.
            writer.writeByte(toWrite);
    }
}