package soundrio.soundrio.soundrio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SoundrioModel {
    private final Set<String> stopCombination;
    private boolean listeningForCombinations;
    private final Set<String> combination;
    private final Set<String> actualKeys;
    private final AudioPlayer player;
    @Expose
    @SerializedName("bindings")
    private List<Binding> bindings;
    private boolean isDirectory;
    private String directoryPath;
    private String filePath;


    public SoundrioModel() {
        this.combination = new TreeSet<>((s1, s2) -> {
            if (s1.length() != s2.length()) {
                return s2.length() - s1.length();
            } else {
                return s1.compareTo(s2);
            }
        });
        this.stopCombination = new TreeSet<>((s1, s2) -> {
            if (s1.length() != s2.length()) {
                return s2.length() - s1.length();
            } else {
                return s1.compareTo(s2);
            }
        });
        stopCombination.add("alt");
        stopCombination.add("s");
        this.bindings = new ArrayList<>();
        this.actualKeys = new TreeSet<>((s1, s2) -> {
            if (s1.length() != s2.length()) {
                return s2.length() - s1.length();
            } else {
                return s1.compareTo(s2);
            }
        });
        this.listeningForCombinations = false;
        this.directoryPath = "";
        this.filePath = "";
        this.player = new AudioPlayer();
    }

    public synchronized void setListeningForCombinations(boolean val) {
        this.listeningForCombinations = val;
    }

    public synchronized boolean isListeningForCombinations() {
        return this.listeningForCombinations;
    }

    public synchronized void addListenedKey(String e) {
        combination.add(e.toLowerCase());
    }

    public synchronized void removeListenedKey(String e) {
        combination.remove(e.toLowerCase());
    }

    public synchronized void persistBindings() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        try (FileWriter writer = new FileWriter("bindings.json")) {
            gson.toJson(this, writer);
        } catch (IOException ignored) {
            System.out.println("Unable to save bindings to disk. Skipping save");
        }
    }

    public synchronized void loadBindings() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        try (FileReader reader = new FileReader("bindings.json")) {
            SoundrioModel loaded = gson.fromJson(reader, SoundrioModel.class);
            if (loaded != null) {
                this.bindings = loaded.getBindings();
                printBindings(bindings);
            } else {
                System.out.println("Could not load bindings from bindings.json");
            }
        } catch (IOException ignored) {
            System.out.println("Unable to load bindings from disk. Skipping load");
        }
    }

    private void printBindings(List<Binding> bindings) {
        System.out.println("Active bindings: ");
        for (Binding binding : bindings) {
            System.out.println(binding.toString());
        }
    }

    public synchronized String getFormattedCombination() {
        String res;
        res = String.join("+", combination);
        if (res.isEmpty()){
            return "...";
        }
        return res;
    }

    public synchronized void removeListenedKeys() {
        combination.clear();
    }

    public synchronized void setIsDirectory(boolean selected) {
        this.isDirectory = selected;
    }

    public synchronized boolean isDirectory() {
        return this.isDirectory;
    }

    public synchronized void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public synchronized String getDirectoryPath() {
        return this.directoryPath;
    }

    public synchronized String getFilePath() {
        return this.filePath;
    }

    public synchronized void registerBinding() {
        if (combination.isEmpty() || alreadyPresent(combination) || (isDirectory ? directoryPath.isEmpty() : filePath.isEmpty()) || combination.equals(stopCombination)) {
            return;
        }
        this.bindings.add(new Binding(combination, this.isDirectory ? this.directoryPath : this.filePath, this.isDirectory));
        this.combination.clear();
        this.filePath = "";
        this.directoryPath = "";
        persistBindings();
    }

    private boolean alreadyPresent(Set<String> combination) {
        for (Binding binding : bindings) {
            if (binding.getCombination().containsAll(combination)) {
                return true;
            }
        }
        return false;
    }

    public synchronized List<Binding> getBindings() {
        return this.bindings;
    }

    public synchronized void unregisterBinding(Binding binding) {
        bindings.remove(binding);
        persistBindings();
    }

    public synchronized void addActualKey(String e) {
        this.actualKeys.add(e.toLowerCase());
        Binding binding = bindingPressed();
        if (binding != null) {
            player.playAudio(binding.getFilePath(), binding.isDirectory());
        }

        if (stopCombination.equals(this.actualKeys)) {
            player.stopAudio();
        }
    }

    public synchronized void removeActualKey(String e) {
        this.actualKeys.remove(e.toLowerCase());
    }

    private Binding bindingPressed() {
        for (Binding binding: bindings) {
            if (binding.getCombination().equals(this.actualKeys)) {
                return binding;
            }
        }
        return null;
    }
}

