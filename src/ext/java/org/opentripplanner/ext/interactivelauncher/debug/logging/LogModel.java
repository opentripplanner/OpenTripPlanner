package org.opentripplanner.ext.interactivelauncher.debug.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsible for storing the selected loggers to debug. This is
 * serialized to store the user preferences between runs.
 */
public class LogModel implements Serializable {

  private final Set<String> activeLoggers = new HashSet<>();

  @JsonIgnore
  private Runnable saveCallback;

  public LogModel() {}

  public static LogModel createFromConfig() {
    var model = new LogModel();
    model.initFromConfig();
    return model;
  }

  /** Need to set this manually to support JSON serialization. */
  public void init(Runnable saveCallback) {
    this.saveCallback = saveCallback;
  }

  /** Used by JSON serialization. */
  public Collection<String> getActiveLoggers() {
    return List.copyOf(activeLoggers);
  }

  /** Used by JSON deserialization. */
  public void setActiveLoggers(Collection<String> loggers) {
    this.activeLoggers.clear();
    this.activeLoggers.addAll(loggers);
    for (var logger : activeLoggers) {
      DebugLoggingSupport.configureDebugLogging(logger, true);
    }
  }

  boolean isLoggerEnabled(String name) {
    return activeLoggers.contains(name);
  }

  void turnLoggerOnOff(String name, boolean enable) {
    if (enable) {
      if (!activeLoggers.contains(name)) {
        activeLoggers.add(name);
        DebugLoggingSupport.configureDebugLogging(name, enable);
        save();
      }
    } else {
      if (activeLoggers.contains(name)) {
        activeLoggers.remove(name);
        DebugLoggingSupport.configureDebugLogging(name, enable);
        save();
      }
    }
  }

  private void initFromConfig() {
    var debugLoggers = DebugLoggers.listLoggers();
    for (var logger : DebugLoggingSupport.listConfiguredDebugLoggers()) {
      if (debugLoggers.contains(logger)) {
        activeLoggers.add(logger);
      }
    }
  }

  private void save() {
    if (saveCallback != null) {
      saveCallback.run();
    }
  }
}
