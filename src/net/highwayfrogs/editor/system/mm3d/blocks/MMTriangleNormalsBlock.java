package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Holds the normals for each face of the model.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMTriangleNormalsBlock extends MMDataBlockBody {
    private int index; // Triangle index (0 based)
    private float[] v1Normals = new float[3];
    private float[] v2Normals = new float[3];
    private float[] v3Normals = new float[3];

    public MMTriangleNormalsBlock(MisfitModel3DObject parent) {
        super(OffsetType.TRIANGLE_NORMALS, parent);
    }

    @Override
    public void load(DataReader reader) {
        reader.skipShort(); // Flags
        this.index = reader.readInt();
        readFloatArray(reader, v1Normals);
        readFloatArray(reader, v2Normals);
        readFloatArray(reader, v3Normals);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(0); // Flags
        writer.writeInt(this.index);
        writeFloatArray(writer, v1Normals);
        writeFloatArray(writer, v2Normals);
        writeFloatArray(writer, v3Normals);
    }
}
