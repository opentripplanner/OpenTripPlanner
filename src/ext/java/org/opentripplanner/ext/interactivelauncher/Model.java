package org.opentripplanner.ext.interactivelauncher;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Model.class);

    private static final File MODEL_FILE = new File("interactive_otp_main.json");

    private final Map<String, Boolean> debugLogging = new HashMap<>();

    @JsonIgnore
    private transient Consumer<String> commandLineChange;

    private String rootDirectory = null;
    private String dataSource = null;
    private boolean buildStreet = false;
    private boolean buildTransit = true;
    private boolean saveGraph = false;
    private boolean serveGraph = true;


    public Model() {
        setupListOfDebugLoggers();
    }

    public static Model load() {
        return MODEL_FILE.exists() ? readFromFile() : new Model();
    }

    private static Model readFromFile() {
        try {
            return new ObjectMapper().readValue(MODEL_FILE, Model.class);
        }
        catch (IOException e) {
            System.err.println(
                    "Unable to read the InteractiveOtpMain state cache. If the model changed this "
                            + "is expected, and it will work next time. Cause: " + e.getMessage()
            );
            return new Model();
        }
    }

    public void subscribeCmdLineUpdates(Consumer<String> commandLineChange) {
        this.commandLineChange = commandLineChange;
    }

    @SuppressWarnings("AccessOfSystemProperties")
    public String getRootDirectory() {
        return rootDirectory == null
                ? System.getProperty("user.dir")
                : rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        // If the persisted JSON do not contain the rootDirectory, then avoid setting it
        if (rootDirectory != null) {
            this.rootDirectory = rootDirectory;
        }
        notifyChangeListener();
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
        notifyChangeListener();
    }

    @JsonIgnore
    public List<String> getDataSourceOptions() {
        List<String> dataSourceOptions = new ArrayList<>();
        File rootDir = new File(getRootDirectory());
        List<File> dirs = SearchForOtpConfig.search(rootDir);
        // Add 1 char for the path-separator-character
        int length = rootDir.getAbsolutePath().length() + 1;

        for (File dir : dirs) {
            var path = dir.getAbsolutePath();
            if(path.length() <= length) {
                LOG.warn(
                        "The root directory contains a config file, choose " +
                        "the parent directory or delete the config file."
                );
                continue;
            }
            dataSourceOptions.add(path.substring(length));
        }
        return dataSourceOptions;
    }

    public boolean isBuildStreet() {
        return buildStreet;
    }

    public void setBuildStreet(boolean buildStreet) {
        this.buildStreet = buildStreet;
        notifyChangeListener();
    }

    public boolean isBuildTransit() {
        return buildTransit;
    }

    public void setBuildTransit(boolean buildTransit) {
        this.buildTransit = buildTransit;
        notifyChangeListener();
    }

    public boolean isSaveGraph() {
        return saveGraph;
    }

    public void setSaveGraph(boolean saveGraph) {
        this.saveGraph = saveGraph;
        notifyChangeListener();
    }

    public boolean isServeGraph() {
        return serveGraph;
    }

    public void setServeGraph(boolean serveGraph) {
        this.serveGraph = serveGraph;
        notifyChangeListener();
    }

    public Map<String, Boolean> getDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(Map<String, Boolean> map) {
        for (Entry<String, Boolean> e : map.entrySet()) {
            // Only keep entries that exist in the log config
            if (debugLogging.containsKey(e.getKey())) {
                debugLogging.put(e.getKey(), e.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return "("
                + "data-source-dir: " + getDataSourceDirectory()
                + (buildStreet ? ", buildStreet" : "")
                + (buildTransit ? ", buildTransit" : "")
                + (saveGraph ? ", saveGraph" : "")
                + (serveGraph ? ", serveGraph" : "")
                + ')';
    }

    public String toCliString() {
        return String.join(" ", asOtpArgs());
    }

    public void save() {
        try {
            new ObjectMapper().writeValue(MODEL_FILE, this);
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @JsonIgnore
    String getDataSourceDirectory() {
        if (dataSource == null) {
            return "DATA_SOURCE_NOT_SET";
        }
        return rootDirectory + File.separatorChar + dataSource;
    }

    String[] asOtpArgs() {
        List<String> args = new ArrayList<>();

        if (buildAll()) {
            args.add("--build");
        }
        else if (buildStreet) {
            args.add("--buildStreet");
        }
        else if (buildTransit) {
            args.add("--loadStreet");
        }
        else {
            args.add("--load");
        }

        if (saveGraph && (buildTransit || buildStreet)) { args.add("--save"); }
        if (serveGraph && !buildStreetOnly()) { args.add("--serve"); }

        args.add(getDataSourceDirectory());

        return args.toArray(new String[0]);
    }

    private void notifyChangeListener() {
        if(commandLineChange != null) {
            commandLineChange.accept(toCliString());
        }
    }

    private boolean buildAll() { return buildStreet && buildTransit; }

    private boolean buildStreetOnly() { return buildStreet && !buildTransit; }

    private void setupListOfDebugLoggers() {
        for (String log : DebugLoggingSupport.getLogs()) {
            debugLogging.put(log, Boolean.FALSE);
        }
    }
}
