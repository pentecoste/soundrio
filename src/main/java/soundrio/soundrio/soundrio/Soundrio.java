package soundrio.soundrio.soundrio;

import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Soundrio extends Application {
    private static KeyListener keyListener;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/soundrio-page.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 600);

        SoundrioController controller = fxmlLoader.getController();
        controller.initialize(stage);

        keyListener = new KeyListener(controller);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());

        scene.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);

        stage.setTitle("Soundrio");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/turt_256x256.png"))));
        stage.setScene(scene);
        stage.show();

        controller.updateGUI();

        stage.setOnCloseRequest(event -> keyListener.stopListening());
    }

    private static void startHeadless(SoundrioController controller) {
        keyListener = new KeyListener(controller);

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.contains("--headless") || argList.contains("-h")) {
            SoundrioController controller = new SoundrioController();
            startHeadless(controller);
        } else {
            launch();
        }
    }

    }