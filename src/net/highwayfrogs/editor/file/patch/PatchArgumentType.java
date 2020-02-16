package net.highwayfrogs.editor.file.patch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.patch.argtypes.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A list of the different patch argument types.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
@AllArgsConstructor
public enum PatchArgumentType {
    INT(new IntegerArgument()),
    DECIMAL(new DoubleArgument()),
    STRING(new StringArgument()),
    COLOR(new ColorArgument()),
    BOOLEAN(new BooleanArgument());

    private PatchArgumentBehavior<?> behavior;

    public static Map<String, PatchArgumentType> BY_NAME = new HashMap<>();

    static {
        for (PatchArgumentType type : values())
            BY_NAME.put(type.name().toLowerCase(), type);
    }
}
