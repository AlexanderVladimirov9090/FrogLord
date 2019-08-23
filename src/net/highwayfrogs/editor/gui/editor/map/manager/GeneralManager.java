package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.StartRotation;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Handles general map things.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
@Setter
public class GeneralManager extends MapManager {
    private GUIEditorGrid generalEditor;
    private boolean showGroupBounds;
    private Vector showPosition;

    private static final String GENERIC_POS_LIST = "genericPositionList";
    private static final double GENERIC_POS_SIZE = 3;
    private static final PhongMaterial GENERIC_POS_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);

    public GeneralManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        updateGroupView();
    }

    @Override
    public boolean onKeyPress(KeyEvent event) {
        // Toggle wireframe mode.
        if (event.getCode() == KeyCode.X)
            getController().getMeshView().setDrawMode(getController().getMeshView().getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

        // Toggle fullscreen mode.
        if (event.isControlDown() && event.getCode() == KeyCode.ENTER)
            getController().getOverwrittenStage().setFullScreen(!getController().getOverwrittenStage().isFullScreen());

        return super.onKeyPress(event);
    }

    @Override
    public void setupEditor() {
        if (generalEditor == null)
            generalEditor = new GUIEditorGrid(getController().getGeneralGridPane());

        generalEditor.clearEditor();

        // Setup Map:

        MAPFile map = getMap();
        generalEditor.addLabel("Theme", map.getTheme().name()); // Should look into whether or not this is ok to edit.
        generalEditor.addShortField("Start xTile", map.getStartXTile(), map::setStartXTile, null);
        generalEditor.addShortField("Start zTile", map.getStartZTile(), map::setStartZTile, null);
        generalEditor.addEnumSelector("Start Rotation", map.getStartRotation(), StartRotation.values(), false, map::setStartRotation)
                .setConverter(new AbstractStringConverter<>(StartRotation::getArrow));

        generalEditor.addShortField("Level Timer", map.getLevelTimer(), map::setLevelTimer, null);

        IVector gridOrigin = new IVector(map.getWorldX(map.getStartXTile(), true), -map.getGridStack(map.getStartXTile(), map.getStartZTile()).getHeight(), map.getWorldZ(map.getStartZTile(), true));
        generalEditor.addFloatVector("Camera Source", map.getCameraSourceOffset(), null, getController(), gridOrigin.defaultBits(), gridOrigin);
        generalEditor.addFloatVector("Camera Target", map.getCameraTargetOffset(), null, getController(), gridOrigin.defaultBits(), gridOrigin);
        generalEditor.addSeparator(25);

        // Group:
        generalEditor.addBoldLabel("Group:");
        generalEditor.addShortField("Base Point xTile", map.getBaseXTile(), newVal -> {
            getMap().setBaseXTile(newVal);
            updateGroupView();
        }, null);
        generalEditor.addShortField("Base Point zTile", map.getBaseZTile(), newVal -> {
            getMap().setBaseZTile(newVal);
            updateGroupView();
        }, null);
        generalEditor.addShortField("Group X Count", map.getGroupXCount(), newVal -> {
            getMap().setGroupXCount(newVal);
            updateGroupView();
        }, null);
        generalEditor.addShortField("Group Z Count", map.getGroupZCount(), newVal -> {
            getMap().setGroupZCount(newVal);
            updateGroupView();
        }, null);
        generalEditor.addCheckBox("Show Group Bounds", isShowGroupBounds(), this::setShowGroupBounds);
    }

    /**
     * Update the group display.
     */
    public void updateGroupView() {
        getRenderManager().addMissingDisplayList("groupOutline");
        getRenderManager().clearDisplayList("groupOutline");

        if (!isShowGroupBounds())
            return;

        SVector basePoint = getMap().makeBasePoint();
        float maxX = Utils.fixedPointShortToFloat4Bit((short) (basePoint.getX() + (getMap().getGroupXSize()) * getMap().getGroupXCount()));
        float maxZ = Utils.fixedPointShortToFloat4Bit((short) (basePoint.getZ() + (getMap().getGroupZSize()) * getMap().getGroupZCount()));
        getRenderManager().addBoundingBoxFromMinMax("groupOutline", basePoint.getFloatX(), 0, basePoint.getFloatZ(), maxX, 0, maxZ, Utils.makeSpecialMaterial(Color.YELLOW), true);
    }

    /**
     * Sets whether or not to show group bounds.
     * @param newState The new show state.
     */
    public void setShowGroupBounds(boolean newState) {
        this.showGroupBounds = newState;
        updateGroupView();
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateMarker(Vector vec, int bits, Vector origin) {
        getRenderManager().addMissingDisplayList(GENERIC_POS_LIST);
        getRenderManager().clearDisplayList(GENERIC_POS_LIST);
        this.showPosition = vec;
        if (vec != null) {
            float baseX = vec.getFloatX(bits);
            float baseY = vec.getFloatY(bits);
            float baseZ = vec.getFloatZ(bits);
            if (origin != null) {
                baseX += origin.getFloatX();
                baseY += origin.getFloatY();
                baseZ += origin.getFloatZ();
            }

            getRenderManager().addBoundingBoxFromMinMax(GENERIC_POS_LIST, baseX - GENERIC_POS_SIZE, baseY - GENERIC_POS_SIZE, baseZ - GENERIC_POS_SIZE, baseX + GENERIC_POS_SIZE, baseY + GENERIC_POS_SIZE, baseZ + GENERIC_POS_SIZE, GENERIC_POS_MATERIAL, true);
        }
    }
}
