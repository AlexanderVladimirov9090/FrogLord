package net.highwayfrogs.editor.gui.editor;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Manages the grid editor gui.
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
public class GridController implements Initializable {
    @FXML private Canvas gridCanvas;
    private GraphicsContext graphics;

    private Stage stage;
    private MapUIController controller;
    private MAPFile map;
    private double tileWidth;
    private double tileHeight;

    private GridController(Stage stage, MapUIController controller, MAPFile map) {
        this.stage = stage;
        this.controller = controller;
        this.map = map;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        graphics = gridCanvas.getGraphicsContext2D();
        updateCanvas();
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

                if (stack.getSquareCount() > 0) {
                    GridSquare square = getMap().getGridSquares().get(stack.getIndex()); //TODO: What about multiple squares per?
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

    /**
     * Open the padding menu for a particular image.
     * @param controller The VLO controller opening this.
     */
    public static void openGridEditor(MapUIController controller) {
        Utils.loadFXMLTemplate("grid", "Grid Editor", newStage -> new GridController(newStage, controller, controller.getMap()));
    }
}
