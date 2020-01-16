package net.highwayfrogs.editor.file.patch;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.gui.GUIMain;

import java.util.*;

/**
 * Loads a patch from a config.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
public class GamePatch {
    private String name;
    private String description;
    private String author;
    private Set<String> supportedVersions = new HashSet<>();
    private List<String> code = new ArrayList<>();
    private Map<String, Map<String, PatchValue>> versionSpecificVariables = new HashMap<>();
    private List<PatchArgument> arguments = new ArrayList<>();

    /**
     * Loads patch data from the config.
     * @param config The config to read from.
     */
    public void loadPatchFromConfig(Config config) {
        this.name = config.getString("Name");
        this.description = config.getString("Description", null);
        this.author = config.getString("Author", null);
        Collections.addAll(this.supportedVersions, config.getString("Versions").split(","));

        // Read arguments.
        if (config.hasChild("Args"))
            for (String line : config.getChild("Args").getText())
                if (line.length() > 0)
                    this.arguments.add(PatchArgument.parsePatchArgument(line));

        // Read code.
        if (config.hasChild("Code"))
            this.code.addAll(config.getChild("Code").getText());

        // Read version-specific variables.
        for (String versionKey : GUIMain.getVersions().keySet()) {
            if (!config.hasChild(versionKey))
                continue;

            Config child = config.getChild(versionKey);
            Map<String, PatchValue> versionValues = new HashMap<>();
            for (String key : child.keySet())
                versionValues.put(key, PatchValue.parseStringAsPatchValue(child.getString(key)));
            this.versionSpecificVariables.put(versionKey, versionValues);
        }
    }

    /**
     * Test if this patch is compatible with a given version.
     * @param version The version to test compatibility with.
     * @return isCompatible
     */
    public boolean isCompatibleWithVersion(String version) {
        return this.supportedVersions.contains(version);
    }
}
