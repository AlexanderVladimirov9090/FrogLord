package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Type B data block.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMDataBlockHeader extends GameObject {
    private List<MMDataBlockBody> dataBlockBodies = new ArrayList<>();
    private OffsetType offsetType;
    private int invalidBodies;
    private static final short FLAGS = 0x00;

    public MMDataBlockHeader(OffsetType type) {
        this.offsetType = type;
    }

    @Override
    public void load(DataReader reader) {
        reader.skipShort();
        long elementCount = reader.readUnsignedIntAsLong();

        if (getOffsetType().isTypeA()) {
            for (int i = 0; i < elementCount; i++) {
                int elementSize = reader.readInt(); // Keep this.
                int readGoal = (reader.getIndex() + elementSize);
                MMDataBlockBody body = getOffsetType().makeNew();
                body.load(reader);
                if (reader.getIndex() != readGoal) {
                    System.out.println("[A/" + this.dataBlockBodies.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementSize + ", " + elementCount + ")");
                    this.invalidBodies++;
                    continue;
                }
                this.dataBlockBodies.add(body);
            }
        } else if (getOffsetType().isTypeB()) {
            int elementSize = reader.readInt(); // Keep this.
            for (int i = 0; i < elementCount; i++) {
                int readGoal = (reader.getIndex() + elementSize);
                MMDataBlockBody body = getOffsetType().makeNew();
                body.load(reader);
                if (reader.getIndex() != readGoal) {
                    System.out.println("[B/" + this.dataBlockBodies.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementCount + ")");
                    this.invalidBodies++;
                    continue;
                }
                this.dataBlockBodies.add(body);
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FLAGS);
        writer.writeUnsignedInt(dataBlockBodies.size() + this.invalidBodies);

        if (getOffsetType().isTypeA()) {
            for (MMDataBlockBody body : dataBlockBodies) {
                int writeSizeTo = writer.writeNullPointer();
                int structStart = writer.getIndex();
                body.save(writer);
                writer.writeAddressAt(writeSizeTo, (writer.getIndex() - structStart));
            }
        } else if (getOffsetType().isTypeB()) {
            int writeSizeTo = writer.writeNullPointer();
            int structStart = writer.getIndex();
            this.dataBlockBodies.forEach(block -> block.save(writer));
            writer.writeAddressAt(writeSizeTo, (writer.getIndex() - structStart) / this.dataBlockBodies.size());
        }
    }
}
