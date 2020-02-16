package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;

import java.util.List;

/**
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandSubtract extends PatchCommand {
    public PatchCommandSubtract() {
        super("subtract");
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        PatchValue value = runtime.getVariable(getValueText(args, 0));
        PatchValue otherValue = getValue(runtime, args, 1);

        if (value.isDecimal() || otherValue.isDecimal()) {
            value.setDecimal((value.isDecimal() ? value.getAsDecimal() : value.getAsInteger()) - (otherValue.isDecimal() ? otherValue.getAsDecimal() : otherValue.getAsInteger()));
        } else if (value.isInteger() && otherValue.isInteger()) {
            value.setInteger(value.getAsInteger() - otherValue.getAsInteger());
        } else {
            throw new RuntimeException("Cannot subtract '" + otherValue + "' from '" + value + "'.");
        }
    }

}
