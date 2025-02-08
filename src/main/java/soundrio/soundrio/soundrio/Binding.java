package soundrio.soundrio.soundrio;

import com.google.gson.annotations.Expose;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

public class Binding {
    @Expose
    private Set<String> keys;
    @Expose
    private String filePath;
    @Expose
    private boolean isDirectory;

    public Binding(Set<String> combination, String path, boolean isDirectory) {
        this.isDirectory = isDirectory;
        this.keys = new TreeSet<>();
        this.keys.addAll(combination);
        this.filePath = path;
    }

    public synchronized Set<String> getCombination() {
        return this.keys;
    }

    public synchronized String getFormattedCombination() {
        String res;
        res = String.join("+", keys);
        if (res.isEmpty()){
            return "...";
        }
        return res;
    }

    public synchronized String getFilePath() {
        return this.filePath;
    }

    public synchronized String getFormattedPath() {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }

    @Override
    public String toString() {
        return getFormattedCombination() + "\t\t" + getFilePath() + "\t\t" + (isDirectory ? "Dir" : "File") + "\t";
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}
