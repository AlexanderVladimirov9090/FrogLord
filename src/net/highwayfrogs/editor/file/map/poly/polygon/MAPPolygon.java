package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents Playstation polygon data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public abstract class MAPPolygon extends MAPPrimitive {
    private short padding;
    @Setter private transient boolean flippedVertices;

    public static final int TRI_SIZE = 3;
    public static final int QUAD_SIZE = 4;
    public static final int REQUIRES_VERTEX_PADDING = TRI_SIZE;
    public static final int REQUIRES_VERTEX_SWAPPING = QUAD_SIZE;

    public MAPPolygon(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount);
    }

    @Override
    public MAPPolygonType getType() {
        return (MAPPolygonType) super.getType();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        swapIfNeeded();
        if (getVerticeCount() == REQUIRES_VERTEX_PADDING)
            this.padding = reader.readShort(); // Padding? This value seems to sometimes match the last vertices element, and sometimes it doesn't. I don't believe this value is used. Most likely it is whatever value was there when malloc was run, but we preserve it just in case it's important.
    }

    @Override
    public void save(DataWriter writer) {
        boolean swap = isFlippedVertices();
        if (swap)
            swapIfNeeded(); // Swap back to default flip state.

        super.save(writer);

        if (getVerticeCount() == REQUIRES_VERTEX_PADDING)
            writer.writeShort(this.padding);

        if (swap)
            swapIfNeeded(); // Swap them back.
    }

    private void swapIfNeeded() {
        if (getVerticeCount() != REQUIRES_VERTEX_SWAPPING)
            return; // We only need to swap vertexes 2 and 3 if there are 4 vertexes.

        // I forget exactly why we swap this, but it seems to work right when we do.
        int swap = getVertices()[2];
        getVertices()[2] = getVertices()[3];
        getVertices()[3] = swap;
        this.flippedVertices = !this.flippedVertices;
    }

    /**
     * Convert this into a wavefront object face command.
     * @return faceCommand
     */
    public String toObjFaceCommand(boolean showTextures, AtomicInteger textureCounter) {
        StringBuilder builder = new StringBuilder("f");
        for (int i = getVerticeCount() - 1; i >= 0; i--) {
            builder.append(" ").append(getVertices()[i] + 1);
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
     * Calculate geometric center point of a polygon.
     * @return Center of a polygon, else null.
     */
    public static SVector getCenterOfPolygon(MapMesh mesh, MAPPolygon poly) {
        if (poly == null)
            return null;

        int[] vertexIndices = poly.getVertices();

        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;

        for (int index : vertexIndices) {
            x += mesh.getVertices().get(index).getFloatX();
            y += mesh.getVertices().get(index).getFloatY();
            z += mesh.getVertices().get(index).getFloatZ();
        }

        x /= vertexIndices.length;
        y /= vertexIndices.length;
        z /= vertexIndices.length;

        return new SVector(x, y, z);

    }
}
