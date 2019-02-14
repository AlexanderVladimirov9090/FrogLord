package net.highwayfrogs.editor.file.mof;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a MOF file.
 * Some files are off by 4-16 bytes. This is caused by us merging prim banks of matching types, meaning the header that was there before is removed.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFFile extends GameFile {
    private boolean dummy; // Is this dummied data?
    private MOFAnimation animation; // Animation data. For some reason they thought it'd be a good idea to make MOF have two different data structures.
    private byte[] signature;
    private byte[] bytes;
    private int flags;
    private int extra;
    private List<MOFPart> parts = new ArrayList<>();
    private int unknownValue;
    @Setter private boolean incompleteMOF; // Some mofs are changed at run-time to share information. This attempts to handle that.

    public static final int FLAG_OFFSETS_RESOLVED = Constants.BIT_FLAG_0; // Fairly sure this is applied by frogger.exe runtime, and not something that should be true in the MWD. (Verify though.)
    public static final int FLAG_SIZES_RESOLVED = Constants.BIT_FLAG_1; // Like before, this is likely frogger.exe run-time only. But, we should confirm that.
    public static final int FLAG_TEXTURES_RESOLVED = Constants.BIT_FLAG_2; // Again.
    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.
    public static final int FLAG_ANIMATED_POLY_FILE = Constants.BIT_FLAG_4; // MOF contains some animated textured polys. What's the difference between this and the normal animation MOF?
    public static final int FLAG_FLIPBOOK_FILE = Constants.BIT_FLAG_5; // Static flipbook file. (What does this mean?)

    public static final int FLAG_ANIM_TRANSFORMS_INDEXED = Constants.BIT_FLAG_16; // Appears like the only thing this is used for is making sure it's present. Otherwise, the game will crash.
    public static final int FLAG_ANIM_INDEXED_TRANSFORMS_IN_PARTS = Constants.BIT_FLAG_17; // I believe this should always be false.

    public static final int MOF_ID = 3;
    public static final int MAP_MOF_ID = 4;

    private static final Image ICON = loadIcon("swampy");
    private static final byte[] DUMMY_DATA = "DUMY".getBytes();
    private static final int COMPLETE_TEST = 0x40;

    public static final ImageFilterSettings MOF_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    @Override
    public void load(DataReader reader) {
        this.signature = reader.readBytes(4);
        if (Arrays.equals(DUMMY_DATA, getSignature())) {
            this.dummy = true;
            return;
        }

        reader.skipInt(); // File length, including header.
        this.flags = reader.readInt();

        if (testFlag(FLAG_ANIMATION_FILE)) {
            resolveAnimatedMOF(reader);
        } else {
            resolveStaticMOF(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (dummy) {
            writer.writeBytes(DUMMY_DATA);
            return;
        }

        if (this.bytes != null && isIncompleteMOF()) {
            writer.writeBytes(bytes);
            return;
        }

        writer.writeBytes(getSignature());
        int fileSizePointer = writer.writeNullPointer(); // Optional, but might as well.
        writer.writeInt(this.flags);

        if (animation != null) { // If this is an animation, save the animation.
            animation.save(writer);
            return;
        }

        writer.writeInt(this.extra);
        getParts().forEach(part -> part.save(writer));
        writer.writeInt(this.unknownValue);
        getParts().forEach(part -> part.saveExtra(writer));
        writer.writeAddressTo(fileSizePointer);
    }

    private void resolveStaticMOF(DataReader reader) {
        this.extra = reader.readInt();
        int partCount = this.extra;

        reader.jumpTemp(COMPLETE_TEST);
        this.incompleteMOF = (reader.readInt() == 0);
        reader.jumpReturn();

        if (isIncompleteMOF()) { // Just copy the MOF directly.
            reader.setIndex(0);
            this.bytes = reader.readBytes(reader.getRemaining());
            return;
        }

        for (int i = 0; i < partCount; i++) {
            MOFPart part = new MOFPart(this);
            part.load(reader);
            parts.add(part);
        }

        this.unknownValue = reader.readInt();
    }

    private void resolveAnimatedMOF(DataReader reader) {
        this.animation = new MOFAnimation(this);
        this.animation.load(reader);
    }

    /**
     * Export this object to wavefront obj.
     * Not the cleanest thing in the world, but it doesn't need to be.
     */
    @SneakyThrows
    public void exportObject(FileEntry entry, File folder, VLOArchive vloTable, String cleanName) {
        if (isDummy()) {
            System.out.println("Cannot export dummy MOF.");
            return;
        }

        if (isIncompleteMOF()) {
            System.out.println("Cannot export incomplete MOF.");
            return;
        }

        if (testFlag(FLAG_ANIMATION_FILE)) {
            this.animation.getStaticMOF().exportObject(entry, folder, vloTable, cleanName);
            return;
        }

        boolean exportTextures = vloTable != null;


        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(folder, cleanName + ".obj"));

        objWriter.write("# FrogLord MOF Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime().toString() + Constants.NEWLINE);
        objWriter.write("# MOF Name: " + entry.getDisplayName() + Constants.NEWLINE);
        objWriter.write(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.write("mtllib " + mtlName + Constants.NEWLINE);
            objWriter.write(Constants.NEWLINE);
        }

        // Write Vertices.
        int verticeStart = 0;

        int partCount = 0;
        for (MOFPart part : getParts()) {
            part.setTempVertexStart(verticeStart);
            objWriter.write("# Part " + (partCount++) + ":" + Constants.NEWLINE);
            MOFPartcel partcel = part.getPartcels().get(0); // 0 is the animation frame.
            verticeStart += partcel.getVertices().size();

            for (SVector vertex : partcel.getVertices())
                objWriter.write(vertex.toOBJString() + Constants.NEWLINE);
        }

        objWriter.write(Constants.NEWLINE);

        // Write Faces.
        Map<MOFPolygon, MOFPart> ownerMap = new HashMap<>();
        List<MOFPolygon> allPolygons = new ArrayList<>();
        getParts().forEach(part -> part.getMofPolygons().values().forEach(polys -> {
            allPolygons.addAll(polys);
            for (MOFPolygon poly : polys)
                ownerMap.put(poly, part);
        }));

        // Register textures.
        if (exportTextures) {
            allPolygons.sort(Comparator.comparingInt(MOFPolygon::getOrderId));
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            for (MOFPolygon poly : allPolygons) {
                if (poly instanceof MOFPolyTexture) {
                    MOFPolyTexture mofTex = (MOFPolyTexture) poly;
                    for (int i = mofTex.getUvs().length - 1; i >= 0; i--)
                        objWriter.write(mofTex.getObjUVString(i) + Constants.NEWLINE);
                }
            }

        }

        objWriter.write("# Faces" + Constants.NEWLINE);

        AtomicInteger textureId = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger counter = new AtomicInteger();

        Map<Integer, GameImage> textureMap = new HashMap<>();
        List<PSXColorVector> faceColors = new ArrayList<>();
        Map<PSXColorVector, List<MOFPolygon>> facesWithColors = new HashMap<>();

        allPolygons.forEach(polygon -> {
            if (!(polygon instanceof MOFPolyTexture)) {
                PSXColorVector color = polygon.getColor();
                if (!faceColors.contains(color))
                    faceColors.add(color);
                facesWithColors.computeIfAbsent(color, key -> new ArrayList<>()).add(polygon);
            } else {
                MOFPolyTexture texture = (MOFPolyTexture) polygon;

                if (exportTextures) {
                    int newTextureId = texture.getImageId();

                    GameImage image = textureMap.computeIfAbsent(newTextureId, key -> {
                        for (GameImage testImage : vloTable.getImages())
                            if (testImage.getTextureId() == texture.getImageId())
                                return testImage;

                        throw new RuntimeException("Failed to find: " + texture.getImageId());
                    });
                    newTextureId = image.getTextureId();

                    if (newTextureId != textureId.get()) { // It's time to change the texture.
                        textureId.set(newTextureId);
                        objWriter.write(Constants.NEWLINE);
                        objWriter.write("usemtl tex" + newTextureId + Constants.NEWLINE);
                    }
                }

                objWriter.write(polygon.toObjFaceCommand(exportTextures, counter, ownerMap.get(polygon)) + Constants.NEWLINE);
            }
        });

        objWriter.append(Constants.NEWLINE);
        objWriter.append("# Faces without textures.").append(Constants.NEWLINE);
        for (Entry<PSXColorVector, List<MOFPolygon>> mapEntry : facesWithColors.entrySet()) {
            objWriter.write("usemtl color" + faceColors.indexOf(mapEntry.getKey()) + Constants.NEWLINE);
            mapEntry.getValue().forEach(poly -> objWriter.write(poly.toObjFaceCommand(exportTextures, null, ownerMap.get(poly)) + Constants.NEWLINE));
        }


        // Write MTL file.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(folder, mtlName));

            for (GameImage image : textureMap.values()) {
                mtlWriter.write("newmtl tex" + image.getTextureId() + Constants.NEWLINE);
                mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
                // "d 0.75" = Partially transparent, if we want to support this later.
                mtlWriter.write("map_Kd " + vloTable.getImages().indexOf(image) + ".png" + Constants.NEWLINE);
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

        System.out.println("MOF Exported.");
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return flagPresent
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }
}
