package org.opentripplanner.ext.interactivelauncher.startup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.interactivelauncher.support.SearchForOtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartupModel {

  private static final Logger LOG = LoggerFactory.getLogger(StartupModel.class);

  @JsonIgnore
  private transient Consumer<String> commandLineChange;

  private String rootDirectory = null;
  private String dataSource = null;
  private boolean buildStreet = false;
  private boolean buildTransit = true;
  private boolean saveGraph = false;
  private boolean serveGraph = true;
  private boolean visualizer = false;

  public void subscribeCmdLineUpdates(Consumer<String> commandLineChange) {
    this.commandLineChange = commandLineChange;
  }

  @SuppressWarnings("AccessOfSystemProperties")
  public String getRootDirectory() {
    return rootDirectory == null ? System.getProperty("user.dir") : rootDirectory;
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
      if (path.length() <= length) {
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

  public boolean isVisualizer() {
    return visualizer;
  }

  public void setVisualizer(boolean visualizer) {
    this.visualizer = visualizer;
    notifyChangeListener();
  }

  @Override
  public String toString() {
    return String.join("", asOtpArgs());
  }

  public String toCliString() {
    return String.join(" ", asOtpArgs());
  }

  private void notifyChangeListener() {
    if (commandLineChange != null) {
      commandLineChange.accept(toCliString());
    }
  }

  @JsonIgnore
  String getDataSourceDirectory() {
    if (dataSource == null) {
      return "DATA_SOURCE_NOT_SET";
    }
    return getRootDirectory() + File.separatorChar + dataSource;
  }

  public String[] asOtpArgs() {
    List<String> args = new ArrayList<>();

    if (buildAll()) {
      args.add("--build");
    } else if (buildStreet) {
      args.add("--buildStreet");
    } else if (buildTransit) {
      args.add("--loadStreet");
    } else {
      args.add("--load");
    }

    if (saveGraph && (buildTransit || buildStreet)) {
      args.add("--save");
    }
    if (serveGraph && !buildStreetOnly()) {
      args.add("--serve");
    }
    if (serveGraph && !buildStreetOnly() && visualizer) {
      args.add("--visualize");
    }

    args.add(getDataSourceDirectory());

    return args.toArray(new String[0]);
  }

  private boolean buildAll() {
    return buildStreet && buildTransit;
  }

  private boolean buildStreetOnly() {
    return buildStreet && !buildTransit;
  }
}
