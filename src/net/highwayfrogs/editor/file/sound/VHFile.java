package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses VH files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VHFile extends GameFile {
    @Setter private VBFile VB;
    private List<AudioHeader> entries = new ArrayList<>();
    private AtomicInteger suppliedSoundId;

    private static final int ENTRY_LENGTH = 7 * Constants.INTEGER_SIZE;
    public static final int TYPE_ID = 2;
    public static final Image ICON = loadIcon("sound");

    /**
     * Load this file.
     * @param reader  The DataReader.
     * @param soundId The supplied sound id.
     */
    public void load(DataReader reader, AtomicInteger soundId) {
        this.suppliedSoundId = soundId;
        this.load(reader);
        this.suppliedSoundId = null;
    }

    @Override
    public void load(DataReader reader) {
        Utils.verify(this.suppliedSoundId != null, "Tried to load without a supplied sound id.");
        int numEntries = reader.readInt();

        for (int i = 0; i < numEntries; i++) {
            AudioHeader entry = new AudioHeader();

            entry.setChannels(reader.readInt());
            entry.setDataStartOffset(reader.readInt());
            entry.setDataSize(reader.readInt());
            entry.setUnknown1(reader.readInt());
            entry.setUnknown2(reader.readInt());
            entry.setSampleRate(reader.readInt());
            entry.setBitWidth(reader.readInt());

            getEntries().add(entry);
        }

        getVB().load(this); // Load the linked body file.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getEntries().size());

        int offset = 0;
        int lastEntry = -1;
        int lastSize = 0;
        for (AudioHeader entry : getEntries()) {
            writer.writeInt(entry.getChannels());

            // Recalculate data offsets.
            int currentOffset = entry.getDataStartOffset();
            if (lastEntry >= 0 && currentOffset > lastEntry)
                offset += lastSize;

            entry.setDataStartOffset(offset);
            writer.writeInt(entry.getDataStartOffset());
            writer.writeInt(entry.getDataSize());
            writer.writeInt(entry.getUnknown1());
            writer.writeInt(entry.getUnknown2());
            writer.writeInt(entry.getSampleRate());
            writer.writeInt(entry.getBitWidth());
            lastEntry = currentOffset;
            lastSize = entry.getDataSize();
        }
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        Utils.verify(getVB() != null, "VB sound was null.");
        return getVB().makeEditor(); // Build the editor for the right file.
    }

    @Setter
    @Getter
    public static class AudioHeader {
        private int channels;
        private int dataStartOffset;
        private int dataSize;
        private int unknown1;
        private int unknown2;
        private int sampleRate;
        private int bitWidth;
    }
}
