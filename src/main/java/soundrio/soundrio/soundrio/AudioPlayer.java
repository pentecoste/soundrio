package soundrio.soundrio.soundrio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AudioPlayer {
    private static final String[] AUDIO_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac", ".aac"};

    private MediaPlayer currentPlayer;

    public synchronized void playAudio(String filePath, boolean directory) {
        if (currentPlayer != null) {
            stopAudio();
        }

        Media media;
        File mediaFile;
        if (directory) {
            try {
                mediaFile = getRandomAudioFile(filePath);
            } catch (IOException e) {
                return;
            }
            media = new Media(Paths.get(mediaFile.getAbsolutePath()).toUri().toString());
        } else {
            media = new Media(Paths.get(filePath).toUri().toString());
        }
        this.currentPlayer = new MediaPlayer(media);
        currentPlayer.play();
    }

    public synchronized void stopAudio() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
            currentPlayer = null;
        }
    }

    public synchronized static File getRandomAudioFile(String directoryPath) throws IOException {
        Path dir = Path.of(directoryPath);
        try (Stream<Path> paths = Files.walk(dir)) {
            List<File> audioFiles = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> isAudioFile(file.getName()))
                    .toList();

            if (audioFiles.isEmpty()) {
                throw new IOException("No audio file found in: " + directoryPath);
            }

            Random random = new Random();
            return audioFiles.get(random.nextInt(audioFiles.size()));
        }
    }

    private static boolean isAudioFile(String name) {
        for (String ext : AUDIO_EXTENSIONS) {
            if (name.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

}
