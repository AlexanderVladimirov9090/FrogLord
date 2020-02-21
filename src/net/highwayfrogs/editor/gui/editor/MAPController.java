package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert.AlertType;
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
import net.highwayfrogs.editor.file.map.FFSUtil;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.utils.FileUtils3D;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    @FXML private Button loadFromFFS;
    @FXML private Button saveToFFS;
    @FXML private Button saveToObj;

    @Override
    public void loadFile(MAPFile mapFile) {
        super.loadFile(mapFile);

        List<Short> remapTable = mapFile.getConfig().getRemapTable(mapFile.getFileEntry());
        if (remapTable == null) {
            changeTextureButton.setDisable(true);
            remapList.setDisable(true);
            loadFromFFS.setDisable(true);
            saveToFFS.setDisable(true);
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
            Utils.makePopUp("Cannot edit remaps for a map which has no associated VLO!", AlertType.WARNING);
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
        InputMenu.promptInput("Please enter the grid dimensions for the cleared map.", "5,5", newText -> {
            String[] split = newText.split(",");
            if (split.length != 2) {
                Utils.makePopUp("'" + newText + "' was invalid.\nPlease enter two numbers separated by a comma.", AlertType.ERROR);
                return;
            }

            if (!Utils.isInteger(split[0])) {
                Utils.makePopUp("'" + split[0] + "' is not a valid number.", AlertType.ERROR);
                return;
            }

            if (!Utils.isInteger(split[1])) {
                Utils.makePopUp("'" + split[1] + "' is not a valid number.", AlertType.ERROR);
                return;
            }

            int x = Integer.parseInt(split[0]);
            int z = Integer.parseInt(split[1]);
            if (x < 0 || z < 0) {
                Utils.makePopUp("Dimensions cannot be less than zero.", AlertType.ERROR);
                return;
            }

            if (x > 255 || z > 255) { // Frogger limitation.
                Utils.makePopUp("The collision grid cannot go larger than 255x255.", AlertType.ERROR);
                return;
            }

            getFile().randomizeMap(x, z);
        });
    }

    @FXML
    @SneakyThrows
    private void loadFromFFS(ActionEvent event) {
        FFSUtil.importFFSToMap(getFile(), Utils.promptFileOpen("Choose the .ffs file to import", "FrogLord Map", "ffs"));
    }

    @FXML
    @SneakyThrows
    private void exportToFFS(ActionEvent event) {
        FFSUtil.saveMapAsFFS(getFile(), Utils.promptChooseDirectory("Choose the directory to save the map to.", false));
        Files.write(new File("./frogger-map-blender-plugin.py").toPath(), Utils.readBytesFromStream(Utils.getResourceStream("frogger-map-blender-plugin.py")));
    }

    @FXML
    @SneakyThrows
    private void exportToObj(ActionEvent event) {
        FileUtils3D.exportMapToObj(getFile(), Utils.promptChooseDirectory("Choose the directory to save the map to.", false));
    }

    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        SVector basePoint = getFile().makeBasePoint();
        float baseX = basePoint.getFloatX();
        float baseZ = basePoint.getFloatZ();
        System.out.println("Base: " + baseX + ", " + baseZ);

        float[] pos = new float[6];
        for (Entity entity : getFile().getEntities()) {
            PSXMatrix matrix = entity.getMatrixInfo();
            if (matrix == null)
                continue;

            entity.getPosition(pos, getFile());

            double xDis = (pos[0] - baseX);
            double zDis = (pos[2] - baseZ);
            double distance = Math.sqrt((xDis * xDis) + (zDis * zDis));

            System.out.println(entity.getFormEntry().getFormName() + "[" + entity.getUniqueId() + "]" + " X: " + pos[0] + ", Y: " + pos[1] + ", Z: " + pos[2] + ", " + matrix.getTransform()[0] + ", " + matrix.getTransform()[1] + ", " + matrix.getTransform()[2] + ", " + getFile().getGroupX(matrix.getTransform()[0] >> 16) + ", " + getFile().getGroupZ(matrix.getTransform()[2] >> 16));
            // Probably: Order, position, or unique id.

            // Stuff to test:
            // Distance from origin and id. Nope
            // Distance from origin and sorting. Nope
            // Position and id.

            //TODO: Map Group and registration order are the answer.
        }

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
