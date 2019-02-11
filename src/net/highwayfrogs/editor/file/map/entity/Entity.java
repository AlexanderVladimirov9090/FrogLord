package net.highwayfrogs.editor.file.map.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.map.form.FormBook;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.Objects;

/**
 * Represents the "ENTITY" struct.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Entity extends GameObject {
    private int formGridId;
    private int uniqueId;
    private FormBook formBook;
    private int flags;
    private EntityData entityData;
    private EntityScriptData scriptData;

    private transient int loadScriptDataPointer;
    private transient int loadReadLength;
    private transient MAPFile map;

    private static final int RUNTIME_POINTERS = 4;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Don't create a live entity while this is set.
    public static final int FLAG_NO_DISPLAY = Constants.BIT_FLAG_1; // Don't display any mesh.
    public static final int FLAG_NO_MOVEMENT = Constants.BIT_FLAG_2; // Don't allow entity movement.
    public static final int FLAG_NO_COLLISION = Constants.BIT_FLAG_3; // Collision does not apply to this entity.
    public static final int FLAG_ALIGN_TO_WORLD = Constants.BIT_FLAG_4; // Entity matrix always aligned to world axes.
    public static final int FLAG_PROJECT_ON_LAND = Constants.BIT_FLAG_5; // Entity position is projected onto the landscape.
    public static final int FLAG_LOCAL_ALIGN = Constants.BIT_FLAG_6; // Entity matrix is calculated locally (Using Y part of entity matrix.)

    public Entity(MAPFile parentMap) {
        this.map = parentMap;
    }

    public Entity(MAPFile file, FormBook formBook) {
        this(file);
        setFormBook(formBook);
    }

    @Override
    public void load(DataReader reader) {
        this.formGridId = reader.readUnsignedShortAsInt();
        this.uniqueId = reader.readUnsignedShortAsInt();
        this.formBook = FormBook.getFormBook(map.getTheme(), reader.readUnsignedShortAsInt());
        this.flags = reader.readUnsignedShortAsInt();
        reader.skipBytes(RUNTIME_POINTERS * Constants.POINTER_SIZE);

        this.loadScriptDataPointer = reader.getIndex();
        if (formBook.getEntity().getScriptDataMaker() != null) {
            this.entityData = formBook.getEntity().getScriptDataMaker().get();
            this.entityData.load(reader);
        }

        if (formBook.getScriptDataMaker() != null) {
            this.scriptData = formBook.getScriptDataMaker().get();
            this.scriptData.load(reader);
        }

        this.loadReadLength = reader.getIndex() - this.loadScriptDataPointer;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.formGridId);
        writer.writeUnsignedShort(this.uniqueId);
        writer.writeUnsignedShort(getFormBook().getRawId());
        writer.writeUnsignedShort(this.flags);
        writer.writeNull(RUNTIME_POINTERS * Constants.POINTER_SIZE);
        if (this.entityData != null)
            this.entityData.save(writer);
        if (this.scriptData != null)
            this.scriptData.save(writer);
    }

    /**
     * Test if this entity has a particular flag.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    /**
     * Get any PathInfo owned by this entity, if it has any.
     * @return pathInfo
     */
    public PathInfo getPathInfo() {
        return getEntityData() instanceof PathData ? ((PathData) getEntityData()).getPathInfo() : null;
    }

    /**
     * Get any PSXMatrix owned by this entity, if it has any.
     * @return psxMatrix
     */
    public PSXMatrix getMatrixInfo() {
        return getEntityData() instanceof MatrixData ? ((MatrixData) getEntityData()).getMatrix() : null;
    }

    /**
     * Get the x, y, z position of this entity.
     * @param map The map file this entity presides in.
     * @return position
     */
    public float[] getPosition(float[] position, MAPFile map) {
        PSXMatrix matrix = getMatrixInfo();
        if (matrix != null) {
            int[] pos = matrix.getTransform();
            position[0] = Utils.fixedPointIntToFloatNBits(pos[0], 20);
            position[1] = Utils.fixedPointIntToFloatNBits(pos[1], 20);
            position[2] = Utils.fixedPointIntToFloatNBits(pos[2], 20);
            return position;
        }

        PathInfo pathInfo = getPathInfo();
        if (pathInfo != null) {
            Path path = map.getPaths().get(pathInfo.getPathId());
            SVector end = path.evaluatePosition(pathInfo);
            position[0] = Utils.fixedPointShortToFloat412(end.getX());
            position[1] = Utils.fixedPointShortToFloat412(end.getY());
            position[2] = Utils.fixedPointShortToFloat412(end.getZ());
            return position;
        }

        throw new UnsupportedOperationException("Tried to get the position of an entity without position data!");
    }

    /**
     * Set this entity's form book.
     * @param newBook This entities new form book.
     */
    public void setFormBook(FormBook newBook) {
        if (this.formBook == null || !Objects.equals(newBook.getScriptDataMaker(), this.formBook.getScriptDataMaker())) {
            this.scriptData = null;
            if (newBook.getScriptDataMaker() != null)
                this.scriptData = newBook.getScriptDataMaker().get();
        }

        if (this.formBook == null || !Objects.equals(newBook.getEntity().getScriptDataMaker(), this.formBook.getEntity().getScriptDataMaker())) {
            PSXMatrix oldMatrix = getMatrixInfo(); // Call before setting entityData to null.
            PathInfo oldPath = getPathInfo();
            this.entityData = null;

            if (newBook.getEntity().getScriptDataMaker() != null) {
                this.entityData = newBook.getEntity().getScriptDataMaker().get();

                if (this.entityData instanceof MatrixData && oldMatrix != null)
                    ((MatrixData) this.entityData).setMatrix(oldMatrix);

                if (this.entityData instanceof PathData && oldPath != null)
                    ((PathData) this.entityData).setPathInfo(oldPath);
            }
        }

        this.formBook = newBook;
    }
}
