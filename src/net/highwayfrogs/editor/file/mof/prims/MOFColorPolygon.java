package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.mof.MOFPart;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents a vertex color mof polygon.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFColorPolygon extends MOFPolygon implements VertexColor {
    @Setter private transient TextureEntry textureEntry;

    public MOFColorPolygon(MOFPart parent, MOFPrimType type, int verticeCount, int normalCount, int enCount) {
        super(parent, type, verticeCount, normalCount, enCount);
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return getTextureEntry();
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        graphics.setColor(getColor().toColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }
}
