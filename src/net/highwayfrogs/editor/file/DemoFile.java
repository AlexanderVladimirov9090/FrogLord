package net.highwayfrogs.editor.file;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a frogger key-map sequence that plays if you idle on the main menu for too long.
 * Created by Kneesnap on 8/14/2018.
 */
@Getter
public class DemoFile extends GameFile {
    private List<List<DemoAction>> actions = new ArrayList<>();
    private int startX;
    private int startZ;
    private int startY;

    public static final int TYPE_ID = 6;
    private static final Image ICON = loadIcon("demo");
    private static final int FILE_SIZE = 1812;

    @Override
    public void load(DataReader reader) {
        this.startX = reader.readInt();
        this.startZ = reader.readInt();
        this.startY = reader.readInt();

        while (reader.hasMore()) {
            byte actionId = reader.readByte();

            List<DemoAction> actions = new ArrayList<>();
            for (DemoAction action : DemoAction.values())
                if (action.test(actionId))
                    actions.add(action);

            Utils.verify(!actions.isEmpty(), "Unknown action for action id 0x%s.", Utils.toByteString(actionId));
            getActions().add(actions);
            if (actions.contains(DemoAction.STOP))
                break;
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getStartX());
        writer.writeInt(getStartZ());
        writer.writeInt(getStartY());

        for (List<DemoAction> actions : getActions()) {
            byte result = 0;
            for (DemoAction action : actions)
                result |= action.getId();
            writer.writeByte(result);
        }

        writer.jumpTo(FILE_SIZE); // Jump to end of file.
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Getter
    @AllArgsConstructor
    public enum DemoAction {
        UP("Move Up", 0x00, false),
        RIGHT("Move Right", 0x01, false),
        DOWN("Move Down", 0x02, false),
        LEFT("Move Left", 0x03, false),
        SUPER_HOP("Super Hop", 0x08, true),
        TONGUE("Tongue", 0x10, true),
        ROTATE_COUNTER_CLOCKWISE("Rotate Camera Counter-Clockwise", 0x20, true),
        ROTATE_CLOCKWISE("Rotate Camera Clockwise", 0x40, true),
        SKIP("Do Nothing", 0x80, true),
        STOP("End Demo", 0xA0, false);

        private String info;
        private int id;
        private boolean additive;

        public boolean test(byte actionId) {
            return isAdditive()
                    ? (actionId & getId()) == getId()
                    : actionId == getId();
        }
    }
}
