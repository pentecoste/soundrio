module soundrio.soundrio.soundrio {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.google.gson;
    requires com.github.kwhat.jnativehook;

    opens soundrio.soundrio.soundrio to javafx.fxml, com.google.gson;
    exports soundrio.soundrio.soundrio;
}