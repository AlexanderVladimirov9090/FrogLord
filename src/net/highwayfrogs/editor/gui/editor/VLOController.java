package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIMain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controls the VLO edit screen.
 * TODO: Control In-game width + height.
 * TODO: Show Flags?
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController extends EditorController<VLOArchive> {
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private CheckBox sizeCheckBox;
    @FXML private ImageView imageView;
    @FXML private ListView<GameImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;

    private GameImage selectedImage;
    private double defaultEditorMaxHeight;

    private static final int SCALE_DIMENSION = 256;

    @Override
    public void loadFile(VLOArchive vlo) {
        super.loadFile(vlo);

        ObservableList<GameImage> gameImages = FXCollections.observableArrayList(vlo.getImages());
        imageList.setItems(gameImages);
        imageList.setCellFactory(param -> new AttachmentListCell());

        imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedImage = newValue;
            this.updateImage();
            this.updateImageInfo();
        });

        imageList.getSelectionModel().select(0);
    }

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        this.defaultEditorMaxHeight = editorRoot.getMaxHeight();
    }

    @Override
    public void onClose(AnchorPane editorRoot) {
        super.onClose(editorRoot);
        editorRoot.setMaxHeight(this.defaultEditorMaxHeight);
    }

    private static class AttachmentListCell extends ListCell<GameImage> {
        @Override
        public void updateItem(GameImage image, boolean empty) {
            super.updateItem(image, empty);
            setText(empty ? null
                    : getIndex() + ": [" + image.getFullWidth() + ", " + image.getFullHeight() + "] (Tex ID: " + image.getTextureId() + ")");
        }
    }


    @FXML
    private void exportImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify the file to export this image as...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);

        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            ImageIO.write(toBufferedImage(), "png", selectedFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void importImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the image to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            this.selectedImage.replaceImage(ImageIO.read(selectedFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        updateImage();
        updateImageInfo();
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the directory to export images to.");
        chooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFolder);
        GameImage originalImage = this.selectedImage;

        try {
            for (int i = 0; i < getFile().getImages().size(); i++) {
                this.selectedImage = getFile().getImages().get(i);
                File output = new File(selectedFolder, i + ".png");
                ImageIO.write(toBufferedImage(), "png", output);
                System.out.println("Exported image #" + i + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            this.selectedImage = originalImage;
        }
    }

    @FXML
    private void onImageToggle(ActionEvent event) {
        updateImage();
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateImageInfo() {
        dimensionLabel.setText("Archive Dimensions: [Width: " + this.selectedImage.getFullWidth() + ", Height: " + this.selectedImage.getFullHeight() + "]");
        ingameDimensionLabel.setText("In-Game Dimensions: [Width: " + this.selectedImage.getIngameWidth() + ", Height: " + this.selectedImage.getIngameHeight() + "]");
        idLabel.setText("Texture ID: " + this.selectedImage.getTextureId() + ", Flags: " + this.selectedImage.getFlags());
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        boolean hasImage = (this.selectedImage != null);
        imageView.setVisible(hasImage);

        if (hasImage) {
            BufferedImage image = toBufferedImage();

            boolean scaleSize = sizeCheckBox.isSelected();
            imageView.setFitWidth(scaleSize ? SCALE_DIMENSION : image.getWidth());
            imageView.setFitHeight(scaleSize ? SCALE_DIMENSION : image.getHeight());

            imageView.setImage(SwingFXUtils.toFXImage(image, null));
        }
    }

    private BufferedImage toBufferedImage() {
        return this.selectedImage.toBufferedImage(!this.paddingCheckBox.isSelected(), this.transparencyCheckBox.isSelected());
    }
}
