package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creates an editor grid.
 * Created by Kneesnap on 1/20/2019.
 */
@SuppressWarnings("UnusedReturnValue")
public class GUIEditorGrid {
    private GridPane gridPane;
    private int rowIndex;

    public GUIEditorGrid(GridPane pane) {
        this.gridPane = pane;
    }

    /**
     * Clear everything from this editor.
     */
    public void clearEditor() {
        this.rowIndex = 0;
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
    }

    /**
     * Add a label.
     * @param label The label to add.
     */
    public void addBoldLabel(String label) {
        Label labelText = new Label(label);
        GridPane.setColumnSpan(labelText, 2);
        labelText.setFont(Constants.SYSTEM_BOLD_FONT);

        gridPane.getChildren().add(setupNode(labelText));
        addRow(15);
    }

    /**
     * Adds a label
     * @param text The text to add.
     */
    public Label addLabel(String text) {
        Label newLabel = new Label(text);
        gridPane.getChildren().add(setupNode(newLabel));
        return newLabel;
    }

    /**
     * Add a label.
     * @param label The label to add.
     * @param value The value of the label.
     */
    public Label addLabel(String label, String value) {
        addLabel(label);
        Label valueText = new Label(value);
        GridPane.setColumnIndex(valueText, 1);

        gridPane.getChildren().add(setupNode(valueText));
        addRow(15);
        return valueText;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value) {
        addLabel(label);
        TextField field = new TextField(value);
        GridPane.setColumnIndex(field, 1);
        gridPane.getChildren().add(setupNode(field));

        addRow(25);
        return field;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value, Function<String, Boolean> setter) {
        TextField field = addTextField(label, value);
        field.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (field.getStyle().isEmpty() && (code.isLetterKey() || code.isDigitKey() || code == KeyCode.BACK_SPACE)) {
                field.setStyle("-fx-text-inner-color: darkgreen;");
            } else if (code == KeyCode.ENTER) {
                boolean pass = setter.apply(field.getText());
                field.setStyle(pass ? null : "-fx-text-inner-color: red;");
            }
        });

        return field;
    }

    /**
     * Add an integer field.
     * @param label  The name.
     * @param number The initial number.
     * @param setter The success behavior.
     * @param test   Whether the number is valid.
     * @return textField
     */
    public TextField addIntegerField(String label, int number, Consumer<Integer> setter, Function<Integer, Boolean> test) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!Utils.isInteger(str))
                return false;

            int value = Integer.parseInt(str);
            boolean testPass = test == null || test.apply(value);

            if (testPass)
                setter.accept(value);

            return testPass;
        });
    }

    /**
     * Add a short field.
     * @param label  The name.
     * @param number The initial number.
     * @param setter The success behavior.
     * @param test   Whether the number is valid.
     * @return textField
     */
    public TextField addShortField(String label, short number, Consumer<Short> setter, Function<Short, Boolean> test) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!Utils.isInteger(str))
                return false;

            int intValue = Integer.parseInt(str);
            if (intValue < Short.MIN_VALUE || intValue > Short.MAX_VALUE)
                return false;

            short shortValue = (short) intValue;
            boolean testPass = test == null || test.apply(shortValue);

            if (testPass)
                setter.accept(shortValue);

            return testPass;
        });
    }

    /**
     * Add a selection-box.
     * @param label   The label text to add.
     * @param current The currently selected value.
     * @param values  Accepted values. (If null is acceptible, add null to this list.)
     * @param setter  The setter
     * @return comboBox
     */
    public <T> ComboBox<T> addSelectionBox(String label, T current, List<T> values, Consumer<T> setter) {
        addLabel(label);
        ComboBox<T> box = new ComboBox<>(FXCollections.observableArrayList(values));
        box.valueProperty().setValue(current); // Set the selected value.
        box.getSelectionModel().select(current); // Automatically scroll to selected value.
        GridPane.setColumnIndex(box, 1);
        gridPane.getChildren().add(setupNode(box));

        box.valueProperty().addListener((listener, oldVal, newVal) -> setter.accept(newVal));

        addRow(25);
        return box;
    }

    /**
     * Add a selection-box.
     * @param label   The label text to add.
     * @param current The currently selected value.
     * @param values  Accepted values. (If null is acceptible, add null to this list.)
     * @param setter  The setter
     * @return comboBox
     */
    public <E extends Enum<E>> ComboBox<E> addEnumSelector(String label, E current, E[] values, boolean allowNull, Consumer<E> setter) {
        List<E> enumList = new ArrayList<>(Arrays.asList(values));
        if (allowNull)
            enumList.add(0, null);

        return addSelectionBox(label, current, enumList, setter);
    }

    /**
     * Add a checkbox.
     * @param label        The check-box label.
     * @param currentState The current state of the check-box.
     * @param setter       What to do when the checkbox is changed.
     * @return checkBox
     */
    public CheckBox addCheckBox(String label, boolean currentState, Consumer<Boolean> setter) {
        CheckBox box = new CheckBox(label);
        GridPane.setColumnSpan(box, 2);
        box.setSelected(currentState);
        gridPane.getChildren().add(setupNode(box));

        box.selectedProperty().addListener((listener, oldVal, newVal) -> setter.accept(newVal));

        addRow(15);
        return box;
    }

    private Node setupNode(Node node) {
        GridPane.setRowIndex(node, this.rowIndex);
        return node;
    }

    private void addRow(double height) {
        RowConstraints newRow = new RowConstraints(height + 1);
        gridPane.getRowConstraints().add(newRow);
        this.rowIndex++;
    }
}
