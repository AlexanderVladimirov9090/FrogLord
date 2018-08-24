package net.highwayfrogs.editor.file;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.VHFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses VB files and allows exporting to WAV, and importing audio files.
 * TODO: Add support for importing audio files.
 * TODO: Test exporting audio files.
 * TODO: Move into a sound folder.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VBFile extends GameObject {
    private VHFile header;
    private List<AudioEntry> audioEntries = new ArrayList<>();

    public VBFile(VHFile header) {
        this.header = header;
    }

    @Override
    public void load(DataReader reader) {
        for (FileEntry vhEntry : header.getEntries()) {
            AudioEntry audioEntry = new AudioEntry(vhEntry);

            reader.jumpTemp(vhEntry.getDataStartOffset());
            int readLength = vhEntry.getDataSize() / audioEntry.getBitWidth();
            for (int i = 0; i < readLength; i++)
                audioEntry.getAudioData().add(reader.readInt(audioEntry.getByteWidth()));
            reader.jumpReturn();

            this.audioEntries.add(audioEntry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (AudioEntry entry : getAudioEntries())
            for (int toWrite : entry.getAudioData())
                writer.writeNumber(toWrite, entry.getByteWidth());
    }

    @Getter
    private static class AudioEntry {
        private FileEntry vhEntry;
        private List<Integer> audioData = new ArrayList<>();

        public AudioEntry(FileEntry vhEntry) {
            this.vhEntry = vhEntry;
        }

        /**
         * Export this audio entry as a standard audio clip.
         * @return audioClip
         */
        public Clip toStandardAudio() throws LineUnavailableException {
            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            for (int i = 0; i < getAudioData().size(); i++)
                writer.writeNumber(getAudioData().get(i), getByteWidth());
            byte[] byteData = receiver.toArray();

            Clip result = AudioSystem.getClip();
            result.open(new AudioFormat(getSampleRate(), getBitWidth(), getChannelCount(), true, false),
                    byteData, 0, byteData.length);

            return result;
        }

        /**
         * Get the number of channels for this entry.
         * @return channelCount
         */
        public int getChannelCount() {
            return vhEntry.getChannels();
        }

        /**
         * Set the number of channels for this entry.
         * @param channelCount The new channel amount.
         */
        public void setChannelCount(int channelCount) {
            vhEntry.setChannels(channelCount);
        }

        /**
         * Gets the sample rate of this audio entry.
         * @return sampleRate
         */
        public int getSampleRate() {
            return vhEntry.getSampleRate();
        }

        /**
         * Set the sample rate for this audio entry.
         * @param newSampleRate The new sample rate.
         */
        public void setSampleRate(int newSampleRate) {
            vhEntry.setSampleRate(newSampleRate);
        }

        /**
         * Gets the bit width for this audio entry.
         * @return bitWidth
         */
        public int getBitWidth() {
            return vhEntry.getBitWidth();
        }

        /**
         * Gets the byte width for this audio entry.
         * @return byteWidth
         */
        public int getByteWidth() {
            return getBitWidth() / Constants.BITS_PER_BYTE;
        }

        /**
         * Sets the bit width for this audio entry.
         * @param newBitWidth The new bit width.
         */
        public void setBitWidth(int newBitWidth) {
            vhEntry.setBitWidth(newBitWidth);
        }
    }
}
