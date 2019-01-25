package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.gui.mesh.MeshData;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the grid editor gui.
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
public class GridController implements Initializable {
    @FXML private Canvas gridCanvas;
    private GraphicsContext graphics;

    @FXML private ImageView selectedImage;
    @FXML private ComboBox<Integer> layerSelector;
    @FXML private GridPane flagTable;

    private Stage stage;
    private MapUIController controller;
    private MAPFile map;

    private GridStack selectedStack;
    private int selectedLayer;
    private double tileWidth;
    private double tileHeight;

    private GridController(Stage stage, MapUIController controller, MAPFile map) {
        this.stage = stage;
        this.controller = controller;
        this.map = map;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        layerSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null)
                setSelectedSquare(getSelectedStack(), newValue);
        }));

        layerSelector.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer id) {
                return "Layer #" + (id + 1);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        gridCanvas.setOnMousePressed(evt -> {
            int gridX = (int) (evt.getSceneX() / getTileWidth());
            int gridZ = (int) (evt.getSceneY() / getTileHeight());
            GridStack stack = getMap().getGridStack(gridX, getMap().getGridZCount() - gridZ - 1);

            if (evt.isSecondaryButtonDown()) { // Remove.
                stack.getGridSquares().clear();
                updateCanvas();
            }

            setSelectedStack(stack);
        });

        graphics = gridCanvas.getGraphicsContext2D();
        updateCanvas();
        setSelectedStack(null);
    }

    private void updateCanvas() {
        this.tileWidth = gridCanvas.getWidth() / getMap().getGridXCount();
        this.tileHeight = gridCanvas.getHeight() / getMap().getGridZCount();

        graphics.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        TextureMap texMap = getController().getMesh().getTextureMap();
        Image fxTextureImage = SwingFXUtils.toFXImage(texMap.getImage(), null);

        graphics.setFill(Color.GRAY);
        for (int z = 0; z < getMap().getGridZCount(); z++) {
            for (int x = 0; x < getMap().getGridXCount(); x++) {
                GridStack stack = getMap().getGridStack(x, z);

                double xPos = getTileWidth() * x;
                double yPos = getTileHeight() * (getMap().getGridZCount() - z - 1);

                if (stack.getGridSquares().size() > 0) {
                    GridSquare square = stack.getGridSquares().get(0);
                    TextureEntry entry = square.getPolygon().getEntry(texMap);
                    graphics.drawImage(fxTextureImage, entry.getX(texMap), entry.getY(texMap), entry.getWidth(texMap), entry.getHeight(texMap), xPos, yPos, getTileWidth(), getTileHeight());
                } else {
                    graphics.fillRect(xPos, yPos, getTileWidth(), getTileHeight());
                }
            }
        }

        graphics.setStroke(Color.BLACK);
        for (int x = 0; x <= getMap().getGridXCount(); x++)
            graphics.strokeLine(x * getTileWidth(), 0, x * getTileWidth(), gridCanvas.getHeight());

        for (int z = 0; z <= getMap().getGridZCount(); z++)
            graphics.strokeLine(0, z * getTileHeight(), gridCanvas.getWidth(), z * getTileHeight());
    }

    private void selectSquare(Consumer<PSXPolygon> onSelect) {
        stage.close();

        for (GridStack stack : getMap().getGridStacks())
            for (GridSquare square : stack.getGridSquares())
                getController().getController().renderOverPolygon(square.getPolygon(), MapMesh.GRID_COLOR);
        MeshData data = getController().getMesh().getManager().addMesh();

        getController().selectPolygon(poly -> {
            getController().getMesh().getManager().removeMesh(data);
            onSelect.accept(poly);
            updateCanvas();
            Platform.runLater(stage::showAndWait);
        }, () -> {
            getController().getMesh().getManager().removeMesh(data);
            Platform.runLater(stage::showAndWait);
        });
    }

    @FXML
    private void choosePolygon(ActionEvent evt) {
        if (getSelectedStack() == null)
            return;

        selectSquare(poly -> {
            getSelectedStack().getGridSquares().get(getSelectedLayer()).setPolygon(poly);
            setSelectedSquare(getSelectedStack(), getSelectedLayer());
        });
    }

    @FXML
    private void addLayer(ActionEvent evt) {
        if (getSelectedStack() == null)
            return;

        selectSquare(poly -> {
            getSelectedStack().getGridSquares().add(new GridSquare(poly, getMap()));
            setSelectedStack(getSelectedStack());
            setSelectedSquare(getSelectedStack(), getSelectedStack().getGridSquares().size() - 1);
        });
    }

    @FXML
    private void removeLayer(ActionEvent evt) {
        if (getSelectedStack() == null || getSelectedStack().getGridSquares().isEmpty())
            return;

        getSelectedStack().getGridSquares().remove(this.selectedLayer);
        setSelectedStack(getSelectedStack());
        updateCanvas();
    }

    /**
     * Select a square.
     * @param stack The stack the square belongs to.
     * @param layer The layer.
     */
    public void setSelectedSquare(GridStack stack, int layer) {
        this.selectedLayer = layer;

        TextureMap texMap = getController().getMesh().getTextureMap();
        GridSquare square = stack.getGridSquares().get(layer);
        TextureEntry entry = square.getPolygon().getEntry(texMap);

        selectedImage.setImage(SwingFXUtils.toFXImage(entry.getImage(texMap), null));

        int x = 1;
        int y = 0;
        flagTable.getChildren().clear();
        for (GridSquareFlag flag : GridSquareFlag.values()) {
            if (x == 2) {
                x = 0;
                y++;
            }

            CheckBox checkBox = new CheckBox(Utils.capitalize(flag.name()));
            GridPane.setRowIndex(checkBox, y);
            GridPane.setColumnIndex(checkBox, x++);
            checkBox.setSelected(square.testFlag(flag));
            checkBox.selectedProperty().addListener(((observable, oldValue, newValue) -> square.setFlag(flag, newValue)));
            flagTable.getChildren().add(checkBox);
        }
    }

    /**
     * Select the stack currently being edited.
     * @param stack The stack to select.
     */
    public void setSelectedStack(GridStack stack) {
        this.selectedStack = stack;

        if (stack != null) {
            List<Integer> layers = new LinkedList<>();
            for (int i = 0; i < stack.getGridSquares().size(); i++)
                layers.add(i);

            layerSelector.setItems(FXCollections.observableArrayList(layers));
            if (layers.size() > 0)
                layerSelector.getSelectionModel().select(0); // Automatically calls setSquare
        }

        int squareCount = stack != null ? stack.getGridSquares().size() : 0;
        flagTable.setVisible(squareCount > 0);
        selectedImage.setVisible(squareCount > 0);
        layerSelector.setVisible(squareCount > 1);
    }

    /**
     * Open the padding menu for a particular image.
     * @param controller The VLO controller opening this.
     */
    public static void openGridEditor(MapUIController controller) {
        Utils.loadFXMLTemplate("grid", "Grid Editor", newStage -> new GridController(newStage, controller, controller.getMap()));
    }
}
