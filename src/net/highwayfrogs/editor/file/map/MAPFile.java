package net.highwayfrogs.editor.file.map;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.group.MAPGroup;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.zone.Zone;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;
import net.highwayfrogs.editor.file.standard.psx.prims.line.PSXLineType;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.*;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.editor.MAPController;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Parses Frogger MAP files.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public class MAPFile extends GameFile {
    private short startXTile;
    private short startYTile;
    private short startRotation;
    private MAPTheme theme;
    private short checkPointTimers[] = new short[5]; // Each frog (checkpoint) has its own timer value. In the vanilla game, they all match.
    private SVector cameraSourceOffset;
    private SVector cameraTargetOffset;
    private List<Path> paths = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();
    private List<Form> forms = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private SVector basePoint; // This is the bottom left of the map group grid.
    private List<MAPGroup> groups = new ArrayList<>();
    private List<SVector> vertexes = new ArrayList<>();
    private List<GridStack> gridStacks = new ArrayList<>();
    private List<GridSquare> gridSquares = new ArrayList<>();
    private List<MAPAnimation> mapAnimations = new ArrayList<>();

    private Map<PSXPrimitiveType, List<PSXGPUPrimitive>> loosePolygons = new HashMap<>();
    private Map<PSXPrimitiveType, List<PSXGPUPrimitive>> cachedPolygons = new HashMap<>();

    private MWDFile parentMWD;

    private short groupXCount;
    private short groupZCount;
    private short groupXLength;
    private short groupZLength;

    private short gridXCount;
    private short gridZCount;
    private short gridXLength;
    private short gridZLength;

    private transient Map<PSXGPUPrimitive, Integer> loadPolygonPointerMap = new HashMap<>();
    private transient Map<Integer, PSXGPUPrimitive> loadPointerPolygonMap = new HashMap<>();

    private transient Map<PSXGPUPrimitive, Integer> savePolygonPointerMap = new HashMap<>();
    private transient Map<Integer, PSXGPUPrimitive> savePointerPolygonMap = new HashMap<>();

    public static final int TYPE_ID = 0;
    private static final String SIGNATURE = "FROG";
    private static final String VERSION = "2.00";
    private static final String COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;
    private static final String GENERAL_SIGNATURE = "GENE";
    private static final String PATH_SIGNATURE = "PATH";
    private static final String ZONE_SIGNATURE = "ZONE";
    private static final String FORM_SIGNATURE = "FORM";
    private static final String ENTITY_SIGNATURE = "EMTP";
    private static final String GRAPHICAL_SIGNATURE = "GRAP";
    private static final String LIGHT_SIGNATURE = "LITE";
    private static final String GROUP_SIGNATURE = "GROU";
    private static final String POLYGON_SIGNATURE = "POLY"; // Supported.
    private static final String VERTEX_SIGNATURE = "VRTX"; // Supported.
    private static final String GRID_SIGNATURE = "GRID";
    private static final String ANIMATION_SIGNATURE = "ANIM";

    public static final short MAP_ANIMATION_TEXTURE_LIST_TERMINATOR = (short) 0xFFFF;

    public static final Image ICON = loadIcon("map");
    public static final List<PSXPrimitiveType> PRIMITIVE_TYPES = new ArrayList<>();

    public MAPFile(MWDFile parent) {
        this.parentMWD = parent;
    }

    static {
        PRIMITIVE_TYPES.addAll(Arrays.asList(PSXPolygonType.values()));
        PRIMITIVE_TYPES.add(PSXLineType.G2);
    }

    /**
     * Remove an entity from this map.
     * @param entity The entity to remove.
     */
    public void removeEntity(Entity entity) {
        getEntities().remove(entity);
        getGroups().forEach(group -> group.getEntities().remove(entity));
    }

    @Override
    public void load(DataReader reader) {
        getLoadPolygonPointerMap().clear();
        getLoadPointerPolygonMap().clear();

        reader.verifyString(SIGNATURE);
        reader.readInt(); // File length.
        reader.verifyString(VERSION);
        reader.readString(COMMENT_BYTES); // Comment bytes.

        int generalAddress = reader.readInt();
        int graphicalAddress = reader.readInt();
        int formAddress = reader.readInt();
        int entityAddress = reader.readInt();
        int zoneAddress = reader.readInt();
        int pathAddress = reader.readInt();

        reader.setIndex(generalAddress);
        reader.verifyString(GENERAL_SIGNATURE);
        this.startXTile = reader.readShort();
        this.startYTile = reader.readShort();
        this.startRotation = reader.readShort();
        this.theme = MAPTheme.values()[reader.readShort()];

        for (int i = 0; i < checkPointTimers.length; i++)
            this.checkPointTimers[i] = reader.readShort();

        reader.readShort(); // Unused perspective variable.

        this.cameraSourceOffset = new SVector();
        this.cameraSourceOffset.loadWithPadding(reader);

        this.cameraTargetOffset = new SVector();
        this.cameraTargetOffset.loadWithPadding(reader);
        reader.readBytes(4 * Constants.SHORT_SIZE); // Unused "LEVEL_HEADER" data.

        reader.setIndex(pathAddress);
        reader.verifyString(PATH_SIGNATURE);
        int pathCount = reader.readInt();

        // PATH
        for (int i = 0; i < pathCount; i++) {
            reader.jumpTemp(reader.readInt()); // Starts after the pointers.
            Path path = new Path();
            path.load(reader);
            this.paths.add(path);
            reader.jumpReturn();
        }

        // Read Camera Zones.
        reader.setIndex(zoneAddress);
        reader.verifyString(ZONE_SIGNATURE);
        int zoneCount = reader.readInt();

        for (int i = 0; i < zoneCount; i++) {
            reader.jumpTemp(reader.readInt()); // Move to the zone location.
            Zone zone = new Zone();
            zone.load(reader);
            this.zones.add(zone);
            reader.jumpReturn();
        }

        // Read forms.
        reader.setIndex(formAddress);
        reader.verifyString(FORM_SIGNATURE);
        short formCount = reader.readShort();
        reader.readShort(); // Padding.

        for (int i = 0; i < formCount; i++) {
            reader.jumpTemp(reader.readInt());
            Form form = new Form();
            form.load(reader);
            forms.add(form);
            reader.jumpReturn();
        }

        // Read entities
        reader.setIndex(entityAddress);
        reader.verifyString(ENTITY_SIGNATURE);
        reader.readInt(); // Entity packet length.
        int entityCount = reader.readShort();
        reader.readShort(); // Padding.

        Entity lastEntity = null;
        for (int i = 0; i < entityCount; i++) {
            int newEntityPointer = reader.readInt();
            printInvalidEntityReadDetection(lastEntity, newEntityPointer);

            reader.jumpTemp(newEntityPointer);
            Entity entity = new Entity(this);
            entity.load(reader);
            entities.add(entity);
            reader.jumpReturn();
            lastEntity = entity;
        }
        printInvalidEntityReadDetection(lastEntity, graphicalAddress); // Go over the last entity.

        reader.setIndex(graphicalAddress);
        reader.verifyString(GRAPHICAL_SIGNATURE);
        int lightAddress = reader.readInt();
        int groupAddress = reader.readInt();
        int polygonAddress = reader.readInt();
        int vertexAddress = reader.readInt();
        int gridAddress = reader.readInt();
        int animAddress = reader.readInt();

        reader.setIndex(lightAddress);
        reader.verifyString(LIGHT_SIGNATURE);
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            Light light = new Light();
            light.load(reader);
            lights.add(light);
        }

        reader.setIndex(groupAddress);
        reader.verifyString(GROUP_SIGNATURE);
        this.basePoint = SVector.readWithPadding(reader);
        this.groupXCount = reader.readShort(); // Number of groups in x.
        this.groupZCount = reader.readShort(); // Number of groups in z.
        this.groupXLength = reader.readShort(); // Group X Length
        this.groupZLength = reader.readShort(); // Group Z Length
        int groupCount = groupXCount * groupZCount;

        for (int i = 0; i < groupCount; i++) {
            MAPGroup group = new MAPGroup(this);
            group.load(reader);
            groups.add(group);
        }

        // Read POLY
        reader.setIndex(polygonAddress);
        reader.verifyString(POLYGON_SIGNATURE);

        Map<PSXPrimitiveType, Short> polyCountMap = new HashMap<>();
        Map<PSXPrimitiveType, Integer> polyOffsetMap = new HashMap<>();

        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            polyCountMap.put(type, reader.readShort());
        reader.readShort(); // Padding.
        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            polyOffsetMap.put(type, reader.readInt());

        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            short polyCount = polyCountMap.get(type);
            int polyOffset = polyOffsetMap.get(type);

            List<PSXGPUPrimitive> primitives = new ArrayList<>();
            loosePolygons.put(type, primitives);

            if (polyCount > 0) {
                reader.jumpTemp(polyOffset);

                for (int i = 0; i < polyCount; i++) {
                    PSXGPUPrimitive primitive = type.newPrimitive();
                    getLoadPolygonPointerMap().put(primitive, reader.getIndex());
                    getLoadPointerPolygonMap().put(reader.getIndex(), primitive);
                    primitive.load(reader);
                    primitives.add(primitive);
                }

                reader.jumpReturn();
            }
        }
        getGroups().forEach(group -> group.setupPolygonData(getLoosePolygons()));
        updatePolygonCache();

        // Read Vertexes.
        reader.setIndex(vertexAddress);
        reader.verifyString(VERTEX_SIGNATURE);
        short vertexCount = reader.readShort();
        reader.readShort(); // Padding.
        for (int i = 0; i < vertexCount; i++)
            this.vertexes.add(SVector.readWithPadding(reader));

        // Read GRID data.
        reader.setIndex(gridAddress);
        reader.verifyString(GRID_SIGNATURE);
        this.gridXCount = reader.readShort(); // Number of grid squares in x.
        this.gridZCount = reader.readShort();
        this.gridXLength = reader.readShort(); // Grid square x length.
        this.gridZLength = reader.readShort();

        int stackCount = gridXCount * gridZCount;
        for (int i = 0; i < stackCount; i++) {
            GridStack stack = new GridStack();
            stack.load(reader);
            getGridStacks().add(stack);
        }

        // Find the total amount of squares to read.
        int squareCount = 0;
        for (GridStack stack : gridStacks)
            squareCount = Math.max(squareCount, stack.getIndex() + stack.getSquareCount());

        for (int i = 0; i < squareCount; i++) {
            GridSquare square = new GridSquare(this);
            square.load(reader);
            gridSquares.add(square);
        }

        // Read "ANIM".
        reader.setIndex(animAddress);
        reader.verifyString(ANIMATION_SIGNATURE);
        int mapAnimCount = reader.readInt(); // 0c
        int mapAnimAddress = reader.readInt(); // 0x2c144
        reader.setIndex(mapAnimAddress); // This points to right after the header.

        for (int i = 0; i < mapAnimCount; i++) {
            MAPAnimation animation = new MAPAnimation(this);
            animation.load(reader);
            mapAnimations.add(animation);
        }
    }

    @Override
    public void save(DataWriter writer) {
        getSavePointerPolygonMap().clear();
        getSavePolygonPointerMap().clear();

        //if (MWDFile.CURRENT_FILE_NAME.equalsIgnoreCase("SUB1.MAP"))
        //    fixDEV1();

        // Write File Header
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(0); // File length. (Unused)
        writer.writeStringBytes(VERSION);
        writer.writeNull(COMMENT_BYTES);

        int generalAddress = writer.getIndex();
        int graphicalAddress = generalAddress + Constants.INTEGER_SIZE;
        int formAddress = graphicalAddress + Constants.INTEGER_SIZE;
        int entityAddress = formAddress + Constants.INTEGER_SIZE;
        int zoneAddress = entityAddress + Constants.INTEGER_SIZE;
        int pathAddress = zoneAddress + Constants.INTEGER_SIZE;
        int writeAddress = pathAddress + Constants.INTEGER_SIZE;

        // Write GENERAL.
        writer.jumpTo(writeAddress);
        writer.jumpTemp(generalAddress);
        writer.writeInt(writeAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GENERAL_SIGNATURE);
        writer.writeShort(this.startXTile);
        writer.writeShort(this.startYTile);
        writer.writeShort(this.startRotation);
        writer.writeShort((short) getTheme().ordinal());
        for (short timerValue : this.checkPointTimers)
            writer.writeShort(timerValue);

        writer.writeShort((short) 0); // Unused perspective variable.
        this.cameraSourceOffset.saveWithPadding(writer);
        this.cameraTargetOffset.saveWithPadding(writer);
        writer.writeNull(4 * Constants.SHORT_SIZE); // Unused "LEVEL_HEADER" data.

        // Write "PATH".
        int tempAddress = writer.getIndex();
        writer.jumpTemp(pathAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(PATH_SIGNATURE);

        int pathCount = this.paths.size();
        writer.writeInt(pathCount); // Path count.

        int pathPointer = writer.getIndex() + (Constants.POINTER_SIZE * pathCount);
        for (Path path : getPaths()) {
            writer.writeInt(pathPointer);

            writer.jumpTemp(pathPointer);
            path.save(writer);
            pathPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(pathPointer);

        // Save "ZONE".
        tempAddress = writer.getIndex();
        writer.jumpTemp(zoneAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ZONE_SIGNATURE);

        int zoneCount = this.zones.size();
        writer.writeInt(zoneCount);

        int zonePointer = writer.getIndex() + (Constants.POINTER_SIZE * zoneCount);
        for (Zone zone : getZones()) {
            writer.writeInt(zonePointer);

            writer.jumpTemp(zonePointer);
            zone.save(writer);
            zonePointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(zonePointer); // Start writing FORM AFTER zone-data. Otherwise, it will write the form data to where the zone data goes.

        // Save "FORM".
        tempAddress = writer.getIndex();
        writer.jumpTemp(formAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(FORM_SIGNATURE);
        short formCount = (short) this.forms.size();
        writer.writeShort(formCount);
        writer.writeShort((short) 0); // Padding.

        int formPointer = writer.getIndex() + (Constants.POINTER_SIZE * formCount);
        for (Form form : getForms()) {
            writer.writeInt(formPointer);

            writer.jumpTemp(formPointer);
            form.save(writer);
            formPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(formPointer);

        // Write "EMTP".
        tempAddress = writer.getIndex();
        writer.jumpTemp(entityAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ENTITY_SIGNATURE);
        writer.writeInt(0); // This is the entity packet length. It is unused.
        short entityCount = (short) this.entities.size();
        writer.writeShort(entityCount);
        writer.writeShort((short) 0); // Padding.

        int entityPointer = writer.getIndex() + (Constants.POINTER_SIZE * entityCount);
        for (Entity entity : getEntities()) {
            writer.writeInt(entityPointer);

            writer.jumpTemp(entityPointer);
            entity.save(writer);
            entityPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(entityPointer);

        // Write GRAP.
        tempAddress = writer.getIndex();
        writer.jumpTemp(graphicalAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GRAPHICAL_SIGNATURE);
        int lightAddress = writer.getIndex();
        int groupAddress = lightAddress + Constants.POINTER_SIZE;
        int polygonAddress = groupAddress + Constants.POINTER_SIZE;
        int vertexAddress = polygonAddress + Constants.POINTER_SIZE;
        int gridAddress = vertexAddress + Constants.POINTER_SIZE;
        int animAddress = gridAddress + Constants.POINTER_SIZE;
        writer.setIndex(animAddress + Constants.POINTER_SIZE);

        // Write LITE.
        tempAddress = writer.getIndex();
        writer.jumpTemp(lightAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(LIGHT_SIGNATURE);
        writer.writeInt(lights.size());
        getLights().forEach(light -> light.save(writer));

        // Write GROU.
        updatePolygonCache();

        tempAddress = writer.getIndex();
        writer.jumpTemp(groupAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GROUP_SIGNATURE);
        this.basePoint.saveWithPadding(writer);
        writer.writeShort(this.groupXCount);
        writer.writeShort(this.groupZCount);
        writer.writeShort(this.groupXLength);
        writer.writeShort(this.groupZLength);
        getGroups().forEach(group -> group.save(writer));

        // Write POLY
        tempAddress = writer.getIndex();
        writer.jumpTemp(polygonAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(POLYGON_SIGNATURE);
        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            writer.writeShort((short) getCachedPolygons().get(type).size());

        writer.writeShort((short) 0); // Padding.

        Map<PSXPrimitiveType, Integer> polyAddresses = new HashMap<>();

        int lastPointer = writer.getIndex();
        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            polyAddresses.put(type, lastPointer);
            lastPointer += Constants.POINTER_SIZE;
        }

        writer.setIndex(lastPointer);
        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            if (type == PSXLineType.G2)
                continue; // G2 was only enabled as a debug rendering option. It is not enabled in the retail release fairly sure.

            tempAddress = writer.getIndex();

            writer.jumpTemp(polyAddresses.get(type));
            writer.writeInt(tempAddress);
            writer.jumpReturn();

            getCachedPolygons().get(type).forEach(polygon -> {
                getSavePointerPolygonMap().put(writer.getIndex(), polygon);
                getSavePolygonPointerMap().put(polygon, writer.getIndex());
                polygon.save(writer);
            });
        }

        // Write MAP_GROUP polygon pointers, since we've written polygon data.
        getGroups().forEach(group -> group.writePolygonPointers(writer));

        AtomicInteger entityIndicePointer = new AtomicInteger(writer.getIndex());
        getGroups().forEach(group -> {
            writer.jumpTemp(entityIndicePointer.get());
            group.writeEntityList(writer);
            entityIndicePointer.set(writer.getIndex());
            writer.jumpReturn();
        });

        // Setup pa_entity_indices. The one problem with this is that the beaver will not render when we do this.
        int emptyEntityIdArray = writer.getIndex();
        writer.writeShort(MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
        getPaths().forEach(path -> path.writePointer(writer, emptyEntityIdArray));

        // Write "VRTX."
        tempAddress = writer.getIndex();
        writer.jumpTemp(vertexAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(VERTEX_SIGNATURE);
        short vertexCount = (short) this.vertexes.size();
        writer.writeShort(vertexCount);
        writer.writeShort((short) 0); // Padding.
        getVertexes().forEach(vertex -> vertex.saveWithPadding(writer));

        // Save GRID data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(gridAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GRID_SIGNATURE);
        writer.writeShort(this.gridXCount);
        writer.writeShort(this.gridZCount);
        writer.writeShort(this.gridXLength);
        writer.writeShort(this.gridZLength);

        getGridStacks().forEach(gridStack -> gridStack.save(writer));
        getGridSquares().forEach(square -> square.save(writer));

        // Save "ANIM" data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(animAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ANIMATION_SIGNATURE);
        writer.writeInt(this.mapAnimations.size());
        writer.writeInt(writer.getIndex() + Constants.POINTER_SIZE);
        getMapAnimations().forEach(anim -> anim.save(writer));
        getMapAnimations().forEach(anim -> anim.writeTextures(writer));
        writer.writeShort(MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
        getMapAnimations().forEach(anim -> anim.writeMapUVs(writer));
    }

    @Override
    public void exportAlternateFormat(FileEntry entry) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select the folder to export this map into.");
        dirChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFolder = dirChooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return;

        FileChooser exeChooser = new FileChooser();
        exeChooser.setTitle("Select the Frogger executable with the remap table.");
        exeChooser.getExtensionFilters().add(new ExtensionFilter("Frogger PC Executable", "*.exe"));
        exeChooser.getExtensionFilters().add(new ExtensionFilter("Frogger PSX Executable", "*.01", "*.02", "*.03", "*.04", "*.05", "*.06", "*.07", "*.08", "*.09"));
        exeChooser.setInitialDirectory(GUIMain.getWorkingDirectory());
        exeChooser.setInitialFileName("frogger.exe");

        List<VLOArchive> allVLOs = getParentMWD().getFiles().stream()
                .filter(VLOArchive.class::isInstance)
                .map(VLOArchive.class::cast)
                .collect(Collectors.toList());
        allVLOs.add(0, null);

        SelectionMenu.promptSelection("Please select the VLO associated with this map.", vlo -> {
            boolean hasTextures = vlo != null;

            File selectedExe = null;
            if (hasTextures) {
                selectedExe = exeChooser.showOpenDialog(GUIMain.MAIN_STAGE);
                if (selectedExe == null)
                    return;
            }

            if (hasTextures)
                vlo.exportAllImages(selectedFolder, true, true, true); // Export VLO images.

            String cleanName = entry.getDisplayName().split("\\.")[0];
            exportToObj(selectedFolder, cleanName, entry, vlo, hasTextures ? GUIMain.EXE_CONFIG.getRemapTable(cleanName, selectedExe) : null);
        }, allVLOs, vlo -> vlo != null ? parentMWD.getEntryMap().get(vlo).getDisplayName() : "No Textures", vlo -> vlo == null ? null :
                new ImageView(SwingFXUtils.toFXImage(Utils.resizeImage(vlo.getImages().get(0).toBufferedImage(false, false, false), 25, 25), null)));
    }

    @SneakyThrows
    private void exportToObj(File directory, String cleanName, FileEntry entry, VLOArchive vloArchive, List<Short> remapTable) {
        updatePolygonCache();
        boolean exportTextures = vloArchive != null;

        System.out.println("Exporting " + cleanName + ".");

        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(directory, cleanName + ".obj"));

        objWriter.write("# FrogLord Map Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime().toString() + Constants.NEWLINE);
        objWriter.write("# Map Name: " + entry.getDisplayName() + Constants.NEWLINE);
        objWriter.write(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.write("mtllib " + mtlName + Constants.NEWLINE);
            objWriter.write(Constants.NEWLINE);
        }

        // Write Vertexes.
        getVertexes().forEach(vertex -> objWriter.write(vertex.toOBJString() + Constants.NEWLINE));
        objWriter.write(Constants.NEWLINE);

        // Write Faces.
        List<PSXPolygon> allPolygons = new ArrayList<>();
        getCachedPolygons().values().forEach(ply -> ply.stream().map(PSXPolygon.class::cast).forEach(allPolygons::add));

        // Register textures.
        if (exportTextures) {
            allPolygons.sort(Comparator.comparingInt(PSXPolygon::getOrderId));
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            for (PSXPolygon poly : allPolygons)
                if (poly instanceof PSXPolyTexture)
                    for (ByteUV uvs : ((PSXPolyTexture) poly).getUvs())
                        objWriter.write(uvs.toObjTextureString() + Constants.NEWLINE);

            objWriter.write(Constants.NEWLINE);
        }

        objWriter.write("# Faces" + Constants.NEWLINE);

        AtomicInteger textureId = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger counter = new AtomicInteger();

        Map<Integer, GameImage> textureMap = new HashMap<>();
        List<PSXColorVector> faceColors = new ArrayList<>();
        Map<PSXColorVector, List<PSXPolygon>> facesWithColors = new HashMap<>();

        AtomicInteger maxUsedRemap = new AtomicInteger(Integer.MIN_VALUE);
        allPolygons.forEach(polygon -> {
            if (polygon instanceof PSXPolyTexture) {
                PSXPolyTexture texture = (PSXPolyTexture) polygon;

                if (exportTextures) {
                    int newTextureId = texture.getTextureId();

                    if (remapTable != null) { // Apply remap.
                        GameImage image = textureMap.computeIfAbsent(newTextureId, key -> {
                            int remapTextureId = remapTable.get(key);
                            if (key > maxUsedRemap.get())
                                maxUsedRemap.set(key);

                            for (GameImage testImage : vloArchive.getImages())
                                if (testImage.getTextureId() == remapTextureId)
                                    return testImage;
                            throw new RuntimeException("Could not find a texture with the id: " + remapTextureId + ".");
                        });
                        newTextureId = image.getTextureId();
                    }

                    if (newTextureId != textureId.get()) { // It's time to change the texture.
                        textureId.set(newTextureId);
                        objWriter.write(Constants.NEWLINE);
                        objWriter.write("usemtl tex" + newTextureId + Constants.NEWLINE);
                    }
                }

                objWriter.write(polygon.toObjFaceCommand(exportTextures, counter) + Constants.NEWLINE);
            } else {
                PSXColorVector color = (polygon instanceof PSXPolyFlat) ? ((PSXPolyFlat) polygon).getColor() : ((PSXPolyGouraud) polygon).getColors()[0];
                if (!faceColors.contains(color))
                    faceColors.add(color);
                facesWithColors.computeIfAbsent(color, key -> new ArrayList<>()).add(polygon);
            }
        });

        objWriter.append(Constants.NEWLINE);
        objWriter.append("# Faces without textures.").append(Constants.NEWLINE);
        for (Entry<PSXColorVector, List<PSXPolygon>> mapEntry : facesWithColors.entrySet()) {
            objWriter.write("usemtl color" + faceColors.indexOf(mapEntry.getKey()) + Constants.NEWLINE);
            mapEntry.getValue().forEach(poly -> objWriter.write(poly.toObjFaceCommand(exportTextures, null) + Constants.NEWLINE));
        }


        // Write MTL file.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(directory, mtlName));

            for (GameImage image : textureMap.values()) {
                mtlWriter.write("newmtl tex" + image.getTextureId() + Constants.NEWLINE);
                mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
                // "d 0.75" = Partially transparent, if we want to support this later.
                mtlWriter.write("map_Kd " + vloArchive.getImages().indexOf(image) + ".png" + Constants.NEWLINE);
                mtlWriter.write(Constants.NEWLINE);
            }

            for (int i = 0; i < faceColors.size(); i++) {
                PSXColorVector color = faceColors.get(i);
                mtlWriter.write("newmtl color" + i + Constants.NEWLINE);
                if (i == 0)
                    mtlWriter.write("d 1" + Constants.NEWLINE); // All further textures should be completely solid.
                mtlWriter.write("Kd " + Utils.unsignedByteToFloat(color.getRed()) + " " + Utils.unsignedByteToFloat(color.getGreen()) + " " + Utils.unsignedByteToFloat(color.getBlue()) + Constants.NEWLINE); // Diffuse color.
                mtlWriter.write(Constants.NEWLINE);
            }
        }

        System.out.println("Export complete.");

        int maxRemap = maxUsedRemap.get() + 1;
        if (exportTextures && remapTable.size() > maxRemap)
            System.out.println("This remap is probably bigger than it needs to be. It can be size " + maxRemap + ".");
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new MAPController(), "map", this);
    }

    /**
     * Rebuild the cache of all polygons.
     */
    public void updatePolygonCache() {
        cachedPolygons.clear();
        addPolygons(getLoosePolygons());
        getGroups().forEach(group -> addPolygons(group.getPolygonMap()));
    }

    private void addPolygons(Map<PSXPrimitiveType, List<PSXGPUPrimitive>> add) {
        for (Entry<PSXPrimitiveType, List<PSXGPUPrimitive>> entry : add.entrySet())
            cachedPolygons.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
    }

    private void printInvalidEntityReadDetection(Entity lastEntity, int endPointer) {
        if (lastEntity == null)
            return;
        int realSize = (endPointer - lastEntity.getLoadScriptDataPointer());
        if (realSize != lastEntity.getLoadReadLength())
            System.out.println("[INVALID/" + MWDFile.CURRENT_FILE_NAME + "] Entity " + getEntities().indexOf(lastEntity) + "/" + Integer.toHexString(lastEntity.getLoadScriptDataPointer()) + " REAL: " + realSize + ", READ: " + lastEntity.getLoadReadLength() + ", " + lastEntity.getFormBook());
    }

    /**
     * This method fixes this MAP (If it is ISLAND.MAP) so it will load properly.
     */
    public void fixDEV1() {
        removeEntity(getEntities().get(11)); // Remove corrupted butterfly entity.

        // Remove "SUB_PEDDLEBOAT" entities. These entities do not exist.
        removeEntity(getEntities().get(7));
        removeEntity(getEntities().get(5));
        removeEntity(getEntities().get(3));
        removeEntity(getEntities().get(2));

        //TODO: Remap
    }

    /**
     * Write an entity list.
     * @param writer          The writer to write the list to.
     * @param entities        The list of entities to include.
     * @param pointerLocation A pointer to an integer which holds the pointer to the entity list.
     */
    public void writeEntityList(DataWriter writer, List<Entity> entities, int pointerLocation) {
        if (entities.isEmpty())
            return;

        Utils.verify(pointerLocation > 0, "Entity pointer location is not set!");

        int tempAddress = writer.getIndex();
        writer.jumpTemp(pointerLocation);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        for (Entity entity : entities) {
            int entityId = getEntities().indexOf(entity);
            Utils.verify(entityId >= 0, "Tried to save a reference to an entity which is not tracked by the map!");
            writer.writeUnsignedShort(entityId);
        }

        writer.writeShort(MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
    }
}
