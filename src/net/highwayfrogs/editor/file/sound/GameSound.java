package net.highwayfrogs.editor.file.sound;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Represents a game sound.
 * Created by Kneesnap on 9/24/2018.
 */
@Getter
public abstract class GameSound extends GameObject {
    private AudioHeader header;
    private int vanillaTrackId;
    private int readLength;

    public GameSound(AudioHeader header, int vanillaTrackId, int readLength) {
        this.vanillaTrackId = vanillaTrackId;
        this.header = header;
        this.readLength = readLength;
    }

    /**
     * Gets a playable audio-clip.
     * @return clip
     */
    @SneakyThrows
    public Clip getClip() {
        return toStandardAudio();
    }

    /**
     * Export this audio entry as a standard audio clip.
     * @return audioClip
     */
    public abstract Clip toStandardAudio() throws LineUnavailableException, IOException, UnsupportedAudioFileException;

    /**
     * Export this audio clip to a file.
     * @param saveTo The audio file to export.
     */
    public abstract void exportToFile(File saveTo) throws IOException, LineUnavailableException, UnsupportedAudioFileException;

    /**
     * Import a sound file to override
     * @param file The file to replace this sound with.
     */
    public abstract void replaceWithFile(File file) throws IOException, UnsupportedAudioFileException;

    /**
     * Gets the audio format used by this GameSound.
     * @return audioFormat
     */
    public AudioFormat getAudioFormat() {
        return new AudioFormat(getSampleRate(), getBitWidth(), VHFile.CHANNEL_COUNT, true, false);
    }

    /**
     * Import a new AudioFormat.
     * @param newFormat The new AudioFormat to import.
     */
    public void importFormat(AudioFormat newFormat) {
        Utils.verify(!newFormat.isBigEndian(), "Big Endian audio files are not accepted.");
        Utils.verify(newFormat.getEncoding() == Encoding.PCM_SIGNED, "Unsigned audio files are not supported. (%s)", newFormat.getEncoding());
        Utils.verify(newFormat.getChannels() == VHFile.CHANNEL_COUNT, "%d-channel audio is not supported!", newFormat.getChannels());

        setBitWidth(newFormat.getSampleSizeInBits());
        setSampleRate((int) newFormat.getSampleRate());
    }

    /**
     * Gets the sample rate of this sound.
     * @return sampleRate
     */
    public int getSampleRate() {
        return header.getSampleRate();
    }

    /**
     * Sets the sample rate for this sound.
     * @param newSampleRate The sample rate to set.
     */
    public void setSampleRate(int newSampleRate) {
        header.setSampleRate(newSampleRate);
    }

    /**
     * Gets the bit width for this sound.
     * @return bitWidth
     */
    public int getBitWidth() {
        return header.getBitWidth();
    }

    /**
     * Sets the bit width for this sound.
     * @param newBitWidth The bit width to set.
     */
    public void setBitWidth(int newBitWidth) {
        header.setBitWidth(newBitWidth);
    }

    /**
     * Gets the byte width for this sound.
     * @return byteWidth
     */
    public int getByteWidth() {
        return getBitWidth() / Constants.BITS_PER_BYTE;
    }

    /**
     * Sets the byte-size of this sound's audio data.
     * @param newSize The audio data's new size.
     */
    public void setDataSize(int newSize) {
        header.setDataSize(newSize);
    }

    /**
     * Get the name of this sound.
     * @return soundName
     */
    public String getSoundName() {
        return getConfig().getSoundBank().getName(getVanillaTrackId());
    }

    /**
     * Called when a sound is imported over the existing one.
     */
    public void onImport() {

    }
}
