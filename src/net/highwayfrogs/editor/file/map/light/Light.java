package net.highwayfrogs.editor.file.map.light;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds lighting data, or the "LIGHT" struct in mapdisp.H
 * Created by Kneesnap on 8/24/2018.
 */
public class Light extends GameObject {
    private byte type;
    private byte priority;
    private short parentId;
    private byte apiType;
    private int color; // BbGgRr
    private SVector position;
    private SVector direction;
    private short attribute0;
    private short attribute1;
    private int framePointer; // Points to li_frame MR_FRAME object.
    private int objectPointer; // Points to MR_OBJECT.

    @Override
    public void load(DataReader reader) {
        this.type = reader.readByte();
        this.priority = reader.readByte();
        this.parentId = reader.readShort();
        this.apiType = reader.readByte();
        reader.readBytes(3); // Padding
        this.color = reader.readInt();
        this.position = SVector.readWithPadding(reader);
        this.direction = SVector.readWithPadding(reader);
        this.attribute0 = reader.readShort();
        this.attribute1 = reader.readShort();
        this.framePointer = reader.readInt();
        this.objectPointer = reader.readInt();

        //TODO: Read frame.
        //TODO: Read object.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.type);
        writer.writeByte(this.priority);
        writer.writeShort(this.parentId);
        writer.writeByte(this.apiType);
        writer.writeNull(3);
        writer.writeInt(this.color);
        this.position.saveWithPadding(writer);
        this.direction.saveWithPadding(writer);
        writer.writeShort(this.attribute0);
        writer.writeShort(this.attribute1);
        writer.writeInt(this.framePointer);
        writer.writeInt(this.objectPointer);

        //TODO: Write frame.
        //TODO: Write object.
    }
}
