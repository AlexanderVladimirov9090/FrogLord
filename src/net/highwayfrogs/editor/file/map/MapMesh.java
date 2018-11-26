package net.highwayfrogs.editor.file.map;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
public class MapMesh extends TriangleMesh {
    private MAPFile map;

    public MapMesh(MAPFile file) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        loadData();
    }

    /**
     * Load mesh data from the map.
     */
    public void loadData() {
        loadVertices();
        loadTextureCoords();
        loadPolygonData();
        //getFaceSmoothingGroups();
        //getNormals();
    }

    /**
     * Load texture coordinates.
     */
    public void loadTextureCoords() {
        getTexCoords().addAll(1, 1);
        getTexCoords().addAll(1, 0);
        getTexCoords().addAll(0, 1);
        getTexCoords().addAll(0, 0);
    }

    /**
     * Load polygon data.
     */
    public void loadPolygonData() {
        map.getCachedPolygons().values().forEach(prim -> {
            if (!(prim instanceof PSXPolygon))
                return;

            PSXPolygon poly = (PSXPolygon) prim;
            int vertCount = poly.getVertices().length;

            if (vertCount == PSXPolygon.TRI_SIZE) {
                addTriangle(poly);
            } else if (vertCount == PSXPolygon.QUAD_SIZE) {
                addRectangle(poly);
            } else {
                throw new RuntimeException("Cannot handle " + vertCount + "vertices");
            }
        });
    }

    /**
     * Add a rectangle polygon.
     * @param poly The rectangle polygon.
     */
    public void addRectangle(PSXPolygon poly) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.QUAD_SIZE, "This polygon has %d vertices!", verts.length);
        getFaces().addAll(verts[0], 0, verts[1], 1, verts[3], 3);
        getFaces().addAll(verts[0], 0, verts[2], 2, verts[3], 3);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(PSXPolygon poly) {
        Utils.verify(poly.getVertices().length == PSXPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVertices().length);

        int vId = 0;
        for (short vertice : poly.getVertices())
            getFaces().addAll(vertice, vId++);
    }

    /**
     * Load vertex data.
     */
    public void loadVertices() {
        getPoints().clear();
        for (SVector vertex : map.getVertexes())
            getPoints().addAll(-Utils.unsignedShortToFloat(vertex.getX()) * 100, -Utils.unsignedShortToFloat(vertex.getY()) * 100, Utils.unsignedShortToFloat(vertex.getZ()) * 100);

        for (int i = 0; i < 6; i++)
            System.out.println(getPoints().get(i));
    }
}
