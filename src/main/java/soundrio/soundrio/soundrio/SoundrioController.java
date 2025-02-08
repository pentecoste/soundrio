package soundrio.soundrio.soundrio;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class SoundrioController {
    private final SoundrioModel soundrioModel;
    private Stage mainStage;
    private final List<AbstractMap.SimpleEntry<Button, Binding>> removeButtonsAssociations;
    public final static int BUTTON_STR_LIMIT = 54;

    @FXML
    private Button listenCombinationsButton;
    @FXML
    private Button chooseFileOrDirectoryButton;
    @FXML
    private CheckBox isDirectoryCheckBox;
    @FXML
    private VBox bindingsVBox;

    public SoundrioController () {
        this.soundrioModel = new SoundrioModel();
        soundrioModel.loadBindings();
        this.removeButtonsAssociations = new ArrayList<>();
    }

    public void listenForCombinations(ActionEvent actionEvent) {
        if (!this.soundrioModel.isListeningForCombinations()) {
            this.soundrioModel.removeListenedKeys();
            this.soundrioModel.setListeningForCombinations(true);
            updateGUI();
        }
    }

    public void chooseFileOrDirectory(ActionEvent actionEvent) {
        if (soundrioModel.isDirectory()) {
            chooseDirectory(actionEvent);
        } else {
            chooseFile(actionEvent);
        }
    }

    private void chooseDirectory(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a folder containing some audios");

        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedDirectory = directoryChooser.showDialog(mainStage);
        if (selectedDirectory != null) {
            soundrioModel.setDirectoryPath(selectedDirectory.getAbsolutePath());
            updateGUI();
        }
    }

    private void chooseFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose an audio file");

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac");
        fileChooser.getExtensionFilters().add(extFilter);

        File selectedFile = fileChooser.showOpenDialog(mainStage);

        if (selectedFile != null) {
            soundrioModel.setFilePath(selectedFile.getAbsolutePath());
            updateGUI();
        }
    }

    public void addBinding(ActionEvent actionEvent) {
        this.soundrioModel.registerBinding();
        updateGUI();
    }

    public void updateGUI () {
        String fileOrDirButtonText = limitChars(soundrioModel.isDirectory() ? soundrioModel.getDirectoryPath().isEmpty() ? "Choose Directory" : soundrioModel.getDirectoryPath() : soundrioModel.getFilePath().isEmpty() ? "Choose File" : soundrioModel.getFilePath(), BUTTON_STR_LIMIT);
        String combinationButtonText = soundrioModel.getFormattedCombination();
        List<Binding> bindings = soundrioModel.getBindings();
        this.removeButtonsAssociations.clear();
        for (Binding binding : bindings) {
            this.removeButtonsAssociations.addLast(new AbstractMap.SimpleEntry<>(new Button(), binding));
        }
        Platform.runLater(() -> {
            bindingsVBox.getChildren().clear();
            for (AbstractMap.SimpleEntry<Button, Binding> entry : removeButtonsAssociations) {
                HBox bindingLine = new HBox();
                Label combination = new Label();
                combination.setText(entry.getValue().getFormattedCombination());
                combination.getStyleClass().add("binding_label");
                Pane spacer1 = new Pane();
                HBox.setHgrow(spacer1, Priority.ALWAYS);
                Label path = new Label();
                path.setText(entry.getValue().getFormattedPath());
                path.getStyleClass().add("binding_label");
                Pane spacer2 = new Pane();
                HBox.setHgrow(spacer2, Priority.ALWAYS);
                Button removeButton = entry.getKey();
                removeButton.setText("Remove");
                removeButton.setOnAction(this::removeBinding);
                removeButton.getStyleClass().add("lower_button");
                bindingLine.getStyleClass().add("binding_line");
                bindingLine.getChildren().add(combination);
                bindingLine.getChildren().add(spacer1);
                bindingLine.getChildren().add(path);
                bindingLine.getChildren().add(spacer2);
                bindingLine.getChildren().add(removeButton);
                bindingsVBox.getChildren().add(bindingLine);
            }
            chooseFileOrDirectoryButton.setText(fileOrDirButtonText);
            listenCombinationsButton.setText(combinationButtonText);
        });
    }

    public void removeBinding(ActionEvent event) {
        Button source = (Button) event.getSource();
        Binding binding = null;
        for (AbstractMap.SimpleEntry<Button, Binding> entry : removeButtonsAssociations) {
            if (entry.getKey().equals(source)) {
                binding = entry.getValue();
                break;
            }
        }
        if (binding != null) {
            soundrioModel.unregisterBinding(binding);
            updateGUI();
        }
    }

    public void keyPressed(String e) {
        if (this.soundrioModel.isListeningForCombinations()) {
            this.soundrioModel.addListenedKey(e);
            updateGUI();
        }

        this.soundrioModel.addActualKey(e);
    }

    public void keyReleased(String e) {
        this.soundrioModel.setListeningForCombinations(false);
        this.soundrioModel.removeActualKey(e);
    }

    public void initialize(Stage stage) {
        this.mainStage = stage;
    }

    public void setDirectory(ActionEvent actionEvent) {
        boolean isSelected = this.isDirectoryCheckBox.isSelected();
        this.soundrioModel.setIsDirectory(isSelected);
        updateGUI();
    }

    private String limitChars(String input, int until) {
        if (input.length() > until) {
            return input.substring(0,until) + "...";
        }
        return input;
    }
}