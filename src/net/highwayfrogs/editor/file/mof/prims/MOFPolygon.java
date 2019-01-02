package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents Playstation polygon data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public abstract class MOFPolygon extends PSXGPUPrimitive {
    private short vertices[];
    private short en[]; // Not entirely sure what this is.
    private short normals[];
    private short padding;
    private PSXColorVector color = new PSXColorVector();

    public static final int TRI_SIZE = 3;
    public static final int QUAD_SIZE = 4;
    public static final int REQUIRES_VERTEX_SWAPPING = QUAD_SIZE;

    public MOFPolygon(int verticeCount, int normalCount, int enCount) {
        this.vertices = new short[verticeCount];
        this.normals = new short[normalCount];
        this.en = new short[enCount];
    }

    @Override
    public final void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readShort();

        for (int i = 0; i < en.length; i++)
            this.en[i] = reader.readShort();

        for (int i = 0; i < normals.length; i++)
            this.normals[i] = reader.readShort();

        if (shouldAddInitialPadding())
            this.padding = reader.readShort(); // Padding? This value seems to sometimes match the last vertices element, and sometimes it doesn't. I don't believe this value is used.

        onLoad(reader);
        this.color.load(reader);
    }

    @Override
    public final void save(DataWriter writer) {
        for (short vertice : vertices)
            writer.writeShort(vertice);

        for (short aShort : en)
            writer.writeShort(aShort);

        for (short normal : normals)
            writer.writeShort(normal);

        if (shouldAddInitialPadding())
            writer.writeShort(this.padding);

        onSave(writer);
        this.color.save(writer);
    }

    public boolean shouldAddInitialPadding() {
        return (en.length + normals.length + vertices.length) % 2 > 0;
    }

    /**
     * Called to load middle-data.
     * @param reader The reader to load data from.
     */
    public void onLoad(DataReader reader) {

    }

    /**
     * Called to save middle data.
     * @param writer The writer to save data to.
     */
    public void onSave(DataWriter writer) {

    }

    /**
     * Convert this into a wavefront object face command.
     * TODO: Add normals.
     * @return faceCommand
     */
    public String toObjFaceCommand(boolean showTextures, AtomicInteger textureCounter) {
        StringBuilder builder = new StringBuilder("f");
        for (int i = this.vertices.length - 1; i >= 0; i--) {
            builder.append(" ").append(this.vertices[i] + 1);
            if (showTextures)
                builder.append("/").append(textureCounter != null ? textureCounter.incrementAndGet() : 0);
        }
        return builder.toString();
    }

    /**
     * Get the order this should be put in a .obj file.
     * @return orderId
     */
    public int getOrderId() {
        return 0;
    }

    /**
     * Get the TextureEntry for this polygon.
     * @param map The map to get this from.
     * @return entry
     */
    public TextureEntry getEntry(TextureMap map) {
        return null;
    }
}
