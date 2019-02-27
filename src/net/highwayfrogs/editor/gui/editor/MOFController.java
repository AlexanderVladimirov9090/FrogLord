package net.highwayfrogs.editor.gui.editor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.system.AbstractStringConverter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls the MOF editor GUI.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFController extends EditorController<MOFHolder> {
    private double oldMouseX;
    private double oldMouseY;
    private double mouseX;
    private double mouseY;

    private MOFUIController uiController;
    private PerspectiveCamera camera;
    private Scene mofScene;
    private MOFMesh mofMesh;
    private Group root3D;
    private Rotate rotX;
    private Rotate rotY;
    private Rotate rotZ;

    @Setter private int framesPerSecond = 20;
    private boolean animationPlaying;
    private Timeline animationTimeline;

    @Override
    public void onInit(AnchorPane editorRoot) {
        setupMofViewer(GUIMain.MAIN_STAGE, TextureMap.newTextureMap(getFile().asStaticFile()));
    }

    @SneakyThrows
    private void setupMofViewer(Stage stageToOverride, TextureMap texMap) {
        this.mofMesh = new MOFMesh(getFile(), texMap);

        // Create and setup material properties for rendering the level, entity icons and bounding boxes.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.BLACK);
        material.setSpecularColor(Color.BLACK);

        Image fxImage = Utils.toFXImage(texMap.getImage(), true);
        material.setDiffuseMap(fxImage);
        material.setSelfIlluminationMap(fxImage);

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(getMofMesh());

        this.rotX = new Rotate(0, Rotate.X_AXIS);
        this.rotY = new Rotate(0, Rotate.Y_AXIS);
        this.rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);

        // Setup a perspective camera through which the 3D view is realised.
        this.camera = new PerspectiveCamera(true);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mof-view.fxml"));
        Parent loadRoot = fxmlLoader.load();
        this.uiController = fxmlLoader.getController();

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group(this.camera, meshView);
        SubScene subScene3D = new SubScene(root3D, stageToOverride.getScene().getWidth() - uiController.uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);
        camera.setFarClip(MapUIController.MAP_VIEW_FAR_CLIP);
        subScene3D.setFill(Color.GRAY);
        subScene3D.setCamera(camera);

        // Setup the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        uiPane.setCenter(subScene3D);

        // Create and set the scene.
        mofScene = new Scene(uiPane);
        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mofScene);

        // Handle scaling of SubScene on stage resizing.
        mofScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - uiController.uiRootPaneWidth()));
        subScene3D.heightProperty().bind(mofScene.heightProperty());

        // Input (key) event processing.
        mofScene.setOnKeyPressed(event -> {
            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE)
                Utils.setSceneKeepPosition(stageToOverride, defaultScene);

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

            if (!isAnimationPlaying()) {
                if (event.getCode() == KeyCode.LEFT) {
                    getMofMesh().setFrame(getMofMesh().getFrameCount() - 1);
                    uiController.updateTempUI();
                } else if (event.getCode() == KeyCode.RIGHT) {
                    getMofMesh().setFrame(getMofMesh().getFrameCount() + 1);
                    uiController.updateTempUI();
                }
            }
        });

        mofScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * .25)));

        mofScene.setOnMousePressed(e -> {
            mouseX = oldMouseX = e.getSceneX();
            mouseY = oldMouseY = e.getSceneY();

            uiController.anchorPaneUIRoot.requestFocus();
        });

        mofScene.setOnMouseDragged(e -> {
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            double mouseXDelta = (mouseX - oldMouseX);
            double mouseYDelta = (mouseY - oldMouseY);

            if (e.isPrimaryButtonDown()) {
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * 0.25)); // Rotate the object.
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * 0.25));
            } else if (e.isMiddleButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - (mouseXDelta * 0.25)); // Move the camera.
                camera.setTranslateY(camera.getTranslateY() - (mouseYDelta * 0.25));
            }
        });

        camera.setTranslateZ(-100.0);
        camera.setTranslateY(-10.0);

        uiController.setHolder(this);
    }

    /**
     * Start playing the MOF animation.
     */
    public void startPlaying(boolean repeat, EventHandler<ActionEvent> onFinish) {
        stopPlaying();
        if (!repeat) // Reset at frame zero when playing a non-paused mof.
            getMofMesh().setFrame(0);

        this.animationPlaying = true;
        this.animationTimeline = new Timeline(new KeyFrame(Duration.millis(1000D / getFramesPerSecond()), evt ->
                getMofMesh().setFrame(getMofMesh().getFrameCount() + 1)));
        this.animationTimeline.setCycleCount(repeat ? Timeline.INDEFINITE : getMofMesh().getMofHolder().getMaxFrame(getMofMesh().getAction()) - 1);
        this.animationTimeline.play();
        this.animationTimeline.setOnFinished(onFinish);
    }

    /**
     * Stop playing the MOF animation.
     */
    public void stopPlaying() {
        if (!isAnimationPlaying())
            return;

        this.animationPlaying = false;
        this.animationTimeline.stop();
        this.animationTimeline = null;
    }

    @Getter
    public static final class MOFUIController implements Initializable {
        private MOFHolder holder;
        private MOFController controller;

        // Baseline UI components
        @FXML private AnchorPane anchorPaneUIRoot;
        @FXML private Accordion accordionLeft;

        @FXML private Button playButton;
        @FXML private CheckBox repeatCheckbox;
        @FXML private TextField fpsField;

        @FXML private TitledPane paneAnim;
        @FXML private ComboBox<Integer> animationSelector;

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            paneAnim.setExpanded(true);
            updateTempUI();

            playButton.setOnAction(evt -> {
                boolean newState = !getController().isAnimationPlaying();
                playButton.setText(newState ? "Stop" : "Play");
                repeatCheckbox.setDisable(newState);
                animationSelector.setDisable(newState);
                fpsField.setDisable(newState);

                if (newState) {
                    getController().startPlaying(this.repeatCheckbox.isSelected(), playButton.getOnAction());
                } else {
                    getController().stopPlaying();
                }
            });

            Utils.setHandleKeyPress(fpsField, newString -> {
                if (!Utils.isInteger(newString))
                    return false;

                int newFps = Integer.parseInt(newString);
                if (newFps < 0)
                    return false;

                getController().setFramesPerSecond(newFps);
                return true;
            }, null);
        }

        /**
         * Get the root pane width.
         */
        public double uiRootPaneWidth() {
            return anchorPaneUIRoot.getPrefWidth();
        }

        /**
         * Sets the MOF Holder this controls.
         */
        public void setHolder(MOFController controller) {
            this.controller = controller;
            this.holder = controller.getFile();

            List<Integer> numbers = new ArrayList<>(Utils.getIntegerList(holder.getMaxAnimation()));
            numbers.add(0, -1);
            animationSelector.setItems(FXCollections.observableArrayList(numbers));
            animationSelector.setConverter(new AbstractStringConverter<>(id -> id == -1 ? "No Animation" : holder.getName(id)));
            animationSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue != null)
                    controller.getMofMesh().setAction(newValue);
            }));
            animationSelector.getSelectionModel().select(0); // Automatically selects no animation.

            updateTempUI();
        }

        /**
         * A very quick and dirty (and temporary!) UI. Will be replaced...
         */
        public void updateTempUI() {
            paneAnim.setExpanded(true);
            anchorPaneUIRoot.requestFocus();
            if (getController() != null)
                fpsField.setText(String.valueOf(getController().getFramesPerSecond()));
        }
    }
}