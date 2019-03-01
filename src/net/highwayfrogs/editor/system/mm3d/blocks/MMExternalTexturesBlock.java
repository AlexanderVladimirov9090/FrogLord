package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;

/**
 * Contains information about external textures.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMExternalTexturesBlock extends MMDataBlockBody {
    private int flags;
    private String path; // File path to texture relative to model (directory separator is backslash)

    private static final String SEPARATOR = "\\";

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.path = reader.readNullTerminatedString();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeTerminatorString(this.path);
    }
}
