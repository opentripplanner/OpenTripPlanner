package org.opentripplanner.ext.interactivelauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SetupResult {
  private final File configDataDir;
  private final boolean buildStreet;
  private final boolean buildTransit;
  private final boolean saveGraph;
  private final boolean serveGraph;

  public SetupResult(
      File configDataDir, boolean buildStreet, boolean buildTransit, boolean saveGraph,
      boolean serveGraph
  ) {
    this.configDataDir = configDataDir;
    this.buildStreet = buildStreet;
    this.buildTransit = buildTransit;
    this.saveGraph = saveGraph;
    this.serveGraph = serveGraph;
  }

  File configDataDir() {
    return configDataDir;
  }

  boolean buildStreet() {
    return buildStreet;
  }

  boolean buildTransit() {
    return buildTransit;
  }

  boolean buildAll() {
    return buildStreet && buildTransit;
  }

  boolean buildStreetOnly() {
    return buildStreet && !buildTransit;
  }

  boolean saveGraph() {
    return saveGraph;
  }

  boolean serveGraph() {
    return serveGraph;
  }

  @Override
  public String toString() {
    return "SetupResult{"
        + "configDataDir=" + configDataDir.getAbsolutePath()
        + (buildStreet ? ", buildStreet" : "")
        + (buildTransit ? ", buildTransit" : "")
        + (saveGraph ? ", saveGraph" : "")
        + (serveGraph ? ", serveGraph" : "")
        + '}';
  }

  String[] asOtpArgs() {
    List<String> args = new ArrayList<>();

    if(buildAll()) {
      args.add("--build");
    }
    else if(buildStreet) {
      args.add("--buildStreet");
    }
    else if(buildTransit) {
      args.add("--loadStreet");
    }
    else {
      args.add("--load");
    }

    if(saveGraph && (buildTransit||buildStreet)) { args.add("--save"); }
    if(serveGraph && !buildStreetOnly()) { args.add("--serve"); }

    args.add(configDataDir.getAbsolutePath());

    return args.toArray(new String[0]);
  }

  public String toCliString() {
    return String.join(" ", asOtpArgs());
  }
}
