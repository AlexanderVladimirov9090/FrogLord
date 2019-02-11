package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * PSX implementation of ThemeBook.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PSXThemeBook extends ThemeBook {
    private int wadId;
    private int vloId;
    private int multiplayerWadId;
    private int multiplayerVloId;
    private long formLibraryPointer;
    private int deathHeight;

    @Override
    public void load(DataReader reader) {
        this.wadId = reader.readInt();
        this.formLibraryPointer = reader.readUnsignedIntAsLong();
        this.vloId = reader.readInt();
        this.deathHeight = reader.readInt();
        this.multiplayerWadId = reader.readInt();
        this.multiplayerVloId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.wadId);
        writer.writeUnsignedInt(this.formLibraryPointer);
        writer.writeInt(this.vloId);
        writer.writeInt(this.deathHeight);
        writer.writeInt(this.multiplayerWadId);
        writer.writeInt(this.multiplayerVloId);
    }

    @Override
    public VLOArchive getVLO(MAPFile map) {
        return isValid() ? getConfig().getGameFile(map.isMultiplayer() ? getMultiplayerVloId() : getVloId()) : null;
    }

    @Override
    public boolean isValid() {
        return this.vloId != 0 || this.formLibraryPointer != 0 || this.wadId != 0;
    }

    @Override
    public void handleCorrection(String[] args) {
        this.wadId = Integer.parseInt(args[0]);
        this.vloId = Integer.parseInt(args[1]);
        this.multiplayerWadId = Integer.parseInt(args[2]);
        this.multiplayerVloId = Integer.parseInt(args[3]);
        this.formLibraryPointer = Long.decode(args[4]) + getConfig().getRamPointerOffset();
    }

    @Override
    public String toString() {
        return "WAD = " + getConfig().getResourceName(wadId)
                + ", VLO = " + getConfig().getResourceName(vloId)
                + ", mWAD = " + getConfig().getResourceName(multiplayerWadId)
                + ", mVLO = " + getConfig().getResourceName(multiplayerVloId)
                + ", Death Height: " + deathHeight;
    }

}
