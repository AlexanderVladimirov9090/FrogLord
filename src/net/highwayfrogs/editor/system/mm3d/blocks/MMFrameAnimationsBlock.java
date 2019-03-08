package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.util.ArrayList;
import java.util.List;

/**
 * The frame animations section describes the frame animations in the model.
 * One object is a single animation.
 * It contains data for each frame.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMFrameAnimationsBlock extends MMDataBlockBody {
    private int flags;
    private String name;
    private float framesPerSecond;
    private List<MMAnimationFrame> frames = new ArrayList<>();

    public MMFrameAnimationsBlock(MisfitModel3DObject parent) {
        super(OffsetType.FRAME_ANIMATIONS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.name = reader.readNullTerminatedString();
        this.framesPerSecond = reader.readFloat();

        int totalFrameCount = reader.readInt(); // The sum of every animation's frame count.
        for (int i = 0; i < totalFrameCount; i++) {
            MMAnimationFrame frame = new MMAnimationFrame(getParent().getVertices().size());
            frame.load(reader);
            this.frames.add(frame);
        }
    }

    /**
     * Return the amount of frames in this animation.
     * @return frameCount
     */
    public int getFrameCount() {
        return this.frames.size();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeTerminatorString(this.name);
        writer.writeFloat(this.framesPerSecond);
        writer.writeInt(getFrameCount());
        this.frames.forEach(frame -> frame.save(writer));
    }

    @Getter
    public static final class MMAnimationFrame extends GameObject {
        private List<MMFloatVertex> vertexPositions = new ArrayList<>();
        private transient int loadVertexCount;

        public MMAnimationFrame(int vertexCount) {
            this.loadVertexCount = vertexCount;
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < getLoadVertexCount(); i++) {
                MMFloatVertex vertex = new MMFloatVertex();
                vertex.load(reader);
                this.vertexPositions.add(vertex);
            }
        }

        @Override
        public void save(DataWriter writer) {
            this.vertexPositions.forEach(position -> position.save(writer));
        }
    }

    @Getter
    public static final class MMFloatVertex extends GameObject {
        private float x;
        private float y;
        private float z;

        @Override
        public void load(DataReader reader) {
            this.x = reader.readFloat();
            this.y = reader.readFloat();
            this.z = reader.readFloat();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeFloat(this.x);
            writer.writeFloat(this.y);
            writer.writeFloat(this.z);
        }
    }
}
