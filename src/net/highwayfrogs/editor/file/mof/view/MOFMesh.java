package net.highwayfrogs.editor.file.mof.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.standard.SVector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a MOF Mesh.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFMesh extends FrogMesh<MOFPolygon> {
    private MOFFile mofFile;
    private int animationId;
    private int frameCount;
    private List<SVector> verticeCache = new ArrayList<>();

    public MOFMesh(MOFFile mofFile, TextureMap map) {
        super(map, VertexFormat.POINT_TEXCOORD);
        this.mofFile = mofFile;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();
        getMofFile().forEachPolygon(poly -> addPolygon(poly, texId));
    }

    @Override
    public List<SVector> getVertices() {
        this.verticeCache.clear();
        for (MOFPart part : getMofFile().getParts())
            this.verticeCache.addAll(part.getCel(this.animationId, this.frameCount).getVertices());
        return this.verticeCache;
    }

    /**
     * Set the animation frame.
     * @param newFrame The frame to use.
     */
    public void setFrame(int newFrame) {
        if (newFrame < 0)
            return;

        System.out.println("New Frame: " + newFrame);
        this.frameCount = newFrame;
        updateData();
    }

    /**
     * Set the animation id.
     * @param actionId The frame to use.
     */
    public void setAction(int actionId) {
        if (actionId < 0)
            return;
        //TODO: Upper bounds check?

        System.out.println("New Action: " + actionId);
        this.animationId = actionId;
        setFrame(0);
    }
}
