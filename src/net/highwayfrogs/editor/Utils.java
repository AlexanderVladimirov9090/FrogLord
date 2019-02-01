package net.highwayfrogs.editor;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.stage.Window;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.gui.GUIMain;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.CRC32;

/**
 * Some static utilities.
 * Created by Kneesnap on 8/12/2018.
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused"})
public class Utils {
    private static final ByteBuffer INT_BUFFER = ByteBuffer.allocate(Constants.INTEGER_SIZE);
    private static final CRC32 crc32 = new CRC32();
    private static final File[] EMPTY_FILE_ARRAY = new File[0];
    private static final Map<BufferedImage, TextureCache> imageCacheMap = new HashMap<>();
    private static final long IMAGE_CACHE_EXPIRE = TimeUnit.MINUTES.toMillis(5);

    /**
     * Convert a byte array to a number.
     * @param data The data to turn into a number.
     * @return intValue
     */
    public static int readNumberFromBytes(byte[] data) {
        int value = 0;
        for (int i = 0; i < data.length; i++)
            value += ((long) data[i] & 0xFFL) << (Constants.BITS_PER_BYTE * i);
        return value;
    }

    /**
     * Turn an integer into a byte array.
     * @param value The integer to turn into a byte array.
     * @return byteArray
     */
    public static byte[] toByteArray(int value) {
        INT_BUFFER.clear();
        return INT_BUFFER.order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    /**
     * Turn a byte array into a readable byte string.
     * @param array The array to convert into a string.
     * @return byteString
     */
    public static String toByteString(byte[] array) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(toByteString(array[i]));
        }
        return sb.append("}").toString();
    }

    /**
     * Turn a single byte into a hex string.
     * @param value The byte to convert.
     * @return byteStr
     */
    public static String toByteString(byte value) {
        String str = String.format("%x", value);
        if (str.length() == 1)
            str = "0" + str;
        return str;
    }

    /**
     * Verify a condition is true, otherwise throw an exception.
     * @param condition  The condition to verify is true.
     * @param error      The error message if false.
     * @param formatting Formatting to apply to the error message.
     */
    public static void verify(boolean condition, String error, Object... formatting) {
        if (!condition)
            throw new RuntimeException(formatting.length > 0 ? String.format(error, formatting) : error);
    }

    /**
     * Converts a number to a bit array.
     * Example Input:
     * value = 00101100
     * bitCount = 6
     * Output: 101100 or {1, 0, 1, 1, 0, 0}
     * @param value    The value to get bits from.
     * @param bitCount The amount
     * @return bits
     */
    public static int[] getBits(int value, int bitCount) {
        int[] bits = new int[bitCount];
        for (int i = 0; i < bits.length; i++)
            bits[bits.length - i - 1] = (value >> i) & Constants.BIT_TRUE;
        return bits;
    }

    /**
     * Reverse the order of elements in an array.
     * @param array The array to swap bits of.
     * @return sameArray
     */
    public static <T> T[] reverse(T[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            T temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
        return array;
    }

    /**
     * Flip a bit.
     * @param bit The bit to flip.
     * @return flippedBit
     */
    public static int flipBit(int bit) {
        return bit == Constants.BIT_TRUE ? Constants.BIT_FALSE : Constants.BIT_TRUE;
    }

    /**
     * Turn a list of bytes into a byte array.
     * @param list The list of bytes.
     * @return byteArray
     */
    public static byte[] toArray(List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++)
            bytes[i] = list.get(i);
        return bytes;
    }

    /**
     * Convert an unsigned byte into a float 0-1.
     * @param unsignedByte The byte to convert to a float. 0x00 = 0.00. 0xFF = 1.000
     * @return floatValue
     */
    public static float unsignedByteToFloat(byte unsignedByte) {
        float num = (float) unsignedByte;
        if (num < 0)
            num += 256;
        return num / 0xFF;
    }

    /**
     * Convert an unsigned short (in fixed point representation) into a float.
     * @param unsignedShort The short to convert.
     * @return floatValue
     */
    public static float unsignedShortToFloat(short unsignedShort) {
        return ((float)unsignedShort / (float)(1 << 4));
    }

    /**
     * Convert an unsigned integer (in fixed point representation) into a float.
     * @param unsignedInt The integer to convert.
     * @return floatValue
     */
    public static float unsignedIntToFloat(long unsignedInt) {
        return ((float)unsignedInt / (float)(1 << 20));
    }

    /**
     * Convert an unsigned short value (fixed point, n fractional bits) into a float.
     * @param unsignedShort The short to convert.
     * @param n The number of fractional bits.
     * @return floatValue
     */
    public static float unsignedShortToFloatNBits(short unsignedShort, long n) {
        return ((float)unsignedShort / (float)(1 << n));
    }

    /**
     * Convert an unsigned int value (fixed point, n fractional bits) into a float.
     * @param unsignedInt The integer to convert.
     * @param n The number of fractional bits.
     * @return floatValue
     */
    public static float unsignedIntFloatNBits(long unsignedInt, long n) {
        return ((float)unsignedInt / (float)(1 << n));
    }

    /**
     * Convert an unsigned byte into a short, which can be converted back into a byte.
     * @param unsignedByte The byte to convert into a short.
     * @return unsignedShort
     */
    public static short byteToUnsignedShort(byte unsignedByte) {
        short num = (short) unsignedByte;
        if (num < 0)
            num += 256;
        return num;
    }

    /**
     * Convert an unsigned short back into a unsigned byte.
     * @param unsignedShort the short to turn back into a byte.
     * @return byte
     */
    public static byte unsignedShortToByte(short unsignedShort) {
        if (unsignedShort < 0 || unsignedShort > 0xFF)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,255]. Value: " + unsignedShort);
        return (byte) unsignedShort;
    }

    /**
     * Convert an unsigned short into an int, which can be converted back into a short.
     * @param unsignedShort The short to convert into a int.
     * @return unsignedShort
     */
    public static int shortToUnsignedInt(short unsignedShort) {
        int num = (int) unsignedShort;
        if (num < 0)
            num += 65536;
        return num;
    }

    /**
     * Convert an unsigned int back into a unsigned short.
     * @param unsignedInt the int to turn back into a short.
     * @return byte
     */
    public static short unsignedIntToShort(int unsignedInt) {
        if (unsignedInt < 0 || unsignedInt > 0xFFFF)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,65535]. Value: " + unsignedInt);
        return (short) unsignedInt;
    }

    /**
     * Convert an unsigned int into an long, which can be converted back into a long.
     * @param unsignedInt The int to convert into a long.
     * @return unsignedLong
     */
    public static long intToUnsignedLong(int unsignedInt) {
        long num = (long) unsignedInt;
        if (num < 0) {
            num += Integer.MAX_VALUE;
            num++;
        }
        return num;
    }

    /**
     * Convert an unsigned long back into a unsigned int.
     * @param unsignedLong the long to turn back into an int.
     * @return int
     */
    public static int unsignedLongToInt(long unsignedLong) {
        if (unsignedLong < 0 || unsignedLong > 0xFFFFFFFFL)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,0xFFFFFFFF]. Value: " + unsignedLong);
        return (int) unsignedLong;
    }

    /**
     * Reverse an array, and store it in a new array, which is returned.
     * @param array The array to reverse.
     * @return clonedArray
     */
    public static byte[] reverseCloneByteArray(byte[] array) {
        byte[] output = Arrays.copyOf(array, array.length);
        reverseByteArray(output);
        return output;
    }

    /**
     * Reverse a byte array.
     * @param array The array to reverse
     */
    public static void reverseByteArray(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) { // Reverse the byte order.
            byte temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }

    /**
     * Get a resource in the JAR.
     * @param resourceName The resource name.
     * @return resourceURL
     */
    public static URL getResource(String resourceName) {
        return Utils.class.getClassLoader().getResource(resourceName);
    }

    /**
     * Get a JAR resource as a stream.
     * @param resourceName The name of the resource to load.
     * @return resourceStream
     */
    public static InputStream getResourceStream(String resourceName) {
        return Utils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    /**
     * Get the first file with this name that does not exist. Appends stuff like (1).
     * @param file The file to get.
     * @return nonexistantFile.
     */
    public static File getNonExistantFile(File file) {
        if (!file.exists())
            return file;

        int id = 0;

        File result = file;
        while (result.exists())
            result = getFile(file, ++id);

        return result;
    }

    private static File getFile(File file, int id) {
        if (id == 0)
            return file;

        String fileName = file.getName();
        String name = fileName.replaceFirst("[.][^.]+$", ""); // Remove extension.
        name += " (" + id + ")";
        if (fileName.contains("."))
            name += fileName.substring(fileName.lastIndexOf(".") + 1);

        return new File(file.getParentFile(), name);
    }

    /**
     * Resizes an image.
     * @param img    The image to resize.
     * @param width  The new width.
     * @param height The new height.
     * @return resized
     */
    public static BufferedImage resizeImage(BufferedImage img, int width, int height) {
        BufferedImage newImage = new BufferedImage(width, height, img.getType());
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        return newImage;
    }

    /**
     * Raise x to the y. Math.pow is pretty intensive, so we do this instead.
     * @param x The base.
     * @param y The exponent.
     * @return value
     */
    public static int power(int x, int y) {
        int result = 1;
        while (y-- > 0)
            result *= x;
        return result;
    }

    /**
     * Get a byte value unsigned, as an integer.
     * @param value The value to turn into an integer.
     * @return unsignedInt
     */
    public static int getUnsignedByte(byte value) {
        return value >= 0 ? value : (int) Byte.MAX_VALUE - value;
    }

    /**
     * Strip the extension from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripExtension(String name) {
        return name.split("\\.")[0];
    }

    /**
     * Deletes a file.
     * @param file The file to delete.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFile(File file) {
        if (file.exists())
            file.delete();
    }

    /**
     * Get the CRC32 hash of a file.
     * @param file The file to get the hash of.
     * @return crc32Hash
     */
    public static long getCRC32(File file) {
        crc32.reset();

        try {
            crc32.update(Files.readAllBytes(file.toPath()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return crc32.getValue();
    }

    /**
     * Test if a string is an integer.
     * @param str The string to test.
     * @return isInteger
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty() || str.equals("-"))
            return false;

        int startIndex = str.startsWith("-") ? 1 : 0;
        for (int i = startIndex; i < str.length(); i++)
            if (Character.digit(str.charAt(i), 10) < 0)
                return false;
        return true;
    }

    /**
     * Test if a string is a signed short.
     * @param str The string to test.
     * @return isSignedShort
     */
    public static boolean isSignedShort(String str) {
        if (!Utils.isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= Short.MIN_VALUE && intTest <= Short.MAX_VALUE;
    }

    /**
     * Test if a string is an unsigned byte.
     * @param str The string to test.
     * @return isUnsignedByte
     */
    public static boolean isUnsignedByte(String str) {
        if (!Utils.isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= 0 && intTest <= 0xFF;
    }

    /**
     * Prompt the user to select a file.
     * @param title      The title of the window to display.
     * @param typeInfo   The label to show for the file-type.
     * @param extensions Allowed extensions.
     * @return selectedFile, Can be null.
     */
    public static File promptFileOpenExtensions(String title, String typeInfo, String... extensions) {
        Utils.verify(extensions.length > 0, "");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        List<String> allExtensions = new ArrayList<>();
        for (String ext : extensions) {
            String type = "*." + ext; // Unix is case-sensitive, so we add both lower-case and upper-case.
            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                allExtensions.add(type);
            } else {
                allExtensions.add(lowerCase);
                allExtensions.add(upperCase);
            }
        }
        fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, allExtensions));

        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile != null)
            GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Prompt the user to select a file.
     * @param title     The title of the window to display.
     * @param typeInfo  The label to show for the file-type.
     * @param extension Allowed extension.
     * @return selectedFile, Can be null.
     */
    public static File promptFileOpen(String title, String typeInfo, String extension) {
        return promptFileOpenExtensions(title, typeInfo, extension);
    }

    /**
     * Prompt the user to save a file.
     * @param title       The title of the window to display.
     * @param suggestName The initial name to suggest saving the file as.
     * @param typeInfo    The label to show for the file-type.
     * @param extension   Allowed extension.
     * @return selectedFile, Can be null.
     */
    public static File promptFileSave(String title, String suggestName, String typeInfo, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (extension != null) {
            String type = "*." + extension; // Unix is case-sensitive, so we add both lower-case and upper-case.

            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, type));
            } else {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, lowerCase, upperCase));
            }
        }

        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());
        if (suggestName != null) {
            String initialName = suggestName;
            if (extension != null && !extension.equals("*"))
                initialName += "." + extension;

            fileChooser.setInitialFileName(initialName);
        }

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);
        if (selectedFile != null)
            GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Prompt the user to select a directory.
     * @param title         The title of the window.
     * @param saveDirectory Should this directory be saved as the current directory?
     * @return directoryFile
     */
    public static File promptChooseDirectory(String title, boolean saveDirectory) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder != null && saveDirectory)
            GUIMain.setWorkingDirectory(selectedFolder);

        return selectedFolder;
    }

    /**
     * A null-safe way of reading files from a directory.
     * @param directory The directory to read files from.
     * @return readFiles
     */
    public static File[] listFiles(File directory) {
        verify(directory.isDirectory(), "This is not a directory!");
        File[] files = directory.listFiles();
        return files != null ? files : EMPTY_FILE_ARRAY;
    }

    /**
     * Load a FXML template as a new window.
     * WARNING: This method is blocking.
     * @param template   The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param title      The title of the window to show.
     * @param controller Makes the window controller.
     */
    @SneakyThrows
    public static <T> void loadFXMLTemplate(String template, String title, Function<Stage, T> controller) {
        loadFXMLTemplate(template, title, controller, null);
    }

    /**
     * Gets the FXMLLoader by its name.
     * @param template The template name.
     * @return loader
     */
    public static FXMLLoader getFXMLLoader(String template) {
        return new FXMLLoader(getResource("javafx/" + template + ".fxml"));
    }

    /**
     * Load a FXML template as a new window.
     * WARNING: This method is blocking.
     * @param template   The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param title      The title of the window to show.
     * @param controller Makes the window controller.
     */
    @SneakyThrows
    public static <T> void loadFXMLTemplate(String template, String title, Function<Stage, T> controller, BiConsumer<Stage, T> consumer) {
        FXMLLoader loader = getFXMLLoader(template);

        Stage newStage = new Stage();
        newStage.setTitle(title);

        T controllerObject = controller.apply(newStage);
        loader.setController(controllerObject);

        Parent rootNode = loader.load();
        newStage.setScene(new Scene(rootNode));
        newStage.setResizable(false);

        if (consumer != null)
            consumer.accept(newStage, controllerObject);

        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.setAlwaysOnTop(true);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.getIcons().add(GUIMain.NORMAL_ICON);
        newStage.showAndWait();
    }

    /**
     * Make a given stage close when the escape key is pressed.
     * @param stage   The stage to apply.
     * @param onClose Behavior to run when the escape key is pressed.
     */
    public static void closeOnEscapeKey(Stage stage, Runnable onClose) {
        closeOnEscapeKey(stage, onClose, true);
    }

    private static void closeOnEscapeKey(Stage stage, Runnable onClose, boolean firstTime) {
        Scene scene = stage.getScene();
        if (scene == null || !Platform.isFxApplicationThread()) {
            if (firstTime)
                Platform.runLater(() -> closeOnEscapeKey(stage, onClose, false));
            return;
        }

        Utils.verify(scene.getOnKeyPressed() == null, "Scene already has a key-press listener!");
        scene.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ESCAPE) {
                if (onClose != null)
                    onClose.run();
                stage.close();
            }
        });
    }

    /**
     * Switch the Stage's scene without changing window size.
     * @param stage    The stage that should own the scene.
     * @param newScene The new scene to use.
     * @return oldScene
     */
    public static Scene setSceneKeepPosition(Stage stage, Scene newScene) {
        Scene oldScene = stage.getScene();

        Window oldWindow = oldScene.getWindow();
        double width = oldWindow.getWidth();
        double height = oldWindow.getHeight();
        double x = oldWindow.getX();
        double y = oldWindow.getY();

        stage.setScene(newScene); // Exit the viewer.

        // Maintain the position the viewer Scene was at when it was closed.
        Window newWindow = newScene.getWindow();
        newWindow.setX(x);
        newWindow.setY(y);
        newWindow.setWidth(width);
        newWindow.setHeight(height);

        return oldScene;
    }

    /**
     * Get the raw file name without an extension.
     * @param fileName The file name to get raw.
     * @return rawFileName
     */
    public static String getRawFileName(String fileName) {
        return stripExtension(fileName).toUpperCase();
    }

    /**
     * Turn an integer into a hex string.
     * 255 -> 0xFF
     * @param value The value to convert.
     * @return hexString
     */
    public static String toHexString(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /**
     * Get a Color object from an integer.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromRGB(int rgb) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Get a Color object from an integer.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromBGR(int rgb) {
        return Color.rgb(rgb & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 16) & 0xFF);
    }

    /**
     * Get a integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toRGB(Color color) {
        int result = (int) (color.getRed() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getBlue() * 0xFF);
        return result;
    }

    /**
     * Get a integer from color bytes.
     * @return rgbInt
     */
    public static int toRGB(byte red, byte green, byte blue) {
        int result = byteToUnsignedShort(red);
        result = (result << 8) + byteToUnsignedShort(green);
        result = (result << 8) + byteToUnsignedShort(blue);
        return result;
    }

    /**
     * Get a integer from a color object.
     * @param color The color to turn into bgr.
     * @return rgbInt
     */
    public static int toBGR(Color color) {
        int result = (int) (color.getBlue() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getRed() * 0xFF);
        return result;
    }

    /**
     * Turn a matrix into a string.
     * @param matrix The matrix to turn into a string.
     * @return matrixStr
     */
    public static String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < matrix.length; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(Arrays.toString(matrix[i]));
        }

        return sb.append("]").toString();
    }

    /**
     * Capitalize every letter after a space.
     * @param sentence The sentence to capitalize.
     * @return capitalized
     */
    public static String capitalize(String sentence) {
        String[] split = sentence.replaceAll("_", " ").split(" ");
        List<String> out = new ArrayList<>();
        for (String s : split)
            out.add(s.length() > 0 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : "");
        return String.join(" ", out);
    }

    /**
     * Turn a BufferedImage into an FX Image.
     * @param image The image to convert.
     * @return convertedImage
     */
    public static Image toFXImage(BufferedImage image, boolean useCache) {
        imageCacheMap.entrySet().removeIf(entry -> entry.getValue().hasExpired());
        if (!useCache)
            return SwingFXUtils.toFXImage(image, null);

        return imageCacheMap.computeIfAbsent(image, bufferedImage -> new TextureCache(SwingFXUtils.toFXImage(bufferedImage, null))).getImage();
    }

    private static class TextureCache {
        private long lastUpdate;
        private Image fxImage;

        public TextureCache(Image fxImage) {
            this.fxImage = fxImage;
        }

        /**
         * Gets the image.
         */
        public Image getImage() {
            this.lastUpdate = System.currentTimeMillis();
            return fxImage;
        }

        /**
         * Has this image expired?
         * @return hasExpired
         */
        public boolean hasExpired() {
            return (System.currentTimeMillis() - lastUpdate) > IMAGE_CACHE_EXPIRE;
        }
    }
}
