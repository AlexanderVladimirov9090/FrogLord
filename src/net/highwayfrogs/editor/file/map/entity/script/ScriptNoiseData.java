package net.highwayfrogs.editor.file.map.entity.script;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Loads and saves sound radius data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptNoiseData extends EntityScriptData {
    private int minRadius;
    private int maxRadius;

    @Override
    public void load(DataReader reader) {
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Min Radius", String.valueOf(minRadius)));
        table.getItems().add(new NameValuePair("Max Radius", String.valueOf(maxRadius)));
    }
}
