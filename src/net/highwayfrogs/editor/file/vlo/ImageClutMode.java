package net.highwayfrogs.editor.file.vlo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Image clut modes.
 * Created by Kneesnap on 8/23/2019.
 */
@Getter
@AllArgsConstructor
public enum ImageClutMode {
    MODE_4BIT(4, "4-Bit"),
    MODE_8BIT(2, "8-Bit"),
    MODE_15BIT_NO_CLUT(1, "15-Bit No CLUT");

    private final int multiplier;
    private final String displayName;
}
