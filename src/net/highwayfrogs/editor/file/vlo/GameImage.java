package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singular game image. MR_TXSETUP struct.
 * TODO: Fix some PSX images having a slight offset.
 * TODO: Support saving PSX VLOs.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
@Setter
@SuppressWarnings("unused")
public class GameImage extends GameObject {
    private VLOArchive parent;
    private short vramX;
    private short vramY;
    private short fullWidth;
    private short fullHeight;
    private short textureId;
    private short texturePage;
    private short flags;
    private short clutId; // Believed to always be either 0 or 37.
    private byte u; // Unsure. Texture orientation?
    private byte v;
    private byte ingameWidth; // In-game texture width, used to remove texture padding.
    private byte ingameHeight;
    private byte[] imageBytes;

    private AtomicInteger suppliedTextureOffset;

    private static final int MAX_DIMENSION = 256;
    private static final int PIXEL_BYTES = 4;

    public static int PACK_ID = 0;
    public static int IMAGE_ID = 0;

    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ROTATED = 2; // Unused.
    public static final int FLAG_HIT_X = 4; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = 8; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = 16; // Unsure.
    public static final int FLAG_BLACK_IS_TRANSPARENT = 32; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = 32768;

    public GameImage(VLOArchive parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.vramX = reader.readShort();
        this.vramY = reader.readShort();
        this.fullWidth = reader.readShort();
        this.fullHeight = reader.readShort();

        int offset = reader.readInt();
        this.textureId = reader.readShort();
        this.texturePage = reader.readShort();
        this.clutId = reader.readShort();
        this.flags = reader.readShort();
        this.u = reader.readByte();
        this.v = reader.readByte();
        this.ingameWidth = reader.readByte();
        this.ingameHeight = reader.readByte();

        reader.jumpTemp(offset);

        int pixelCount = getFullWidth() * getFullHeight();
        if (getParent().isPsxMode()) {
            pixelCount *= 2;

            ByteBuffer buffer = ByteBuffer.allocate(2 * PIXEL_BYTES * pixelCount);

            int clutX = ((clutId & 0x3F) << 4);
            int clutY = (clutId >> 6);

            ClutEntry clut = getParent().getClutEntries().stream()
                    .filter(entry -> entry.getClutRect().getX() == clutX)
                    .filter(entry -> entry.getClutRect().getY() == clutY)
                    .findAny().orElse(null);

            Utils.verify(clut != null, "Failed to find clut for coordinates [%d,%d].", clutX, clutY);

            for (int i = 0; i < pixelCount; i++) {
                short value = reader.readUnsignedByteAsShort();
                int low = value & 0x0F;
                int high = value >> 4;

                readPSXPixel(low, clut, buffer);
                readPSXPixel(high, clut, buffer);
            }

            this.imageBytes = buffer.array();

            try {
                File dir = new File("debug/" + PACK_ID + "/");
                if (!dir.exists())
                    dir.mkdirs();

                ImageIO.write(toBufferedImage(false), "png", new File(dir, IMAGE_ID++ + ".png"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
            this.imageBytes = reader.readBytes(pixelCount * PIXEL_BYTES);
        }

        reader.jumpReturn();
    }

    private void readPSXPixel(int clutIndex, ClutEntry clut, ByteBuffer buffer) {
        PSXClutColor color = clut.getColors().get(clutIndex);

        byte[] arr = new byte[4]; //RGBA
        arr[0] = Utils.unsignedShortToByte(color.getUnsignedScaledRed());
        arr[1] = Utils.unsignedShortToByte(color.getUnsignedScaledGreen());
        arr[2] = Utils.unsignedShortToByte(color.getUnsignedScaledBlue());
        arr[3] = (byte) (0xFF - color.getAlpha(false));
        buffer.putInt(Utils.readNumberFromBytes(arr));
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(this.suppliedTextureOffset != null, "Image data offset was not specified.");

        writer.writeShort(this.vramX);
        writer.writeShort(this.vramY);
        writer.writeShort(this.fullWidth);
        writer.writeShort(this.fullHeight);
        writer.writeInt(this.suppliedTextureOffset.get());
        writer.writeShort(this.textureId);
        writer.writeShort(this.texturePage);
        writer.writeShort(this.clutId);
        writer.writeShort(this.flags);
        writer.writeByte(this.u);
        writer.writeByte(this.v);
        writer.writeByte(this.ingameWidth);
        writer.writeByte(this.ingameHeight);

        byte[] savedImageBytes = getSavedImageBytes();
        int writeOffset = this.suppliedTextureOffset.getAndAdd(savedImageBytes.length); // Add offset.

        writer.jumpTemp(writeOffset);
        writer.writeBytes(savedImageBytes);
        writer.jumpReturn();
    }

    public void save(DataWriter writer, AtomicInteger textureOffset) {
        this.suppliedTextureOffset = textureOffset;
        save(writer);
        this.suppliedTextureOffset = null;
    }

    public byte[] getSavedImageBytes() {
        if (!getParent().isPsxMode())
            return getImageBytes(); // The image bytes as they are loaded are already as they should be when saved.

        throw new RuntimeException("PSX not supported yet."); //TODO: Remove once supported.
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) { // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
            BufferedImage sourceImage = image;
            image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = image.getGraphics();
            graphics.drawImage(sourceImage, 0, 0, null);
            graphics.dispose();
        }

        this.fullWidth = (short) image.getWidth();
        this.fullHeight = (short) image.getHeight();
        setIngameWidth(this.fullWidth);
        setIngameHeight(this.fullHeight);

        // Read image rgba data.
        int[] array = new int[getFullHeight() * getFullWidth()];
        image.getRGB(0, 0, getFullWidth(), getFullHeight(), array, 0, getFullWidth());

        //Convert int array into byte array.
        ByteBuffer buffer = ByteBuffer.allocate(array.length * Constants.INTEGER_SIZE);
        buffer.asIntBuffer().put(array);
        byte[] bytes = buffer.array();

        // Convert BGRA -> ABGR, and write the new image bytes.
        this.imageBytes = bytes; // Override existing image.
        for (int i = 0; i < bytes.length; i += PIXEL_BYTES) { // Load image bytes.
            this.imageBytes[i] = (byte) (0xFF - this.imageBytes[i]); // Flip alpha.
            byte temp = this.imageBytes[i + 1];
            this.imageBytes[i + 1] = this.imageBytes[i + 3];
            this.imageBytes[i + 3] = temp;
        }
    }

    /**
     * Export this game image as a BufferedImage.
     * @param trimEdges Should edges be trimmed so the textures are exactly how they appear in-game?
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(boolean trimEdges) { //TODO: Actually make trimming work.
        int height = trimEdges ? getIngameHeight() : getFullHeight();
        int width = trimEdges ? getIngameWidth() : getFullWidth();
        if (parent.isPsxMode())
            width = width * 4;

        byte[] cloneBytes = Arrays.copyOf(getImageBytes(), getImageBytes().length); // We don't want to make changes to the original array.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        //ABGR -> BGRA
        for (int temp = 0; temp < cloneBytes.length; temp += PIXEL_BYTES) {
            byte alpha = cloneBytes[temp];
            int alphaIndex = temp + PIXEL_BYTES - 1;
            System.arraycopy(cloneBytes, temp + 1, cloneBytes, temp, alphaIndex - temp);
            cloneBytes[alphaIndex] = (byte) (0xFF - alpha); // Alpha needs to be flipped.
        }

        IntBuffer buffer = ByteBuffer.wrap(cloneBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer();

        int[] array = new int[buffer.remaining()];
        buffer.get(array);
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), array, 0, image.getWidth());
        return image;
    }

    /**
     * Gets the in-game height of this image.
     * @return ingameHeight
     */
    public int getIngameHeight() {
        return this.ingameHeight == 0 ? MAX_DIMENSION : this.ingameHeight;
    }

    /**
     * Gets the in-game width of this image.
     * @return ingameWidth
     */
    public int getIngameWidth() {
        return this.ingameWidth == 0 ? MAX_DIMENSION : this.ingameWidth;
    }

    /**
     * Set the in-game height of this image.
     * @param height The in-game height.
     */
    public void setIngameHeight(int height) {
        Utils.verify(height >= 0 && height <= MAX_DIMENSION, "Image height is not in the required range (0,%d].", MAX_DIMENSION);
        this.ingameHeight = (byte) (height == MAX_DIMENSION ? 0 : height);
    }

    /**
     * Set the in-game width of this image.
     * @param width The in-game width.
     */
    public void setIngameWidth(int width) {
        Utils.verify(width >= 0 && width <= MAX_DIMENSION, "Image width is not in the required range: (0,%d].", MAX_DIMENSION);
        this.ingameWidth = (byte) (width == MAX_DIMENSION ? 0 : width);
    }
}
