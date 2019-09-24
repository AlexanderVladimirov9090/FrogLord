package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
@Getter
public class MAPController extends EditorController<MAPFile> {
    @FXML private ListView<Short> remapList;
    @FXML private ImageView previewImage;
    @FXML private ImageView nameImage;
    @FXML private ImageView remapImage;
    @FXML private Button changeTextureButton;
    private MapUIController mapUIController;
    @FXML private Button saveTextureButton;

    @Override
    public void loadFile(MAPFile mapFile) {
        super.loadFile(mapFile);

        List<Short> remapTable = mapFile.getConfig().getRemapTable(mapFile.getFileEntry());
        if (remapTable == null) {
            changeTextureButton.setDisable(true);
            remapList.setDisable(true);
            return; // Empty.
        }

        // Display Level Name & Image.
        previewImage.setImage(null);
        nameImage.setImage(null);

        MAPLevel level = MAPLevel.getByName(mapFile.getFileEntry().getDisplayName());
        if (level != null && !mapFile.getConfig().getLevelInfoMap().isEmpty()) {
            LevelInfo info = mapFile.getConfig().getLevelInfoMap().get(level);
            if (info != null) {
                previewImage.setImage(mapFile.getConfig().getImageFromPointer(info.getLevelTexturePointer()).toFXImage());
                nameImage.setImage(mapFile.getConfig().getImageFromPointer(info.getLevelNameTexturePointer()).toFXImage());
            }
        }

        // Setup Remap Editor.
        this.remapList.setItems(FXCollections.observableArrayList(remapTable));
        this.remapList.setCellFactory(param -> new AttachmentListCell<>(num -> "#" + num, num -> {
            GameImage temp = getFile().getVlo() != null ? getFile().getVlo().getImageByTextureId(num, false) : null;
            if (temp == null)
                temp = getFile().getMWD().getImageByTextureId(num);

            return temp != null ? temp.toFXImage(MWDFile.VLO_ICON_SETTING) : null;
        }));

        this.remapList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null)
                return;

            GameImage temp = getFile().getVlo() != null ? getFile().getVlo().getImageByTextureId(newValue, false) : null;
            if (temp == null)
                temp = getFile().getMWD().getImageByTextureId(newValue);
            if (temp != null)
                this.remapImage.setImage(temp.toFXImage(MWDFile.VLO_ICON_SETTING));
        });
        this.remapList.getSelectionModel().selectFirst();

        saveTextureButton.setOnAction(evt -> {
            try {
                ImageIO.write(TextureMap.newTextureMap(getFile()).getTextureTree().getImage(), "png", new File(GUIMain.getWorkingDirectory(), getFile().getFileEntry().getDisplayName() + ".png"));
                System.out.println("Saved " + getFile().getFileEntry().getDisplayName() + ".png");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onChangeTexture(ActionEvent event) {
        if (getFile().getVlo() == null) {
            System.out.println("Cannot edit remaps for a map which has no associated VLO!");
            return;
        }

        getFile().getVlo().promptImageSelection(newImage -> {
            int index = this.remapList.getSelectionModel().getSelectedIndex();
            getFile().getConfig().getRemapTable(getFile().getFileEntry()).set(index, newImage.getTextureId());
            this.remapList.setItems(FXCollections.observableArrayList(getFile().getConfig().getRemapTable(getFile().getFileEntry()))); // Refresh remap.
            this.remapList.getSelectionModel().select(index);
        }, false);
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        TextureMap textureMap = TextureMap.newTextureMap(getFile());
        setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), textureMap), textureMap);
    }

    @FXML
    private void onFixIslandClicked(ActionEvent event) {
        getFile().fixAsIslandMap();
    }

    @FXML
    private void makeNewMap(ActionEvent event) {
        getFile().randomizeMap();
    }

    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        // Create and setup material properties for rendering the level, entity icons and bounding boxes.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(Utils.toFXImage(texMap.getTextureTree().getImage(), false));

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.BACK);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mapui.fxml"));
        Parent loadRoot = fxmlLoader.load();
        this.mapUIController = fxmlLoader.getController(); // Get the custom mapui controller
        this.mapUIController.setupController(this, stageToOverride, mesh, meshView, loadRoot);
    }
}
