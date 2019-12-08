package net.highwayfrogs.editor.file.sound;

import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Utilities for the VAG file format.
 * TODO: https://github.com/simias/psxsdk/blob/master/tools/vag2wav.c
 * Created by Kneesnap on 11/30/2019.
 */
public class VAGUtil {
    private static final String VAG_SIGNATURE = "VAGp";
    private static final String RIFF_SIGNATURE = "RIFF";
    private static final String WAV_SIGNATURE = "WAVE";
    private static final String DATA_CHUNK_SIGNATURE = "data";
    private static final double[][] TABLE = new double[][]{{0.0, 0.0},
            {60D / 64D, 0D},
            {115D / 64D, -52D / 64D},
            {98D / 64D, -55D / 64D},
            {122D / 64D, -60D / 64D}};

    /**
     * Converts a playstation VAG sound to .WAV
     * @param vagFile The vag file to convert.
     * @return wavBytes
     */
    public static byte[] vagToWav(byte[] vagFile) {
        double[] samples = new double[28];
        double s1 = 0;
        double s2 = 0;

        DataReader reader = new DataReader(new ArraySource(vagFile));
        reader.verifyString(VAG_SIGNATURE);
        int fileVersion = reader.readInt();
        reader.skipInt();
        int dataSize = reader.readInt();
        int sampleRate = reader.readInt();
        reader.setIndex(32);
        String vagName = reader.readString(16);
        reader.setIndex(64);

        ArrayReceiver dataReceiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(dataReceiver);

        // Write wav.
        writer.writeStringBytes(RIFF_SIGNATURE);
        int fileSizeAddress = writer.writeNullPointer();
        writer.writeStringBytes(WAV_SIGNATURE);
        writer.writeInt(16); // Write chunk 1 size.
        writer.writeShort((short) 1); // Writes audio format. 1 = PCM.
        writer.writeInt(1); // Number of channels.
        writer.writeInt(sampleRate);
        writer.writeInt(sampleRate * 2); // (SampleRate * NumChannels * BitsPerSample/8). That would be 44100*1*(16/8), thus 88200.
        writer.writeShort((short) 2); // Block align: (NumChannels * BitsPerSample/8), thus 2
        writer.writeShort((short) 16); // 16 bits per sample.

        writer.writeStringBytes(DATA_CHUNK_SIGNATURE);
        int subChunk2SizeAddress = writer.writeNullPointer();

        while (reader.getIndex() < dataSize + 48) {
            byte predictNr = reader.readByte();
            int shiftFactor = (predictNr & 0b1111);
            predictNr >>= 4;
            byte flags = reader.readByte();

            if (flags == 7)
                break; // End.

            for (int i = 0; i < 28; i += 2) {
                byte d = reader.readByte();
                int s = (d & 0xF) << 12;
                if ((s & 0x8000) == 0x8000)
                    s |= 0xFFFF0000;
                samples[i] = (double) (s >> shiftFactor);
                s = (d & 0xF0) << 8;
                if ((s & 0x8000) == 0x8000)
                    s |= 0xFFFF0000;
                samples[i + 1] = (double) (s >> shiftFactor);
            }

            for (int i = 0; i < 28; i++) {
                samples[i] += (s1 * TABLE[predictNr][0]) + (s2 * TABLE[predictNr][1]);
                s2 = s1;
                s1 = samples[i];
                int d = (int) (samples[i] + 1);
                writer.writeByte((byte) (d & 0xFF)); //TODO: This ok?
                writer.writeByte((byte) ((d >> 8) & 0xFF));
            }
        }

        // Write sizes.
        writer.writeAddressAt(fileSizeAddress, writer.getIndex() - 8); // Write file size.
        writer.writeAddressAt(subChunk2SizeAddress, writer.getIndex() - 44); // Write chunk size.

        writer.closeReceiver();
        return dataReceiver.toArray();
    }
}
