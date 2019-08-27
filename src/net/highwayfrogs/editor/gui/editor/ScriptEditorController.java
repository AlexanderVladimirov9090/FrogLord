package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.script.FroggerScript;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptCommandType;
import net.highwayfrogs.editor.file.config.script.format.ScriptFormatter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Manages the script editor.
 * Created by Kneesnap on 8/1/2019.
 */
public class ScriptEditorController implements Initializable {
    @FXML private ChoiceBox<FroggerScript> scriptSelector;
    @FXML private VBox commandEditors;
    @FXML private TextFlow codeArea;
    @FXML private Button doneButton;
    @FXML private Label warningLabel;
    @FXML private Button printUsagesButton;

    private Stage stage;
    private FroggerScript openScript;

    private static final Font DISPLAY_FONT = Font.font("Consolas");
    private static final String COMMAND_TYPE_STYLE = "-fx-fill: #4F8A10;-fx-font-weight:bold;";

    public ScriptEditorController(Stage stage) {
        this.stage = stage;
    }

    public ScriptEditorController(Stage stage, FroggerScript openScript) {
        this(stage);
        this.openScript = openScript;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup Selector.
        scriptSelector.setConverter(new AbstractStringConverter<>(FroggerScript::getName));
        scriptSelector.setItems(FXCollections.observableArrayList(GUIMain.EXE_CONFIG.getScripts()));
        scriptSelector.valueProperty().addListener(((observable, oldValue, newValue) -> updateCodeDisplay()));
        if (this.openScript != null) {
            scriptSelector.setValue(this.openScript);
            scriptSelector.getSelectionModel().select(this.openScript);
            this.openScript = null;
        } else {
            scriptSelector.getSelectionModel().selectFirst();
        }

        // Setup Buttons.
        doneButton.setOnAction(evt -> stage.close());
        printUsagesButton.setDisable(GUIMain.EXE_CONFIG.getFullFormBook().isEmpty()); // Disable searching if the form table isn't loaded.
        printUsagesButton.setOnAction(evt -> {
            FroggerScript script = this.scriptSelector.getValue();
            int id = GUIMain.EXE_CONFIG.getScripts().indexOf(script);

            StringBuilder results = new StringBuilder("Usages of ").append(script.getName())
                    .append(" (").append(id).append("):").append(Constants.NEWLINE);
            for (FormEntry entry : GUIMain.EXE_CONFIG.getFullFormBook())
                if (entry.getScriptId() == id)
                    results.append(" - ").append(entry.getFormName()).append(Constants.NEWLINE);

            System.out.println(results.toString());
            Utils.makePopUp(results.toString(), AlertType.INFORMATION);
        });

        Utils.closeOnEscapeKey(stage, null);
        updateCodeDisplay();
    }

    /**
     * Updates the code display.
     */
    public void updateCodeDisplay() {
        commandEditors.getChildren().clear();
        codeArea.getChildren().clear();
        this.warningLabel.setVisible(false);
        FroggerScript currentScript = this.scriptSelector.getValue();
        if (currentScript == null)
            return;

        this.warningLabel.setVisible(currentScript.isTooLarge());

        // Update editors.
        for (ScriptCommand command : currentScript.getCommands()) {
            GridPane pane = new GridPane();
            pane.setMinHeight(10);
            pane.setPrefHeight(30);
            VBox.setVgrow(pane, Priority.SOMETIMES);
            pane.setMinWidth(10);
            pane.setPrefWidth(100);
            HBox.setHgrow(pane, Priority.SOMETIMES);

            pane.addRow(0);
            for (int i = 0; i < command.getCommandType().getSize(); i++) {
                Node node;

                if (i == 0) {
                    ComboBox<ScriptCommandType> typeChoiceBox = new ComboBox<>();
                    typeChoiceBox.setItems(FXCollections.observableArrayList(ScriptCommandType.values()));
                    typeChoiceBox.getSelectionModel().select(command.getCommandType());
                    typeChoiceBox.setValue(command.getCommandType());
                    node = typeChoiceBox;

                    typeChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
                        command.setCommandType(newValue);
                        updateCodeDisplay();
                    }));
                } else {
                    node = command.getCommandType().getFormatters()[i - 1].makeEditor(this, command, i - 1);
                }

                pane.addColumn(i, node);
            }
            commandEditors.getChildren().add(pane);
        }


        // Update text view.
        for (ScriptCommand command : currentScript.getCommands()) {
            Text commandTypeText = new Text(command.getCommandType().name());
            commandTypeText.setStyle(COMMAND_TYPE_STYLE);
            commandTypeText.setFont(DISPLAY_FONT);
            codeArea.getChildren().add(commandTypeText);

            for (int i = 0; i < command.getArguments().length; i++) {
                codeArea.getChildren().add(new Text(" "));
                ScriptFormatter formatter = command.getCommandType().getFormatters()[i];
                Text toAdd = new Text(formatter.numberToString(command.getArguments()[i]));
                toAdd.setStyle(formatter.getTextStyle());
                toAdd.setFont(DISPLAY_FONT);
                codeArea.getChildren().add(toAdd);
            }

            codeArea.getChildren().add(new Text(Constants.NEWLINE));
        }
    }

    /**
     * Opens the Script Editor.
     */
    public static void openEditor() {
        Utils.loadFXMLTemplate("script", "Script Editor", ScriptEditorController::new);
    }

    /**
     * Opens the Script Editor and view a given script.
     */
    public static void openEditor(FroggerScript script) {
        Utils.loadFXMLTemplate("script", "Script Editor", stage -> new ScriptEditorController(stage, script));
    }
}
