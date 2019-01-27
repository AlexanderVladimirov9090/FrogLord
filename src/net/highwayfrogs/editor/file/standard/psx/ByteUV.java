package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Holds texture UV information.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ByteUV extends GameObject {
    private short u;
    private short v;

    public static final int BYTE_SIZE = 2 * Constants.BYTE_SIZE;

    @Override
    public void load(DataReader reader) {
        this.u = reader.readUnsignedByteAsShort();
        this.v = reader.readUnsignedByteAsShort();
    }

    /**
     * Get U as a float ranging from 0 to 1.
     * @return floatU
     */
    public float getFloatU() {
        return Utils.unsignedByteToFloat(Utils.unsignedShortToByte(this.u));
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * @return floatV
     */
    public float getFloatV() {
        return Utils.unsignedByteToFloat(Utils.unsignedShortToByte(this.v));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.u);
        writer.writeUnsignedByte(this.v);
    }

    /**
     * Get this as an OBJ vt command.
     * @return objTextureString
     */
    public String toObjTextureString() {
        return "vt " + getFloatU() + " " + getFloatV();
    }

    /**
     * Setup an editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(GUIEditorGrid editor) {
        editor.addShortField("U", getU(), this::setU, null);
        editor.addShortField("V", getV(), this::setV, null);
    }
}
